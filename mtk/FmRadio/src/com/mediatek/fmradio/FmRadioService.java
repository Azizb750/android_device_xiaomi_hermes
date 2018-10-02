/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2011-2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.fmradio;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.fmradio.FmRadioStation.Station;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
/* Vanzo:tanglei on: Sat, 14 Mar 2015 11:51:16 +0800
 */
//import com.android.featureoption.FeatureOption;
// End of Vanzo:tanglei

/**
 * Background service to control FM or do background tasks.
 */
public class FmRadioService extends Service implements FmRecorder.OnRecorderStateChangedListener {
    // Audio config replacer
    private static int mAudioMode;
    private static int playSound;
    private final float FX_VOLUME = -1.0F;
    
    // Logging
    private static final String TAG = "FmRx/Service";

    // Broadcast messages from clients to FM service.
    public static final String ACTION_TOFMSERVICE_POWERDOWN
            = "com.mediatek.FMRadio.FMRadioService.ACTION_TOFMSERVICE_POWERDOWN";
    // Broadcast messages to FM Tx service.
    public static final String ACTION_TOFMTXSERVICE_POWERDOWN
            = "com.mediatek.FMTransmitter.FMTransmitterService.ACTION_TOFMTXSERVICE_POWERDOWN";
    // Broadcast messages to mATV service.
    public static final String ACTION_TOATVSERVICE_POWERDOWN
            = "com.mediatek.app.mtv.ACTION_REQUEST_SHUTDOWN";
    // Broadcast messages to music service.
    public static final String ACTION_TOMUSICSERVICE_POWERDOWN
            = "com.android.music.musicservicecommand.pause";
    // Broadcast messages from mATV service.
    public static final String ACTION_FROMATVSERVICE_POWERUP = "com.mediatek.app.mtv.POWER_ON";

    // Broadcast messages from other sounder APP to FM service
    private static final String SOUND_POWER_DOWN_MSG = "com.android.music.musicservicecommand";
    private static final String CMDPAUSE = "pause";

    // HandlerThread Keys
    private static final String FM_FREQUENCY = "frequency";
    private static final String OPTION = "option";
    private static final String RECODING_FILE_NAME = "name";

    // RDS events
    // PS
    private static final int RDS_EVENT_PROGRAMNAME = 0x0008;
    // RT
    private static final int RDS_EVENT_LAST_RADIOTEXT = 0x0040;
    // AF
    private static final int RDS_EVENT_AF = 0x0080;

    // Headset
    private static final int HEADSET_PLUG_IN = 1;
    // Short antenna support
    private static final boolean SHORT_ANNTENNA_SUPPORT = FmRadioUtils.isFmShortAntennaSupport();

    // Notification id
    private static final int NOTIFICATION_ID = 1;

    // Set audio policy for FM
    // should check AUDIO_POLICY_FORCE_FOR_MEDIA in audio_policy.h
    private static final int FOR_PROPRIETARY = 1;
    // Forced Use value
    private int mForcedUseForMedia;

    // TX and RX interaction
    private static final int CURRENT_RX_ON = 0;
    private static final int CURRENT_TX_ON = 1;
    private static final int CURRENT_TX_SCAN = 2;

    // FM recorder
    FmRecorder mFmRecorder = null;
    private BroadcastReceiver mSdcardListener = null;
    private int mRecordState = FmRecorder.STATE_INVALID;
    private int mRecorderErrorType = -1;
    // If eject record sdcard, should set Value false to not record.
    // Key is sdcard path(like "/storage/sdcard0"), V is to enable record or
    // not.
    private HashMap<String, Boolean> mSdcardStateMap = new HashMap<String, Boolean>();
    // The show name in save dialog but saved in service
    // If modify the save title it will be not null, otherwise it will be null
    private String mModifiedRecordingName = null;
    // record the listener list, will notify all listener in list
    private ArrayList<Record> mRecords = new ArrayList<Record>();
    // record FM whether in recording mode
    private boolean mIsInRecordingMode = false;
    // record sd card path when start recording
    private static String sRecordingSdcard = FmRadioUtils.getDefaultStoragePath();

    // RDS
    // PS String
    private String mPSString = "";
    // RT String
    private String mLRTextString = "";
    // PS RT
    private boolean mIsPSRTEnabled = false;
    // AF
    private boolean mIsAFEnabled = false;
    // RDS thread use to receive the information send by station
    private Thread mRdsThread = null;
    // record whether RDS thread exit
    private boolean mIsRdsThreadExit = false;

    // State variables
    // Record whether FM is in native scan state
    private boolean mIsNativeScanning = false;
    // Record whether FM is in scan thread
    private boolean mIsScanning = false;
    // Record whether FM is in seeking state
    private boolean mIsNativeSeeking = false;
    // Record whether FM is in native seek
    private boolean mIsSeeking = false;
    // Record whether searching progress is canceled
    private boolean mIsStopScanCalled = false;
    // Record whether is speaker used
    private boolean mIsSpeakerUsed = false;
    // Record whether device is open
    private boolean mIsDeviceOpen = false;
    // Record whether FM is power up
    private boolean mIsPowerUp = false;
    // Record whether is power uping, if so, should judge in activity back key.
    private boolean mIsPowerUping = false;
    // Record whether service is init
    private boolean mIsServiceInited = false;
    // Fm power down by loss audio focus,should make power down menu item can
    // click
    private boolean mIsMakePowerDown = false;

    // Instance variables
    private Context mContext = null;
    private AudioManager mAudioManager = null;
    private ActivityManager mActivityManager = null;
    private MediaPlayer mFmPlayer = null;
    private WakeLock mWakeLock = null;
    // Audio focus is held or not
    private boolean mIsAudioFocusHeld = false;
    // Focus transient lost
    private boolean mPausedByTransientLossOfFocus = false;
    private int mCurrentStation = FmRadioUtils.DEFAULT_STATION;
    // Headset plug state (0:long antenna plug in, 1:long antenna plug out)
    private int mValueHeadSetPlug = 1;
    // For bind service
    private final IBinder mBinder = new ServiceBinder();
    // Broadcast to receive the external event
    private FmServiceBroadcastReceiver mBroadcastReceiver = null;
    // Async handler
    private FmRadioServiceHandler mFmServiceHandler;
    // Lock for lose audio focus and receive SOUND_POWER_DOWN_MSG
    // at the same time
    // while recording call stop recording not finished(status is still
    // RECORDING), but
    // SOUND_POWER_DOWN_MSG will exitFm(), if it is RECORDING will discard the
    // record.
    // 1. lose audio focus -> stop recording(lock) -> set to IDLE and show save
    // dialog
    // 2. exitFm() -> check the record status, discard it if it is recording
    // status(lock)
    // Add this lock the exitFm() while stopRecording()
    private Object mStopRecordingLock = new Object();
    // The listener for exit, should finish favorite when exit FM
    private static OnExitListener sExitListener = null;
    // Record FmRadioActivity state
    private static boolean sActivityIsOnStop = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "FmRadioService.onBind: " + intent);
        return mBinder;
    }

    /**
     * class use to return service instance
     */
    public class ServiceBinder extends Binder {
        /**
         * get FM service instance
         *
         * @return service instance
         */
        FmRadioService getService() {
            return FmRadioService.this;
        }
    }

    /**
     * Broadcast monitor external event, Other app want FM stop, Phone shut
     * down, screen state, headset state
     */
    private class FmServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, ">>> FmRadioService.onReceive");
            String action = intent.getAction();
            String command = intent.getStringExtra("command");
            Log.d(TAG, "Action/Command: " + action + " / " + command);
            // other app want FM stop, stop FM
            if (ACTION_TOFMSERVICE_POWERDOWN.equals(action)
                    || ACTION_FROMATVSERVICE_POWERUP.equals(action)
                    || (SOUND_POWER_DOWN_MSG.equals(action) && CMDPAUSE.equals(command))) {
                // need remove all messages, make power down will be execute
                mFmServiceHandler.removeCallbacksAndMessages(null);

                Log.d(TAG, "onReceive.SOUND_POWER_DOWN_MSG. exit FM");
                exitFm();
                stopSelf();
                // phone shut down, so exit FM
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                /**
                 * here exitFm, system will send broadcast, system will shut
                 * down, so fm does not need call back to activity
                 */
                mFmServiceHandler.removeCallbacksAndMessages(null);
                exitFm();
                // screen on, if FM play, open rds
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {

                setRdsAsync(true);
                // screen off, if FM play, close rds
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                setRdsAsync(false);
                // switch antenna when headset plug in or plug out
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                // switch antenna should not impact audio focus status
                mValueHeadSetPlug = (intent.getIntExtra("state", -1) == HEADSET_PLUG_IN) ? 0 : 1;
                switchAntennaAsync(mValueHeadSetPlug);

                if (SHORT_ANNTENNA_SUPPORT) {
                    boolean isSwitch = (switchAntenna(mValueHeadSetPlug) == 0) ? true : false;
                    Log.d(TAG, "onReceive.switch anntenna:isWitch:" + isSwitch);

                    // Plug out->Speaker Mode; Plug in->Earphone Mode
                    boolean plugInEarphone = (0 == mValueHeadSetPlug);
                    // Need check to switch to earphone mode for audio will
                    // change to AudioSystem.FORCE_NONE
                    if (plugInEarphone) {
                        mForcedUseForMedia = AudioSystem.FORCE_NONE;
                        mIsSpeakerUsed = false;
                    }
                    //setSpeakerPhoneOn(!plugInEarphone);
                    // Notify UI change to earphone mode, false means not speaker mode
                    Bundle bundle = new Bundle(2);
                    bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                            FmRadioListener.LISTEN_SPEAKER_MODE_CHANGED);
                    bundle.putBoolean(FmRadioListener.KEY_IS_SPEAKER_MODE, !plugInEarphone);
                    notifyActivityStateChanged(bundle);

                    powerUpAutoIfNeed();
                } else {
                    // Avoid Service is killed,and receive headset plug in
                    // broadcast again
                    if (!mIsServiceInited) {
                        Log.d(TAG, "onReceive.switch anntenna:service is not init");
                        powerUpAutoIfNeed();
                        return;
                    }
                    /*
                     * If ear phone insert and activity is
                     * foreground. power up FM automatic
                     */
                    if ((0 == mValueHeadSetPlug) && isActivityForeground()) {
                        Log.d(TAG, "onReceive.switch anntenna:need auto power up");
                        powerUpAsync(FmRadioUtils.computeFrequency(mCurrentStation));
                    } else if (1 == mValueHeadSetPlug) {
                        Log.d(TAG, "plug out earphone, need to stop fm");
                        // ALPS01687760 Avoid sound from speaker after plug out earphone when recording
                        // plug out earphone will power down or exit, need to mute first anyway
                        setMute(true);
                        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SCAN_FINISHED);
                        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SEEK_FINISHED);
                        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_TUNE_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_POWERDOWN_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_POWERUP_FINISHED);
                        stopFmFocusLoss(AudioManager.AUDIOFOCUS_LOSS);
    
                        // Need check to switch to earphone mode for audio will
                        // change to AudioSystem.FORCE_NONE
                        setSpeakerPhoneOn(false);
    
                        // Notify UI change to earphone mode, false means not speaker mode
                        Bundle bundle = new Bundle(2);
                        bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                                FmRadioListener.LISTEN_SPEAKER_MODE_CHANGED);
                        bundle.putBoolean(FmRadioListener.KEY_IS_SPEAKER_MODE, false);
                        notifyActivityStateChanged(bundle);
                    }
                }

            } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                int connectState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, 0);
                Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED: connectState=" + connectState + ", ispowerup="
                        + mIsPowerUp);
                handleBtConnectState(connectState);
/* Vanzo:tanglei on: Fri, 13 Mar 2015 21:26:14 +0800
 * earphone key action
 */
            } //else if (FeatureOption.VANZO_FEATURE_EARPHONE_KEY_ACTION && "com.mediatek.FMRadio.FMRadioService.NEXT_STATION".equals(action)) {
               //seekStationAsync(FmRadioUtils.computeFrequency(mCurrentStation), true);
// End of Vanzo:tanglei
            //} 
            else {
                Log.w(TAG, "Error: undefined action.");
            }
            Log.d(TAG, "<<< FmRadioService.onReceive");
        }
    }

    /**
     * ALPS01756692 No sound after click FM app and power key
     * Need to power up auto for two cases:
     * case 1: Launcher click FM app, then quickly click Power key to lock phone.
     * case 2: Launcher click FM app, then quickly click Home key.
     * Because power up action is in FmRadioActivity.onServiceConnected(), these two cases
     * will not callback onServiceConnected() cause FmRadioActivity.onStop() has called unbind()
     */
    private void powerUpAutoIfNeed() {
        if ((0 == mValueHeadSetPlug)) {
            if (!mIsPowerUping && !mIsPowerUp && sActivityIsOnStop) {
                Log.w(TAG, "Power up for start app then quick click power/home");
                int iCurrentStation = FmRadioStation.getCurrentStation(mContext);
                powerUpAsync(FmRadioUtils.computeFrequency(iCurrentStation));
            }
        }
    }
    
    /**
     * handle FM over BT connect state
     * 
     * @param connectState
     *            FM over BT connect state
     */
    private void handleBtConnectState(int connectState) {
        if (!mIsPowerUp) {
            return;
        }

        switch (connectState) {
        case BluetoothA2dp.STATE_CONNECTED:
        //case BluetoothA2dp.STATE_PLAYING:
        //case BluetoothA2dp.STATE_CONNECTING:
            Log.d(TAG, "handleBtConnectState bt connected");
            changeToEarphoneMode();
            break;
        case BluetoothA2dp.STATE_DISCONNECTED:
        //case BluetoothA2dp.STATE_DISCONNECTING:
            Log.d(TAG, "handleBtConnectState bt disconnected");
            changeToEarphoneMode();
            break;
        default:
            Log.d(TAG, "invalid fm over bt connect state");
            break;
        }

        
    }

    private void changeToEarphoneMode() {
        setSpeakerPhoneOn(false);
        // Notify UI change to earphone mode, false means not speaker mode
        Bundle bundle = new Bundle(2);
        bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                FmRadioListener.LISTEN_SPEAKER_MODE_CHANGED);
        // Always set to earphone mode when bt is connected or disconnected
        bundle.putBoolean(FmRadioListener.KEY_IS_SPEAKER_MODE, false);
        notifyActivityStateChanged(bundle);
    }

    /**
     * Check if BT is connected
     * @return true if current is playing with BT earphone
     */
    public boolean isBtConnected() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "isBtConnected headset:"
                + btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                + ", a2dp:" + btAdapter.getProfileConnectionState(BluetoothProfile.A2DP));
        int a2dpState = btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        return (BluetoothProfile.STATE_CONNECTED == a2dpState
                || BluetoothProfile.STATE_CONNECTING == a2dpState);
    }

    /**
     * Handle sdcard mount/unmount event. 1. Update the sdcard state map 2. If
     * the recording sdcard is unmounted, need to stop and notify
     */
    private class SdcardListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If eject record sdcard, should set this false to not
            // record.
            updateSdcardStateMap(intent);

            if (mFmRecorder == null) {
                Log.w(TAG, "SD receiver: FMRecorder is not present!!");
                return;
            }

            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_EJECT.equals(action) ||
                    Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                // If not unmount recording sd card, do nothing;
                if (isRecordingCardUnmount(intent)) {
                    Log.v(TAG, "MEDIA_EJECT");
                    if (mFmRecorder.getState() == FmRecorder.STATE_RECORDING) {
                        Log.d(TAG, "old state is recording");
                        onRecorderError(FmRecorder.ERROR_SDCARD_NOT_PRESENT);
                        mFmRecorder.discardRecording();
                    } else {
                        Bundle bundle = new Bundle(2);
                        bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                                FmRadioListener.LISTEN_RECORDSTATE_CHANGED);
                        bundle.putInt(FmRadioListener.KEY_RECORDING_STATE,
                                FmRecorder.STATE_IDLE);
                        notifyActivityStateChanged(bundle);
                    }
                }
                return;
            }
        }
    }

    /**
     * whether antenna available
     *
     * @return true, antenna available; false, antenna not available
     */
    public boolean isAntennaAvailable() {
        return mAudioManager.isWiredHeadsetOn();
    }

    /**
     * Set FM audio from speaker or not
     *
     * @param isSpeaker true if set FM audio from speaker
     */
    public void setSpeakerPhoneOn(boolean isSpeaker) {
        Log.d(TAG, ">>> FmRadioService.useSpeaker: " + isSpeaker);
        mForcedUseForMedia = isSpeaker ? AudioSystem.FORCE_SPEAKER : AudioSystem.FORCE_NONE;
        AudioSystem.setForceUse(FOR_PROPRIETARY, mForcedUseForMedia);
        mIsSpeakerUsed = isSpeaker;
        Log.d(TAG, "<<< FmRadioService.useSpeaker");
    }

    private boolean isSpeakerPhoneOn() {
        return (mForcedUseForMedia == AudioSystem.FORCE_SPEAKER);
    }

    /**
     * open FM device, should be call before power up
     *
     * @return true if FM device open, false FM device not open
     */
    private boolean openDevice() {
        Log.d(TAG, ">>> FmRadioService.openDevice");
        if (!mIsDeviceOpen) {
            mIsDeviceOpen = FmRadioNative.openDev();
        }

        // set audio config
        Log.d(TAG, "<<< AD: set audio config: RINGER_MODE_VIBRATE");
        mAudioMode = mAudioManager.getRingerMode();
        playSound = AudioManager.FX_KEYPRESS_STANDARD;
        mAudioManager.playSoundEffect(playSound, FX_VOLUME);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

        Log.d(TAG, "<<< FmRadioService.openDevice: " + mIsDeviceOpen);
        return mIsDeviceOpen;
    }

    /**
     * close FM device
     *
     * @return true if close FM device success, false close FM device failed
     */
    private boolean closeDevice() {
        Log.d(TAG, ">>> FmRadioService.closeDevice");
        boolean isDeviceClose = false;
        if (mIsDeviceOpen) {
            isDeviceClose = FmRadioNative.closeDev();
            mIsDeviceOpen = !isDeviceClose;
        }
        Log.d(TAG, "<<< FmRadioService.closeDevice: " + isDeviceClose);

        // restore audio config
        Log.d(TAG, "<<< AD: restore audio config: "+ mAudioMode);
        mAudioManager.setRingerMode(mAudioMode);

        // quit looper
        mFmServiceHandler.getLooper().quit();
        return isDeviceClose;
    }

    /**
     * get FM device opened or not
     *
     * @return true FM device opened, false FM device closed
     */
    public boolean isDeviceOpen() {
        Log.d(TAG, "FmRadioService.isDeviceOpen: " + mIsDeviceOpen);
        return mIsDeviceOpen;
    }

    /**
     * power up FM, and make FM voice output from earphone
     *
     * @param frequency
     */
    public void powerUpAsync(float frequency) {
        mIsPowerUping = true;
        final int bundleSize = 1;
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_POWERUP_FINISHED);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_POWERDOWN_FINISHED);
        Bundle bundle = new Bundle(bundleSize);
        bundle.putFloat(FM_FREQUENCY, frequency);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_POWERUP_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private boolean powerUpFm(float frequency) {
        Log.d(TAG, ">>> FmRadioService.powerUp: " + frequency);
        if (mIsPowerUp) {
            Log.d(TAG, "<<< FmRadioService.powerUp: already power up:" + mIsPowerUp);
            return true;
        }

        if (!requestAudioFocus()) {
            // activity used for update powerdown menu
            mIsMakePowerDown = true;
            Log.d(TAG, "FM can't get audio focus when power up");
            sendBroadcastToStopOtherAPP();
            return false;
        }

        // if device open fail when chip reset, it need open device again before
        // power up
        if (!mIsDeviceOpen) {
            openDevice();
        }

        waitIfTxSearching();
        Log.d(TAG, "set CURRENT_RX_ON true, CURRENT_TX_ON false");
        FmRadioNative.setFmStatus(CURRENT_RX_ON, true);
        FmRadioNative.setFmStatus(CURRENT_TX_ON, false);
        sendBroadcastToStopOtherAPP();

        Log.d(TAG, "service native power up start");
        if (!FmRadioNative.powerUp(frequency)) {
            Log.e(TAG, "Error: powerup failed.");
            return false;
        }
        Log.d(TAG, "service native power up end");
        mIsPowerUp = true;
        // need mute after power up
        setMute(true);

        // activity used for update powerdown menu
        mIsMakePowerDown = false;
        Log.d(TAG, "<<< FmRadioService.powerUp: " + mIsPowerUp);
        return mIsPowerUp;
    }

    // wait if TX is searching for timing issue
    private void waitIfTxSearching() {
        Log.d(TAG, ">>> waitIfTxSearching " + FmRadioNative.getFmStatus(CURRENT_TX_SCAN));
        long start = System.currentTimeMillis();
        // true for TX is searching
        while (FmRadioNative.getFmStatus(CURRENT_TX_SCAN) == true) {
            if (System.currentTimeMillis() - start > 5000) {
                Log.e(TAG, "waitIfTxSearching timeout");
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        Log.d(TAG, "<<< waitIfTxSearching");
    }

    private boolean startPlayFm(float frequency) {
        Log.d(TAG, ">>> FmRadioService.initDevice: " + frequency);

        mCurrentStation = FmRadioUtils.computeStation(frequency);
        FmRadioStation.setCurrentStation(mContext, mCurrentStation);
        // Add notification to the title bar.
        showNotification();

        // Start the RDS thread if RDS is supported.
        if (isRdsSupported()) {
            Log.d(TAG, "RDS is supported. Start the RDS thread.");
            startRdsThread();
        }

        if (!FmRadioUtils.isFmSuspendSupport()) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
                Log.d(TAG, "acquire wake lock");
            }
        }
        if (mIsSpeakerUsed != isSpeakerPhoneOn()) {
            setSpeakerPhoneOn(mIsSpeakerUsed);
        }
        if (mRecordState != FmRecorder.STATE_PLAYBACK) {
            enableFmAudio(true);
        }

        setRds(true);
        setMute(false);

        Log.d(TAG, "<<< FmRadioService.initDevice: " + mIsPowerUp);
        return mIsPowerUp;
    }

    /**
     * send broadcast to stop other application, such as music, MATV,
     * FMTransmitter
     */
    private void sendBroadcastToStopOtherAPP() {
        Intent intentToMusic = new Intent(ACTION_TOMUSICSERVICE_POWERDOWN);
        sendBroadcast(intentToMusic);
        Intent intentToAtv = new Intent(ACTION_TOATVSERVICE_POWERDOWN);
        sendBroadcast(intentToAtv);
        Intent intentToFMTx = new Intent(ACTION_TOFMTXSERVICE_POWERDOWN);
        sendBroadcast(intentToFMTx);
    }

    /**
     * power down FM
     */
    public void powerDownAsync() {
        // if power down Fm, should remove message first.
        // not remove all messages, because such as recorder message need
        // to execute after or before power down
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SCAN_FINISHED);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SEEK_FINISHED);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_TUNE_FINISHED);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_POWERDOWN_FINISHED);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_POWERUP_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_POWERDOWN_FINISHED);
    }

    /**
     * Power down FM
     *
     * @return true if power down success
     */
    private boolean powerDown() {
        Log.d(TAG, ">>> FmRadioService.powerDown");
        if (!mIsPowerUp) {
            Log.w(TAG, "Error: device is already power down.");
            return true;
        }

        setMute(true);
        setRds(false);
        enableFmAudio(false);

        // Only need to power down if RX status in native is ON
        // If TX is on, so need power down(TX is using).
        boolean isRxOn = FmRadioNative.getFmStatus(CURRENT_RX_ON);
        boolean powerDownSuccess = false;
        if (isRxOn) {
            powerDownSuccess = FmRadioNative.powerDown(0);
        } else {
            powerDownSuccess = true;
        }
        
        if (!powerDownSuccess) {
            Log.e(TAG, "Error: powerdown failed.");
            // activity used for update powerdown menu
            mIsMakePowerDown = true;

            if (isRdsSupported()) {
                Log.d(TAG, "RDS is supported. Stop the RDS thread.");
                stopRdsThread();
            }
            mIsPowerUp = false;
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
                Log.d(TAG, "release wake lock");
            }
            // Remove the notification in the title bar.
            removeNotification();
            Log.d(TAG, "powerdown failed.release some resource.");
            return false;
        }
        // activity used for update powerdown menu
        mIsMakePowerDown = true;

        if (isRdsSupported()) {
            Log.d(TAG, "RDS is supported. Stop the RDS thread.");
            stopRdsThread();
        }
        mIsPowerUp = false;

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(TAG, "release wake lock");
        }

        // Remove the notification in the title bar.
        removeNotification();
        Log.d(TAG, "<<< FmRadioService.powerDown: true");
        return true;
    }

    /**
     * Check whether FM is power up
     *
     * @return true, power up; false, power down.
     */
    public boolean isPowerUp() {
        Log.d(TAG, "FmRadioService.isPowerUp: " + mIsPowerUp);
        return mIsPowerUp;
    }

    /**
     * Check whether FM is power uping. if power uping, activity should call
     * super.onBackPressed, avoid not execute power down method.
     *
     * @return true, power up; false, power down.
     */
    public boolean isPowerUping() {
        Log.d(TAG, "FmRadioService.isPowerUping: " + mIsPowerUping);
        return mIsPowerUping;
    }

    /**
     * Check whether FM is power down by other app.
     *
     * @return true, power down; true.
     */
    public boolean isMakePowerDown() {
        Log.d(TAG, "FmRadioService.mIsMakePowerDown: " + mIsMakePowerDown);
        return mIsMakePowerDown;
    }

    /**
     * Tune to a station
     *
     * @param frequency The frequency to tune
     *
     * @return true, success; false, fail.
     */
    public void tuneStationAsync(float frequency) {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_TUNE_FINISHED);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putFloat(FM_FREQUENCY, frequency);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_TUNE_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private boolean tuneStation(float frequency) {
        Log.d(TAG, ">>> FmRadioService.tune: " + frequency);
        if (mIsPowerUp) {
            setRds(false);
            Log.d(TAG, "FmRadioService.native tune start");
            boolean bRet = FmRadioNative.tune(frequency);
            Log.d(TAG, "FmRadioService.native tune end");
            if (bRet) {
                setRds(true);
                mCurrentStation = FmRadioUtils.computeStation(frequency);
                FmRadioStation.setCurrentStation(mContext, mCurrentStation);
                updateNotification();
            }
            setMute(false);
            Log.d(TAG, "<<< FmRadioService.tune: " + bRet);
            return bRet;
        }

        // if not support short Antenna and earphone is not insert, not power up
        if (!isAntennaAvailable() && !SHORT_ANNTENNA_SUPPORT) {
            Log.d(TAG, "earphone is not insert and short antenna not support");
            return false;
        }

        // if not power up yet, should powerup first
        Log.w(TAG, "FM is not powered up");
        mIsPowerUping = true;
        boolean tune = false;

        if (powerUpFm(frequency)) {
            tune = startPlayFm(frequency);
        }
        mIsPowerUping = false;
        Log.d(TAG, "<<< FmRadioService.tune: mIsPowerup:" + tune);
        return tune;
    }

    /**
     * Seek station according frequency and direction
     *
     * @param frequency start frequency(100KHZ, 87.5)
     * @param isUp direction(true, next station; false, previous station)
     *
     * @return the frequency after seek
     */
    public void seekStationAsync(float frequency, boolean isUp) {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SEEK_FINISHED);
        final int bundleSize = 2;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putFloat(FM_FREQUENCY, frequency);
        bundle.putBoolean(OPTION, isUp);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_SEEK_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private float seekStation(float frequency, boolean isUp) {
        Log.d(TAG, ">>> FmRadioService.seek: " + frequency + " " + isUp);
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return -1;
        }

        setRds(false);
        mIsNativeSeeking = true;
        float fRet = FmRadioNative.seek(frequency, isUp);
        mIsNativeSeeking = false;
        // make mIsStopScanCalled false, avoid stop scan make this true,
        // when start scan, it will return null.
        mIsStopScanCalled = false;
        Log.d(TAG, "<<< FmRadioService.seek: " + fRet);
        return fRet;
    }

    /**
     * Scan stations
     */
    public void startScanAsync() {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SCAN_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_SCAN_FINISHED);
    }

    private int[] startScan() {
        Log.d(TAG, ">>> FmRadioService.startScan");
        int[] iChannels = null;

        setRds(false);
        setMute(true);
        short[] shortChannels = null;
        if (!mIsStopScanCalled) {
            mIsNativeScanning = true;
            Log.d(TAG, "startScan native method:start");
            shortChannels = FmRadioNative.autoScan();
            Log.d(TAG, "startScan native method:end " + Arrays.toString(shortChannels));
            mIsNativeScanning = false;
        }

        setRds(true);
        if (mIsStopScanCalled) {
            // Received a message to power down FM, or interrupted by a phone
            // call. Do not return any stations. shortChannels = null;
            // if cancel scan, return invalid station -100
            shortChannels = new short[] {
                -100
            };
            mIsStopScanCalled = false;
        }

        if (null != shortChannels) {
            int size = shortChannels.length;
            iChannels = new int[size];
            for (int i = 0; i < size; i++) {
                iChannels[i] = shortChannels[i];
            }
        }
        Log.d(TAG, "<<< FmRadioService.startScan: " + Arrays.toString(iChannels));
        return iChannels;
    }

    /**
     * Check FM Radio is in scan progress or not
     *
     * @return if in scan progress return true, otherwise return false.
     */
    public boolean isScanning() {
        return mIsScanning;
    }

    /**
     * Stop scan progress
     *
     * @return true if can stop scan, otherwise return false.
     */
    public boolean stopScan() {
        Log.d(TAG, ">>> FmRadioService.stopScan");
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return false;
        }

        boolean bRet = false;
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SCAN_FINISHED);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SEEK_FINISHED);
        if (mIsNativeScanning || mIsNativeSeeking) {
            mIsStopScanCalled = true;
            Log.d(TAG, "native stop scan:start");
            bRet = FmRadioNative.stopScan();
            Log.d(TAG, "native stop scan:end --" + bRet);
        }
        Log.d(TAG, "<<< FmRadioService.stopScan: " + bRet);
        return bRet;
    }

    /**
     * Check FM is in seek progress or not
     *
     * @return true if in seek progress, otherwise return false.
     */
    public boolean isSeeking() {
        return mIsNativeSeeking;
    }

    /**
     * Set RDS
     *
     * @param on true, enable RDS; false, disable RDS.
     */
    public void setRdsAsync(boolean on) {
        final int bundleSize = 1;
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SET_RDS_FINISHED);
        Bundle bundle = new Bundle(bundleSize);
        bundle.putBoolean(OPTION, on);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_SET_RDS_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private int setRds(boolean on) {
        if (!mIsPowerUp) {
            return -1;
        }
        Log.d(TAG, ">>> FmRadioService.setRDS: " + on);
        int ret = -1;
        if (isRdsSupported()) {
            ret = FmRadioNative.setRds(on);
        }
        setPS("");
        setLRText("");
        Log.d(TAG, "<<< FmRadioService.setRDS: " + ret);
        return ret;
    }

    /**
     * Get PS information
     *
     * @return PS information
     */
    public String getPS() {
        Log.d(TAG, "FmRadioService.getPS: " + mPSString);
        return mPSString;
    }

    /**
     * Get RT information
     *
     * @return RT information
     */
    public String getLRText() {
        Log.d(TAG, "FmRadioService.getLRText: " + mLRTextString);
        return mLRTextString;
    }

    /**
     * Get AF frequency
     *
     * @return AF frequency
     */
    public void activeAFAsync() {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_ACTIVE_AF_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_ACTIVE_AF_FINISHED);
    }

    private int activeAF() {
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return -1;
        }

        int frequency = FmRadioNative.activeAf();
        Log.d(TAG, "FmRadioService.activeAF: " + frequency);
        return frequency;
    }

    /**
     * Mute or unmute FM voice
     *
     * @param mute true for mute, false for unmute
     *
     * @return (true, success; false, failed)
     */
    public void setMuteAsync(boolean mute) {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SET_MUTE_FINISHED);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putBoolean(OPTION, mute);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_SET_MUTE_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private int setMute(boolean mute) {
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return -1;
        }
        Log.d(TAG, ">>> FmRadioService.setMute: " + mute);
        int iRet = FmRadioNative.setMute(mute);
        Log.d(TAG, "<<< FmRadioService.setMute: " + iRet);
        return iRet;
    }

    /**
     * Check whether RDS is support in driver
     *
     * @return (true, support; false, not support)
     */
    public boolean isRdsSupported() {
        boolean isRdsSupported = (FmRadioNative.isRdsSupport() == 1);
        Log.d(TAG, "FmRadioService.isRdsSupported: " + isRdsSupported);
        return isRdsSupported;
    }

    /**
     * Check whether speaker used or not
     *
     * @return true if use speaker, otherwise return false
     */
    public boolean isSpeakerUsed() {
        Log.d(TAG, "FmRadioService.isSpeakerUsed: " + mIsSpeakerUsed);
        return mIsSpeakerUsed;
    }

    /**
     * Initial service and current station
     *
     * @param iCurrentStation current station frequency
     */
    public void initService(int iCurrentStation) {
        Log.d(TAG, "FmRadioService.initService: " + iCurrentStation);
        mIsServiceInited = true;
        mCurrentStation = iCurrentStation;
    }

    /**
     * Check service is initialed or not
     *
     * @return true if initialed, otherwise return false
     */
    public boolean isServiceInited() {
        Log.d(TAG, "FmRadioService.isServiceInit: " + mIsServiceInited);
        return mIsServiceInited;
    }

    /**
     * Get FM service current station frequency
     *
     * @return Current station frequency
     */
    public int getFrequency() {
        Log.d(TAG, "FmRadioService.getFrequency: " + mCurrentStation);
        return mCurrentStation;
    }

    /**
     * Set FM service station frequency
     *
     * @param station Current station
     */
    public void setFrequency(int station) {
        mCurrentStation = station;
    }

    /**
     * resume FM audio
     */
    private void resumeFmAudio() {
        Log.d(TAG, "FmRadioService.resumeFmAudio");
        // If not check mIsAudioFocusHeld && mIsPowerup, when scan canceled,
        // this will be resume first, then execute power down. it will cause
        // nosise.
        if (mIsAudioFocusHeld && mIsPowerUp) {
            enableFmAudio(true);
        }
    }

    /**
     * Switch antenna There are two types of antenna(long and short) If long
     * antenna(most is this type), must plug in earphone as antenna to receive
     * FM. If short antenna, means there is a short antenna if phone already,
     * can receive FM without earphone.
     *
     * @param antenna antenna (0, long antenna, 1 short antenna)
     *
     * @return (0, success; 1 failed; 2 not support)
     */
    public void switchAntennaAsync(int antenna) {
        final int bundleSize = 1;
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SWITCH_ANNTENNA);

        Bundle bundle = new Bundle(bundleSize);
        bundle.putInt(FmRadioListener.SWITCH_ANNTENNA_VALUE, antenna);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_SWITCH_ANNTENNA);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    /**
     * Need native support whether antenna support interface.
     *
     * @param antenna antenna (0, long antenna, 1 short antenna)
     *
     * @return (0, success; 1 failed; 2 not support)
     */
    private int switchAntenna(int antenna) {
        Log.d(TAG, ">>> FmRadioService.switchAntenna:" + antenna);
        // if fm not powerup, switchAntenna will flag whether has earphone
        int ret = FmRadioNative.switchAntenna(antenna);
        Log.d(TAG, "<<< FmRadioService.switchAntenna: " + ret);
        return ret;
    }

    /**
     * Start recording
     */
    public void startRecordingAsync() {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_STARTRECORDING_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_STARTRECORDING_FINISHED);
    }

    private void startRecording() {
        Log.d(TAG, ">>> startRecording");
        if (!mIsPowerUp) {
             Log.d(TAG, "native is not power up: " + mIsPowerUp);
             onRecorderError(FmRecorder.ERROR_RECORDER_INVALID_STATE);
             return;
        }
        sRecordingSdcard = FmRadioUtils.getDefaultStoragePath();
        Log.d(TAG, "default sd card file path: " + sRecordingSdcard);
        if (sRecordingSdcard == null || sRecordingSdcard.isEmpty()) {
            Log.d(TAG, "startRecording: may be no sdcard");
            onRecorderError(FmRecorder.ERROR_SDCARD_NOT_PRESENT);
            return;
        }

        if (mFmRecorder == null) {
            mFmRecorder = new FmRecorder();
            mFmRecorder.registerRecorderStateListener(FmRadioService.this);
        }

        if (isSdcardReady(sRecordingSdcard)) {
            mFmRecorder.startRecording(getApplicationContext());
        } else {
            Log.d(TAG, "Cannot record because sdcard is not ready!!");
            onRecorderError(FmRecorder.ERROR_SDCARD_NOT_PRESENT);
        }
        Log.d(TAG, "<<< startRecording");
    }

    private boolean isSdcardReady(String sdcardPath) {
        Log.d(TAG, ">>> isSdcardReady: sdcardPath is " + sdcardPath +
                ", mSdcardStateMap is " + mSdcardStateMap);
        if (!mSdcardStateMap.isEmpty()) {
            if (mSdcardStateMap.get(sdcardPath) != null && !mSdcardStateMap.get(sdcardPath)) {
                Log.d(TAG, "<<< isSdcardReady: return false");
                return false;
            }
        }
        Log.d(TAG, "<<< isSdcardReady: mSdcardStateMap:" + mSdcardStateMap);
        return true;
    }

    /**
     * stop recording
     */
    public void stopRecordingAsync() {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_STOPRECORDING_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_STOPRECORDING_FINISHED);
    }

    private boolean stopRecording() {
        Log.d(TAG, ">>> stopRecording");
        if (mFmRecorder == null) {
            Log.e(TAG, "stopRecording called without a valid recorder!!");
            return false;
        }
        synchronized (mStopRecordingLock) {
            mFmRecorder.stopRecording();
            Log.d(TAG, "<<< stopRecording");
        }
        return true;
    }

    /**
     * Start play recording file
     */
    public void startPlaybackAsync() {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_STARTPLAYBACK_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_STARTPLAYBACK_FINISHED);
    }

    private boolean startPlayback() {
        Log.d(TAG, ">>> startPlayback");
        if (!requestAudioFocus()) {
            Log.d(TAG, "can't get audio focus when play recording file");
            return false;
        }

        if (mFmRecorder == null) {
            Log.e(TAG, "FMRecorder is null !!");
            return false;
        }

        // Set Mute before start playback
        mAudioManager.setParameters("AudioFmPreStop=1");
        setMute(true);
        enableFmAudio(false);

        mFmRecorder.startPlayback();
        Log.d(TAG, "<<< startPlayback");
        return true;
    }

    /**
     * stop play recording file
     */
    public void stopPlaybackAsync() {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_STOPPLAYBACK_FINISHED);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_STOPPLAYBACK_FINISHED);
    }

    private void stopPlayback() {
        Log.d(TAG, ">>> stopPlayback");
        if (mFmRecorder != null) {
            mFmRecorder.stopPlayback();
            checkAfterPlayback();
        }
        Log.d(TAG, "<<< stopPlayback");
    }

    /**
     * Save recording file according name or discard recording file if name is
     * null
     *
     * @param newName New recording file name
     */
    public void saveRecordingAsync(String newName) {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_SAVERECORDING_FINISHED);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putString(RECODING_FILE_NAME, newName);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_SAVERECORDING_FINISHED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private void saveRecording(String newName) {
        Log.d(TAG, ">>> saveRecording");
        if (mFmRecorder != null) {
            if (newName != null) {
                mFmRecorder.saveRecording(FmRadioService.this, newName);
                Log.d(TAG, "<<< saveRecording");
                return;
            }
            mFmRecorder.discardRecording();
        }
        Log.d(TAG, "<<< saveRecording");
    }

    /**
     * Get record time
     *
     * @return Record time
     */
    public long getRecordTime() {
        if (mFmRecorder != null) {
            return mFmRecorder.recordTime();
        }
        Log.e(TAG, "FMRecorder is null !!");
        return 0;
    }

    /**
     * Set recording mode
     *
     * @param isRecording true, enter recoding mode; false, exit recording mode
     */
    public void setRecordingModeAsync(boolean isRecording) {
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_RECORD_MODE_CHANED);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putBoolean(OPTION, isRecording);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_RECORD_MODE_CHANED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    private void setRecordingMode(boolean isRecording) {
        Log.d(TAG, ">>> setRecordingMode: isRecording=" + isRecording);
        mIsInRecordingMode = isRecording;
        if (mFmRecorder != null) {
            if (!isRecording) {
                if (mFmRecorder.getState() != FmRecorder.STATE_IDLE) {
                    mFmRecorder.stopRecording();
                    mFmRecorder.stopPlayback();
                }
                resumeFmAudio();
                setMute(false);
                Log.d(TAG, "<<< setRecordingMode");
                return;
            }
            // reset recorder to unused status
            mFmRecorder.resetRecorder();
        }
        Log.d(TAG, "<<< setRecordingMode");
    }

    /**
     * Get current recording mode
     *
     * @return if in recording mode return true, otherwise return false;
     */
    public boolean getRecordingMode() {
        return mIsInRecordingMode;
    }

    /**
     * Get record state
     *
     * @return record state
     */
    public int getRecorderState() {
        if (null != mFmRecorder) {
            return mFmRecorder.getState();
        }
        return FmRecorder.STATE_INVALID;
    }

    /**
     * Get recording file name
     *
     * @return recording file name
     */
    public String getRecordingName() {
        if (null != mFmRecorder) {
            return mFmRecorder.getRecordingName();
        }
        return null;
    }

    /**
     * Get current recording file name with full path
     *
     * @return The current recording file name or null
     */
    public String getRecordingNameWithPath() {
        if (null != mFmRecorder) {
            return mFmRecorder.getRecordingNameWithPath();
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, ">>> FmRadioService.onCreate");
        Log.d(TAG, "short antenna support:" + SHORT_ANNTENNA_SUPPORT);
        mContext = getApplicationContext();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);
        sRecordingSdcard = FmRadioUtils.getDefaultStoragePath();

        if (!initFmPlayer()) {
            Log.e(TAG, "init FMPlayer failed");
            return;
        }

        registerFmBroadcastReceiver();
        registerSdcardReceiver();

        HandlerThread handlerThread = new HandlerThread("FmRadioServiceThread");
        handlerThread.start();
        mFmServiceHandler = new FmRadioServiceHandler(handlerThread.getLooper());

        openDevice();
        // set speaker to default status, avoid setting->clear data.
        setSpeakerPhoneOn(mIsSpeakerUsed);
        Log.d(TAG, "<<< FmRadioService.onCreate");
    }

    private boolean initFmPlayer() {
        mFmPlayer = new MediaPlayer();
        if (!FmRadioUtils.isFmSuspendSupport()) {
            mFmPlayer.setWakeMode(FmRadioService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }
        mFmPlayer.setOnErrorListener(mPlayerErrorListener);
        try {
            mFmPlayer.setDataSource("THIRDPARTY://MEDIAPLAYER_PLAYERTYPE_FM");
        } catch (IOException ex) {
            // notify the user why the file couldn't be opened
            Log.e(TAG, "setDataSource: " + ex);
            return false;
        } catch (IllegalArgumentException ex) {
            // notify the user why the file couldn't be opened
            Log.e(TAG, "setDataSource: " + ex);
            return false;
        } catch (SecurityException ex) {
            Log.e(TAG, "setDataSource: " + ex);
            return false;
        } catch (IllegalStateException ex) {
            Log.e(TAG, "setDataSource: " + ex);
            return false;
        }
        mFmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        return true;
    }

    private void registerFmBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SOUND_POWER_DOWN_MSG);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
/* Vanzo:tanglei on: Fri, 13 Mar 2015 21:22:01 +0800
 */
        //if (FeatureOption.VANZO_FEATURE_EARPHONE_KEY_ACTION) {
        //    filter.addAction("com.mediatek.FMRadio.FMRadioService.NEXT_STATION");
        //}
// End of Vanzo:tanglei
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_TOFMSERVICE_POWERDOWN);
        filter.addAction(ACTION_FROMATVSERVICE_POWERUP);
        mBroadcastReceiver = new FmServiceBroadcastReceiver();
        Log.i(TAG, "Register broadcast receiver.");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void unregisterFmBroadcastReceiver() {
        if (null != mBroadcastReceiver) {
            Log.i(TAG, "Unregister broadcast receiver.");
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, ">>> FmRadioService.onDestroy");
        mAudioManager.setParameters("AudioFmPreStop=1");
        setMute(true);
        // stop rds first, avoid blocking other native method
        if (isRdsSupported()) {
            Log.d(TAG, "RDS is supported. Stop the RDS thread.");
            stopRdsThread();
        }
        unregisterFmBroadcastReceiver();
        unregisterSdcardListener();
        abandonAudioFocus();
        exitFm();
        if (null != mFmRecorder) {
            mFmRecorder = null;
        }
        super.onDestroy();
    }

    /**
     * Exit FMRadio application
     */
    private void exitFm() {
        Log.d(TAG, "service.exitFm start");
        mIsAudioFocusHeld = false;
        // Stop FM recorder if it is working
        if (null != mFmRecorder) {
            synchronized (mStopRecordingLock) {
                int fmState = mFmRecorder.getState();
                if (FmRecorder.STATE_PLAYBACK == fmState) {
                    mFmRecorder.stopPlayback();
                    Log.d(TAG, "Stop playback FMRecorder.");
                } else if (FmRecorder.STATE_RECORDING == fmState) {
                    mFmRecorder.stopRecording();
                    Log.d(TAG, "stop Recording.");
                }

                // ALPS01789667 Add to DB if exit, there are two cases:
                // case 1: FileManager play Music->FM receive short audio focus->Fm stop recording
                // -> FM receive SOUND_POWER_DOWN_MSG->Come here but is IDLE status
                // case 2: Music play a song->FM receive long audio focus->Come here is RECORDING
                mFmRecorder.addCurrentRecordingToDb(mContext);
            }
        }

        // When exit, we set the audio path back to earphone.
        if (mIsNativeScanning || mIsNativeSeeking) {
            stopScan();
        }

        mFmServiceHandler.removeCallbacksAndMessages(null);
        mFmServiceHandler.removeMessages(FmRadioListener.MSGID_FM_EXIT);
        mFmServiceHandler.sendEmptyMessage(FmRadioListener.MSGID_FM_EXIT);
        Log.d(TAG, "service.exitFm end");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Change the notification string.
        if (mIsPowerUp) {
            showNotification();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, ">>> FmRadioService.onStartCommand intent: " + intent + " startId: " + startId);
        int ret = super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "<<< FmRadioService.onStartCommand: " + ret);
        return START_NOT_STICKY;
    }

    /**
     * Start RDS thread to update RDS information
     */
    private void startRdsThread() {
        Log.d(TAG, ">>> FmRadioService.startRdSThread");
        mIsRdsThreadExit = false;
        if (null != mRdsThread) {
            return;
        }
        mRdsThread = new Thread() {
            public void run() {
                Log.d(TAG, ">>> RDS Thread run()");
                while (true) {
                    if (mIsRdsThreadExit) {
                        break;
                    }

                    int iRdsEvents = FmRadioNative.readRds();
                    if (iRdsEvents != 0) {
                        Log.d(TAG, "FmRadioNative.readrds events: " + iRdsEvents);
                    }

                    if (RDS_EVENT_PROGRAMNAME == (RDS_EVENT_PROGRAMNAME & iRdsEvents)) {
                        Log.d(TAG, "RDS_EVENT_PROGRAMNAME");
                        byte[] bytePS = FmRadioNative.getPs();
                        if (null != bytePS) {
                            setPS(new String(bytePS).trim());
                        }
                    }

                    if (RDS_EVENT_LAST_RADIOTEXT == (RDS_EVENT_LAST_RADIOTEXT & iRdsEvents)) {
                        Log.d(TAG, "RDS_EVENT_LAST_RADIOTEXT");
                        byte[] byteLRText = FmRadioNative.getLrText();
                        if (null != byteLRText) {
                            setLRText(new String(byteLRText).trim());
                        }
                    }

                    if (RDS_EVENT_AF == (RDS_EVENT_AF & iRdsEvents)) {
                        Log.d(TAG, "RDS_EVENT_AF");
                        /*
                         * add for rds AF
                         */
                        if (mIsScanning || mIsSeeking) {
                            Log.d(TAG, "RDSThread. seek or scan going, no need to tune here");
                        } else if (!mIsPowerUp) {
                            Log.d(TAG, "RDSThread. fm is power down, do nothing.");
                        } else {
                            int iFreq = FmRadioNative.activeAf();
                            if (FmRadioUtils.isValidStation(iFreq)) {
                                // if the new frequency is not equal to current
                                // frequency.
                                if (mCurrentStation == iFreq) {
                                    Log.w(TAG, "RDSThread. the new freq is the same as current.");
                                } else {
                                    setPS("");
                                    setLRText("");
                                    if (!mIsScanning && !mIsSeeking) {
                                        Log.d(TAG, "RDSThread. seek or scan not going," +
                                                "need to tune here");
                                        tuneStationAsync(FmRadioUtils.computeFrequency(iFreq));
                                    }
                                }
                            }
                        }
                    }
                    // Do not handle other events.
                    // Sleep 500ms to reduce inquiry frequency
                    try {
                        final int hundredMillisecond = 500;
                        Thread.sleep(hundredMillisecond);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "<<< RDS Thread run()");
            }
        };

        Log.d(TAG, "Start RDS Thread.");
        mRdsThread.start();
        Log.d(TAG, "<<< FmRadioService.startRdSThread");
    }

    /**
     * Stop RDS thread to stop listen station RDS change
     */
    private void stopRdsThread() {
        Log.d(TAG, ">>> FmRadioService.stopRdSThread");
        if (null != mRdsThread) {
            // Must call closedev after stopRDSThread.
            mIsRdsThreadExit = true;
            mRdsThread = null;
        }
        Log.d(TAG, "<<< FmRadioService.stopRdSThread");
    }

    /**
     * Set PS information
     *
     * @param ps The ps information
     */
    private void setPS(String ps) {
        Log.d(TAG, "FmRadioService.setPS: " + ps + " ,current: " + mPSString);
        if (0 != mPSString.compareTo(ps)) {
            mPSString = ps;
            Bundle bundle = new Bundle(3);
            bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.LISTEN_PS_CHANGED);
            bundle.putString(FmRadioListener.KEY_PS_INFO, mPSString);
            bundle.putString(FmRadioListener.KEY_RT_INFO, mLRTextString);
            notifyActivityStateChanged(bundle);
        } // else New PS is the same as current
    }

    /**
     * Set RT information
     *
     * @param lrtText The RT information
     */
    private void setLRText(String lrtText) {
        Log.d(TAG, "FmRadioService.setLRText: " + lrtText + " ,current: " + mLRTextString);
        if (0 != mLRTextString.compareTo(lrtText)) {
            mLRTextString = lrtText;
            Bundle bundle = new Bundle(3);
            bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.LISTEN_RT_CHANGED);
            bundle.putString(FmRadioListener.KEY_PS_INFO, mPSString);
            bundle.putString(FmRadioListener.KEY_RT_INFO, mLRTextString);
            notifyActivityStateChanged(bundle);
        } // else New RT is the same as current
    }

    /**
     * Open or close FM Radio audio
     *
     * @param enable true, open FM audio; false, close FM audio;
     */
    private void enableFmAudio(boolean enable) {
        Log.d(TAG, ">>> FmRadioService.enableFmAudio: " + enable);
        if ((mFmPlayer == null) || !mIsPowerUp) {
            Log.w(TAG, "mFMPlayer is null in Service.enableFmAudio");
            return;
        }

        try {
            if (!enable) {
                if (!mFmPlayer.isPlaying()) {
                    Log.d(TAG, "warning: FM audio is already disabled.");
                    return;
                }

                Log.d(TAG, "call MediaPlayer.stop()");
                mFmPlayer.stop();
                Log.d(TAG, "stop FM audio.");
                return;
            }

            if (mFmPlayer.isPlaying()) {
                Log.d(TAG, "warning: FM audio is already enabled.");
                return;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Exception: Cannot call MediaPlayer isPlaying.", e);
        }

        try {
            mFmPlayer.prepare();
            if (FmRadioUtils.isFmSuspendSupport()) {
                Log.d(TAG, "support FM suspend");
                mFmPlayer.startWithoutWakelock();
            } else {
                mFmPlayer.start();
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception: Cannot call MediaPlayer prepare.", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Exception: Cannot call MediaPlayer prepare.", e);
        }

        Log.d(TAG, "Start FM audio.");
        Log.d(TAG, "<<< FmRadioService.enableFmAudio");
    }

    /**
     * Show notification
     */
    private void showNotification() {
        Log.d(TAG, "FmRadioService.showNotification");
        Intent notificationIntent = new Intent();
        notificationIntent.setClassName(getPackageName(), FmRadioActivity.class.getName());
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, 0);
        Notification notification = new Notification(R.drawable.fm_title_icon, null,
                System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        String fmUnit = mContext.getString(R.string.fm_unit);
        String text = FmRadioUtils.formatStation(mCurrentStation) + " " + fmUnit;
        notification.setLatestEventInfo(getApplicationContext(),
                getResources().getString(R.string.app_name), text, pendingIntent);
        Log.d(TAG, "Add notification to the title bar.");
        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Remove notification
     */
    private void removeNotification() {
        Log.d(TAG, "FmRadioService.removeNotification");
        stopForeground(true);
    }

    /**
     * Update notification
     */
    private void updateNotification() {
        Log.d(TAG, "FmRadioService.updateNotification");
        if (mIsPowerUp) {
            showNotification();
        }
    }

    /**
     * Register sdcard listener for record
     */
    private void registerSdcardReceiver() {
        Log.v(TAG, "registerSdcardReceiver >>> ");
        if (mSdcardListener == null) {
            mSdcardListener = new SdcardListener();
        }
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("file");
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        registerReceiver(mSdcardListener, filter);
        Log.v(TAG, "registerSdcardReceiver <<< ");
    }

    private void unregisterSdcardListener() {
        if (null != mSdcardListener) {
            unregisterReceiver(mSdcardListener);
        }
    }

    private void updateSdcardStateMap(Intent intent) {
        String action = intent.getAction();
        String sdcardPath = null;
        Uri mountPointUri = intent.getData();
        if (mountPointUri != null) {
            sdcardPath = mountPointUri.getPath();
            if (sdcardPath != null) {
                if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                    Log.d(TAG, "updateSdcardStateMap: ENJECT " + sdcardPath);
                    mSdcardStateMap.put(sdcardPath, false);
                } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                    Log.d(TAG, "updateSdcardStateMap: UNMOUNTED " + sdcardPath);
                    mSdcardStateMap.put(sdcardPath, false);
                } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    Log.d(TAG, "updateSdcardStateMap: MOUNTED " + sdcardPath);
                    mSdcardStateMap.put(sdcardPath, true);
                }
            }
        }
    }

    /**
     * Notify FM recorder state
     *
     * @param state The current FM recorder state
     */
    @Override
    public void onRecorderStateChanged(int state) {
        Log.d(TAG, "onRecorderStateChanged: " + state);
        mRecordState = state;
        Bundle bundle = new Bundle(2);
        bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.LISTEN_RECORDSTATE_CHANGED);
        bundle.putInt(FmRadioListener.KEY_RECORDING_STATE, state);
        notifyActivityStateChanged(bundle);
    }

    /**
     * Notify FM recorder error message
     *
     * @param error The recorder error type
     */
    @Override
    public void onRecorderError(int error) {
        Log.d(TAG, "onRecorderError: " + error);
        // if media server die, will not enable FM audio, and convert to
        // ERROR_PLAYER_INATERNAL, call back to activity showing toast.
        mRecorderErrorType = (MediaPlayer.MEDIA_ERROR_SERVER_DIED == error) ?
                FmRecorder.ERROR_PLAYER_INTERNAL : error;

        Bundle bundle = new Bundle(2);
        bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.LISTEN_RECORDERROR);
        bundle.putInt(FmRadioListener.KEY_RECORDING_ERROR_TYPE, mRecorderErrorType);
        notifyActivityStateChanged(bundle);

        // if media server die, should not enable fm, otherwise je will occur.
        if (FmRecorder.ERROR_PLAYER_INTERNAL == error) {
            resumeFmAudio();
        }
    }

    /**
     * Notify play FM record file complete
     */
    @Override
    public void onPlayRecordFileComplete() {
        Log.d(TAG, "service.onPlayRecordFileComplete");
        checkAfterPlayback();
    }

    /**
     * Check and go next(play or show tips) after recorder file play
     * back finish.
     * Two cases:
     * 1. With headset or support short antenna -> play FM
     * 2. Without headset -> show plug in earphone tips
     */
    private void checkAfterPlayback() {
        if (isHeadSetIn() || SHORT_ANNTENNA_SUPPORT) {
            // with headset
            Log.d(TAG, "checkAfterPlayback:eaphone is in,need resume fm");
            if (mIsPowerUp) {
                resumeFmAudio();
                setMute(false);
            } else {
                powerUpAsync(FmRadioUtils.computeFrequency(mCurrentStation));
            }
        } else {
            // without headset need show plug in earphone tips
            Log.d(TAG, "checkAfterPlayback:earphone is out, need show plug in earphone tips");
            switchAntennaAsync(mValueHeadSetPlug);
        }
    }

    /**
     * Check the headset is plug in or plug out
     *
     * @return true for plug in; false for plug out
     */
    private boolean isHeadSetIn() {
        return (0 == mValueHeadSetPlug);
    }

    private void stopFmFocusLoss(int focusState) {
        mIsAudioFocusHeld = false;
        if (mIsNativeScanning || mIsNativeSeeking) {
            // make stop scan from activity call to service.
            // notifyActivityStateChanged(FMRadioListener.LISTEN_SCAN_CANCELED);
            stopScan();
            Log.d(TAG, "need to stop FM, so stop scan channel.");
        }

        // using handler thread to update audio focus state
        updateAudioFocusAync(focusState);
        Log.d(TAG, "need to stop FM, so powerdown FM.");

    }

    /**
     * Handle FM Player error
     */
    private final MediaPlayer.OnErrorListener mPlayerErrorListener =
            new MediaPlayer.OnErrorListener() {
                /**
                 * handle error message
                 *
                 * @param mp occurred error media player
                 * @param what error message
                 * @param extra error message extra
                 *
                 * @return handle error message or not
                 */
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    if (MediaPlayer.MEDIA_ERROR_SERVER_DIED == what) {
                        Log.d(TAG, "onError: MEDIA_SERVER_DIED");
                        if (null != mFmPlayer) {
                            mFmPlayer.release();
                            mFmPlayer = null;
                        }
                        mFmPlayer = new MediaPlayer();
                        if (!FmRadioUtils.isFmSuspendSupport()) {
                            mFmPlayer.setWakeMode(FmRadioService.this, PowerManager.PARTIAL_WAKE_LOCK);
                        }
                        mFmPlayer.setOnErrorListener(mPlayerErrorListener);
                        try {
                            mFmPlayer.setDataSource("THIRDPARTY://MEDIAPLAYER_PLAYERTYPE_FM");
                            mFmPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            if (mIsPowerUp) {
                                // set speaker mode according to AP
                                setSpeakerPhoneOn(mIsSpeakerUsed);
                                mFmPlayer.prepare();
                                if (FmRadioUtils.isFmSuspendSupport()) {
                                    Log.d(TAG, "support FM suspend");
                                    mFmPlayer.startWithoutWakelock();
                                } else {
                                    mFmPlayer.start();
                                }
                            }
                        } catch (IOException ex) {
                            Log.e(TAG, "setDataSource: " + ex);
                            return false;
                        } catch (IllegalArgumentException ex) {
                            Log.e(TAG, "setDataSource: " + ex);
                            return false;
                        } catch (IllegalStateException ex) {
                            Log.e(TAG, "setDataSource: " + ex);
                            return false;
                        }
                    }

                    return true;
                }
            };

    /**
     * Request audio focus
     *
     * @return true, success; false, fail;
     */
    public boolean requestAudioFocus() {
        if (mIsAudioFocusHeld) {
            return true;
        }

        int audioFocus = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mIsAudioFocusHeld = (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioFocus);
        return mIsAudioFocusHeld;
    }

    /**
     * Abandon audio focus
     */
    public void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        mIsAudioFocusHeld = false;
    }

    /**
     * Use to interact with other voice related app
     */
    private final OnAudioFocusChangeListener mAudioFocusChangeListener =
            new OnAudioFocusChangeListener() {
                /**
                 * Handle audio focus change ensure message FIFO
                 *
                 * @param focusChange audio focus change state
                 */
                @Override
                public void onAudioFocusChange(int focusChange) {
                    Log.d(TAG, "onAudioFocusChange: " + focusChange);
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            synchronized (this) {
                                Log.d(TAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                                mAudioManager.setParameters("AudioFmPreStop=1");
                                setMute(true);
                                Log.d(TAG, "onAudioFocusChange.setParameters end");
                                exitFm();
                                stopSelf();
                            }
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            synchronized (this) {
                                mAudioManager.setParameters("AudioFmPreStop=1");
                                setMute(true);
                                Log.d(TAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                                stopFmFocusLoss(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
                            }
                            break;

                        case AudioManager.AUDIOFOCUS_GAIN:
                            synchronized (this) {
                                Log.d(TAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                                updateAudioFocusAync(AudioManager.AUDIOFOCUS_GAIN);
                            }
                            break;

                        default:
                            Log.d(TAG, "AudioFocus: Audio focus change, but not need handle");
                            break;
                    }
                }
            };

    /**
     * Audio focus changed, will send message to handler thread. synchronized to
     * ensure one message can go in this method.
     *
     * @param focusState AudioManager state
     */
    private synchronized void updateAudioFocusAync(int focusState) {
        Log.d(TAG, "updateAudioFocusAync: focusState = " + focusState);
        final int bundleSize = 1;
        Bundle bundle = new Bundle(bundleSize);
        bundle.putInt(FmRadioListener.KEY_AUDIOFOCUS_CHANGED, focusState);
        Message msg = mFmServiceHandler.obtainMessage(FmRadioListener.MSGID_AUDIOFOCUS_CHANGED);
        msg.setData(bundle);
        mFmServiceHandler.sendMessage(msg);
    }

    /**
     * Audio focus changed, update FM focus state.
     *
     * @param focusState AudioManager state
     */
    private void updateAudioFocus(int focusState) {
        Log.d(TAG, "FmRadioService.updateAudioFocus");
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_LOSS:
                mPausedByTransientLossOfFocus = false;
                // play back audio will output with music audio
                // May be affect other recorder app, but the flow can not be
                // execute earlier,
                // It should ensure execute after start/stop record.
                if (mFmRecorder != null) {
                    int fmState = mFmRecorder.getState();
                    Log.d(TAG, "stopFMFocusLoss.recorder state=" + fmState);
                    // only handle recorder state, not handle playback state
                    if (fmState == FmRecorder.STATE_RECORDING) {
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_STARTRECORDING_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_STOPRECORDING_FINISHED);
                        stopRecording();
                    }
                }
                handlePowerDown();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mIsPowerUp) {
                    mPausedByTransientLossOfFocus = true;
                }
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT: mPausedByTransientLossOfFocus:" +
                        mPausedByTransientLossOfFocus);
                // play back audio will output with music audio
                // May be affect other recorder app, but the flow can not be
                // execute earlier,
                // It should ensure execute after start/stop record.
                if (mFmRecorder != null) {
                    int fmState = mFmRecorder.getState();
                    Log.d(TAG, "stopFMFocusLoss.recorder state=" + fmState);
                    if (fmState == FmRecorder.STATE_RECORDING) {
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_STARTRECORDING_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_STOPRECORDING_FINISHED);
                        stopRecording();
                    }
                    if (fmState == FmRecorder.STATE_PLAYBACK) {
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_STARTPLAYBACK_FINISHED);
                        mFmServiceHandler.removeMessages(
                                FmRadioListener.MSGID_STOPPLAYBACK_FINISHED);
                        stopPlayback();
                    }
                }
                handlePowerDown();
                break;

            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "AUDIOFOCUS_GAIN: mPausedByTransientLossOfFocus:" +
                        mPausedByTransientLossOfFocus);
                if (!mIsPowerUp && mPausedByTransientLossOfFocus) {
                    mIsPowerUping = true;
                    final int bundleSize = 1;
                    mFmServiceHandler.removeMessages(FmRadioListener.MSGID_POWERUP_FINISHED);
                    mFmServiceHandler.removeMessages(FmRadioListener.MSGID_POWERDOWN_FINISHED);
                    Bundle bundle = new Bundle(bundleSize);
                    bundle.putFloat(FM_FREQUENCY, FmRadioUtils.computeFrequency(mCurrentStation));
                    handlePowerUp(bundle);
                }
                break;

            default:
                break;
        }
    }

    /**
     * FM Radio listener record
     */
    private static class Record {
        int mHashCode; // hash code
        FmRadioListener mCallback; // call back
    }

    /**
     * Register FM Radio listener, activity get service state should call this
     * method register FM Radio listener
     *
     * @param callback FM Radio listener
     */
    public void registerFmRadioListener(FmRadioListener callback) {
        synchronized (mRecords) {
            // register callback in AudioProfileService, if the callback is
            // exist, just replace the event.
            Record record = null;
            int hashCode = callback.hashCode();
            final int n = mRecords.size();
            for (int i = 0; i < n; i++) {
                record = mRecords.get(i);
                if (hashCode == record.mHashCode) {
                    return;
                }
            }
            record = new Record();
            record.mHashCode = hashCode;
            record.mCallback = callback;
            mRecords.add(record);
        }
    }

    /**
     * Call back from service to activity
     *
     * @param bundle The message to activity
     */
    private void notifyActivityStateChanged(Bundle bundle) {
        if (!mRecords.isEmpty()) {
            Log.d(TAG, "notifyActivityStatusChanged:clients = " + mRecords.size());
            synchronized (mRecords) {
                Iterator<Record> iterator = mRecords.iterator();
                while (iterator.hasNext()) {
                    Record record = (Record) iterator.next();

                    FmRadioListener listener = record.mCallback;

                    if (listener == null) {
                        iterator.remove();
                        return;
                    }

                    listener.onCallBack(bundle);
                }
            }
        }
    }

    /**
     * Unregister FM Radio listener
     *
     * @param callback FM Radio listener
     */
    public void unregisterFmRadioListener(FmRadioListener callback) {
        remove(callback.hashCode());
    }

    /**
     * Remove call back according hash code
     *
     * @param hashCode The call back hash code
     */
    private void remove(int hashCode) {
        synchronized (mRecords) {
            Iterator<Record> iterator = mRecords.iterator();
            while (iterator.hasNext()) {
                Record record = (Record) iterator.next();
                if (record.mHashCode == hashCode) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Check recording sd card is unmount
     *
     * @param intent The unmount sd card intent
     *
     * @return true or false indicate whether current recording sd card is
     *         unmount or not
     */
    public boolean isRecordingCardUnmount(Intent intent) {
        String unmountSDCard = intent.getData().toString();
        Log.d(TAG, "unmount sd card file path: " + unmountSDCard);
        return unmountSDCard.equalsIgnoreCase("file://" + sRecordingSdcard) ? true : false;
    }

    private int[] insertSearchedStation(int[] channels) {
        Log.d(TAG, "insertSearchedStation.firstValidChannel:" + Arrays.toString(channels));
        int firstValidChannel = mCurrentStation;
        int channelNum = 0;
        if (null != channels) {
            Arrays.sort(channels);
            int size = channels.length;
            // Save searched stations into database by batch
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            String defaultStationName = getString(R.string.default_station_name);
            for (int i = 0; i < size; i++) {
                if (FmRadioUtils.isValidStation(channels[i])) {
                    if (0 == channelNum) {
                        firstValidChannel = channels[i];
                    }

                    if (!FmRadioStation.isFavoriteStation(mContext, channels[i])) {
                        ops.add(ContentProviderOperation.newInsert(Station.CONTENT_URI)
                                .withValue(Station.COLUMN_STATION_NAME, defaultStationName)
                                .withValue(Station.COLUMN_STATION_FREQ, channels[i])
                                .withValue(Station.COLUMN_STATION_TYPE,
                                        FmRadioStation.STATION_TYPE_SEARCHED)
                                .build());
                    }
                    channelNum++;
                }
            }
            // Save search stations to database by batch
            try {
                mContext.getContentResolver().applyBatch(FmRadioStation.AUTHORITY, ops);
            } catch (RemoteException e) {
                Log.d(TAG, "Exception when applyBatch searched stations " + e);
            } catch (OperationApplicationException e) {
                Log.d(TAG, "Exception when applyBatch searched stations " + e);
            }
        }
        Log.d(TAG, "insertSearchedStation.firstValidChannel:" + firstValidChannel +
                ",channelNum:" + channelNum);
        return (new int[] {
                firstValidChannel, channelNum
        });
    }

    /**
     * The background handler
     */
    class FmRadioServiceHandler extends Handler {
        public FmRadioServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            boolean isPowerup = false;
            boolean isSwitch = true;

            switch (msg.what) {

                // power up
                case FmRadioListener.MSGID_POWERUP_FINISHED:
                    bundle = msg.getData();
                    handlePowerUp(bundle);
                    break;

                // power down
                case FmRadioListener.MSGID_POWERDOWN_FINISHED:
                    handlePowerDown();
                    break;

                // fm exit
                case FmRadioListener.MSGID_FM_EXIT:
                    if (mIsSpeakerUsed) {
                        setSpeakerPhoneOn(false);
                    }
                    powerDown();
                    closeDevice();
                    // Release FM player upon exit
                    if (null != mFmPlayer) {
                        mFmPlayer.release();
                        mFmPlayer = null;
                    }

                    bundle = new Bundle(1);
                    bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.MSGID_FM_EXIT);
                    notifyActivityStateChanged(bundle);
                    // Finish favorite when exit FM
                    if (sExitListener != null) {
                        sExitListener.onExit();
                    }
                    break;

                // switch antenna
                case FmRadioListener.MSGID_SWITCH_ANNTENNA:
                    bundle = msg.getData();
                    int value = bundle.getInt(FmRadioListener.SWITCH_ANNTENNA_VALUE);

                 // if not support short antenna, just notify, not need to switch antenna.
                    if (SHORT_ANNTENNA_SUPPORT) {
                        isSwitch = (switchAntenna(value) == 0) ? true : false;
                        Log.d(TAG, "FmServiceHandler.switch anntenna:isSwitch:" + isSwitch);
                    } else {
                        // if ear phone insert, need dismiss plugin earphone
                        // dialog
                        // if earphone plug out and it is not play recorder
                        // state, show plug dialog.
                        if (0 == value) {
                            Log.d(TAG, "FmServiceHandler.switch anntenna:dismiss dialog");
                            // powerUpAsync(FMRadioUtils.computeFrequency(mCurrentStation));
                            bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                                    FmRadioListener.MSGID_SWITCH_ANNTENNA);
                            bundle.putBoolean(FmRadioListener.KEY_IS_SWITCH_ANNTENNA, true);
                            notifyActivityStateChanged(bundle);
                        } else {
                            // ear phone plug out, and recorder state is not
                            // play recorder state,
                            // show dialog.
                            if (mRecordState != FmRecorder.STATE_PLAYBACK) {
                                Log.d(TAG, "FmServiceHandler.switch anntenna:show dialog");
                                bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                                        FmRadioListener.MSGID_SWITCH_ANNTENNA);
                                bundle.putBoolean(FmRadioListener.KEY_IS_SWITCH_ANNTENNA, false);
                                notifyActivityStateChanged(bundle);
                            }
                        }
                    }
                    break;

                // tune to station
                case FmRadioListener.MSGID_TUNE_FINISHED:
                    bundle = msg.getData();
                    float tuneStation = bundle.getFloat(FM_FREQUENCY);
                    boolean isTune = tuneStation(tuneStation);
                    // if tune fail, pass current station to update ui
                    if (!isTune) {
                        tuneStation = FmRadioUtils.computeFrequency(mCurrentStation);
                    }
                    bundle = new Bundle(4);
                    bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                            FmRadioListener.MSGID_TUNE_FINISHED);
                    bundle.putBoolean(FmRadioListener.KEY_IS_TUNE, isTune);
                    bundle.putFloat(FmRadioListener.KEY_TUNE_TO_STATION, tuneStation);
                    bundle.putBoolean(FmRadioListener.KEY_IS_POWER_UP, mIsPowerUp);
                    notifyActivityStateChanged(bundle);
                    break;

                // seek to station
                case FmRadioListener.MSGID_SEEK_FINISHED:
                    bundle = msg.getData();
                    mIsSeeking = true;
                    float seekStation = seekStation(bundle.getFloat(FM_FREQUENCY),
                            bundle.getBoolean(OPTION));
                    boolean isSeekTune = false;
                    int station = FmRadioUtils.computeStation(seekStation);
                    if (FmRadioUtils.isValidStation(station)) {
                        isSeekTune = tuneStation(seekStation);
                    }
                    // if tune fail, pass current station to update ui
                    if (!isSeekTune) {
                        seekStation = FmRadioUtils.computeFrequency(mCurrentStation);
                    }
                    bundle = new Bundle(2);
                    bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                            FmRadioListener.MSGID_TUNE_FINISHED);
                    bundle.putBoolean(FmRadioListener.KEY_IS_TUNE, isSeekTune);
                    bundle.putFloat(FmRadioListener.KEY_TUNE_TO_STATION, seekStation);
                    notifyActivityStateChanged(bundle);
                    mIsSeeking = false;
                    break;

                // start scan
                case FmRadioListener.MSGID_SCAN_FINISHED:
                    int[] channels = null;
                    int[] result = null;
                    int scanTuneStation = 0;
                    boolean isScan = true;
                    mIsScanning = true;
                    if (powerUpFm(FmRadioUtils.DEFAULT_STATION_FLOAT)) {
                        channels = startScan();
                    }

                    // check whether cancel scan
                    if ((null != channels) && channels[0] == -100) {
                        Log.d(TAG, "user canceled scan:channels[0]=" + channels[0]);
                        isScan = false;
                        result = new int[] {
                                -1, 0
                        };
                    } else {
                        result = insertSearchedStation(channels);
                        scanTuneStation = result[0];
                        isTune = tuneStation(FmRadioUtils.computeFrequency(scanTuneStation));
                        scanTuneStation = isTune ? scanTuneStation : mCurrentStation;
                    }

                    /*
                     * if there is stop command when scan, so it needs to mute
                     * fm avoid fm sound come out.
                     */
                    if (mIsAudioFocusHeld) {
                        Log.d(TAG, "there is not power down command.set mute false");
                        setMute(false);
                    }
                    bundle = new Bundle(4);
                    bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                            FmRadioListener.MSGID_SCAN_FINISHED);
                    bundle.putInt(FmRadioListener.KEY_TUNE_TO_STATION, scanTuneStation);
                    bundle.putInt(FmRadioListener.KEY_STATION_NUM, result[1]);
                    bundle.putBoolean(FmRadioListener.KEY_IS_SCAN, isScan);
                    notifyActivityStateChanged(bundle);
                    mIsScanning = false;
                    break;

                // audio focus changed
                case FmRadioListener.MSGID_AUDIOFOCUS_CHANGED:
                    bundle = msg.getData();
                    int focusState = bundle.getInt(FmRadioListener.KEY_AUDIOFOCUS_CHANGED);
                    updateAudioFocus(focusState);
                    break;

                case FmRadioListener.MSGID_SET_RDS_FINISHED:
                    bundle = msg.getData();
                    setRds(bundle.getBoolean(OPTION));
                    break;

                case FmRadioListener.MSGID_SET_MUTE_FINISHED:
                    bundle = msg.getData();
                    setMute(bundle.getBoolean(OPTION));
                    break;

                case FmRadioListener.MSGID_ACTIVE_AF_FINISHED:
                    activeAF();
                    break;

                /********** recording **********/
                case FmRadioListener.MSGID_STARTRECORDING_FINISHED:
                    startRecording();
                    break;

                case FmRadioListener.MSGID_STOPRECORDING_FINISHED:
                    stopRecording();
                    break;

                case FmRadioListener.MSGID_STARTPLAYBACK_FINISHED:
                    boolean isStart = startPlayback();
                    // Can not start play back, call back to activity.
                    if (!isStart) {
                        bundle = new Bundle(2);
                        bundle.putInt(FmRadioListener.CALLBACK_FLAG,
                                FmRadioListener.LISTEN_RECORDERROR);
                        bundle.putInt(FmRadioListener.KEY_RECORDING_ERROR_TYPE,
                                FmRadioListener.NOT_AUDIO_FOCUS);
                        notifyActivityStateChanged(bundle);
                    }
                    break;

                case FmRadioListener.MSGID_STOPPLAYBACK_FINISHED:
                    stopPlayback();
                    break;

                case FmRadioListener.MSGID_RECORD_MODE_CHANED:
                    bundle = msg.getData();
                    setRecordingMode(bundle.getBoolean(OPTION));
                    break;

                case FmRadioListener.MSGID_SAVERECORDING_FINISHED:
                    bundle = msg.getData();
                    saveRecording(bundle.getString(RECODING_FILE_NAME));
                    break;

                default:
                    break;
            }
        }

    }

    /**
     * handle power down, execute power down and call back to activity.
     */
    private void handlePowerDown() {
        Bundle bundle;
        boolean isPowerdown = powerDown();
        bundle = new Bundle(2);
        bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.MSGID_POWERDOWN_FINISHED);
        bundle.putBoolean(FmRadioListener.KEY_IS_POWER_DOWN, isPowerdown);
        notifyActivityStateChanged(bundle);
    }

    /**
     * handle power up, execute power up and call back to activity.
     *
     * @param bundle power up frequency
     */
    private void handlePowerUp(Bundle bundle) {
        boolean isPowerup = false;
        boolean isSwitch = true;
        Log.d(TAG, "service handler power up start");
        float curFrequency = bundle.getFloat(FM_FREQUENCY);

        if (!SHORT_ANNTENNA_SUPPORT && !isAntennaAvailable()) {
            Log.d(TAG, "call back to activity, earphone is not ready");
            mIsPowerUping = false;
            bundle = new Bundle(2);
            bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.MSGID_SWITCH_ANNTENNA);
            bundle.putBoolean(FmRadioListener.KEY_IS_SWITCH_ANNTENNA, false);
            notifyActivityStateChanged(bundle);
            return;
        }

        if (powerUpFm(curFrequency)) {
            isPowerup = startPlayFm(curFrequency);
            mPausedByTransientLossOfFocus = false;
        }
        mIsPowerUping = false;
        bundle = new Bundle(2);
        bundle.putInt(FmRadioListener.CALLBACK_FLAG, FmRadioListener.MSGID_POWERUP_FINISHED);
        bundle.putBoolean(FmRadioListener.KEY_IS_POWER_UP, isPowerup);
        notifyActivityStateChanged(bundle);
        Log.d(TAG, "service handler power up end");
    }

    /**
     * check FM is foreground or background
     */
    public boolean isActivityForeground() {
        boolean isForeground = true;
        List<RunningAppProcessInfo> appProcessInfos = mActivityManager.getRunningAppProcesses();
        for (RunningAppProcessInfo appProcessInfo : appProcessInfos) {
            if (appProcessInfo.processName.equals(mContext.getPackageName())) {
                int importance = appProcessInfo.importance;
                Log.d(TAG, "isActivityForeground importance:" + importance);
                if (importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                        importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    Log.d(TAG, "isActivityForeground is foreground");
                    isForeground = true;
                } else {
                    Log.d(TAG, "isActivityForeground is background");
                    isForeground = false;
                }
                break;
            }
        }
        Log.d(TAG, "isActivityForeground return " + isForeground);
        return isForeground;
    }

    /**
     * Check if current is lock task mode. If in this mode, AMS will cannot destory
     * FmRadioActivity even call finish()
     * Settings->Security->Screen pinning on
     * @return true if current screen pinning on FmRadioActivity
     */
    public boolean isInLockTaskMode() {
        Log.d(TAG, "isInLockTaskMode:" + mActivityManager.isInLockTaskMode());
        return mActivityManager.isInLockTaskMode();
    }

    /**
     * Get the recording sdcard path when staring record
     *
     * @return sdcard path like "/storage/sdcard0"
     */
    public static String getRecordingSdcard() {
        return sRecordingSdcard;
    }

    /**
     * The listener interface for exit
     */
    public interface OnExitListener {
        /**
         * When Service finish, should notify FmRadioFavorite to finish
         */
        void onExit();
    }

    /**
     * Register the listener for exit
     *
     * @param listener The listener want to know the exit event
     */
    public static void registerExitListener(OnExitListener listener) {
        sExitListener = listener;
    }

    /**
     * Unregister the listener for exit
     *
     * @param listener The listener want to know the exit event
     */
    public static void unregisterExitListener(OnExitListener listener) {
        sExitListener = null;
    }

    /**
     * Get the latest recording name the show name in save dialog but saved in
     * service
     *
     * @return The latest recording name or null for not modified
     */
    public String getModifiedRecordingName() {
        Log.d(TAG, "getRecordingNameInDialog:" + mModifiedRecordingName);
        return mModifiedRecordingName;
    }

    /**
     * Set the latest recording name if modify the default name
     *
     * @param name The latest recording name or null for not modified
     */
    public void setModifiedRecordingName(String name) {
        Log.d(TAG, "setRecordingNameInDialog:" + name);
        mModifiedRecordingName = name;
    }

    /**
     * When FmRadioActivity.onStop() set true, FmRadioActivity.onResume() set false;
     * @param stop
     */
    public static void setActivityIsOnStop(boolean stop) {
        sActivityIsOnStop = stop;
    }

    /**
     * Check current is in call/ringtone or not
     * @return true if is not call mode. false mean is in call or ringtone
     */
    public boolean isModeNormal() {
        int mode = mAudioManager.getMode();
        Log.d(TAG, "isInCall mode:" + mode);
        return mode == AudioManager.MODE_NORMAL;
    }

    // FM Radio EM start
    /**
     * Inquiry if fm stereo mono(true, stereo; false mono)
     * 
     * @return (true, stereo; false, mono)
     */
    public boolean getStereoMono() {
        Log.d(TAG, "FMRadioService.getStereoMono");
        return FmRadioNative.stereoMono();
    }

    /**
     * Force set to stero/mono mode
     * 
     * @param isMono
     *            (true, mono; false, stereo)
     * @return (true, success; false, failed)
     */
    public boolean setStereoMono(boolean isMono) {
        Log.d(TAG, "FMRadioService.setStereoMono: isMono=" + isMono);
        return FmRadioNative.setStereoMono(isMono);
    }
    
    /**
     * set RSSI, desense RSSI, mute gain soft
     * @param index flag which will execute
     * (0:rssi threshold,1:desense rssi threshold,2: SGM threshold)
     * @param value send to native
     * @return execute ok or not
     */
    public boolean setEmth(int index, int value) {
        Log.d(TAG, ">>> FMRadioService.setEmth: index=" + index + ",value=" + value);
        boolean isOk = FmRadioNative.emsetth(index, value);
        Log.d(TAG, "<<< FMRadioService.setEmth: isOk=" + isOk);
        return isOk;
    }
    
    /**
     * send variables to native, and get some variables return.
     * @param val send to native
     * @return get value from native
     */
    public short[] emcmd(short[] val) {
        Log.d(TAG, ">>FMRadioService.emcmd: val=" + val);
        short[] shortCmds = null;
        shortCmds = FmRadioNative.emcmd(val);
        Log.d(TAG, "<<FMRadioService.emcmd:" + shortCmds);
        return shortCmds;
    }

    /**
     * Get hardware version not need async
     */
    public int[] getHardwareVersion() {
        return FmRadioNative.getHardwareVersion();
    }

    /**
     * Read cap array method not need async
     */
    public int getCapArray() {
        Log.d(TAG, "FMRadioService.readCapArray");
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return -1;
        }
        return FmRadioNative.readCapArray();
    }

    /**
     * Get rssi not need async
     */
    public int getRssi() {
        Log.d(TAG, "FMRadioService.readRssi");
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return -1;
        }
        return FmRadioNative.readRssi();
    }

    /**
     * read rds bler not need async
     */
    public int getRdsBler() {
        Log.d(TAG, "FMRadioService.readRdsBler");
        if (!mIsPowerUp) {
            Log.w(TAG, "FM is not powered up");
            return -1;
        }
        return FmRadioNative.readRdsBler();
    }
    // FM Radio EM end

}
