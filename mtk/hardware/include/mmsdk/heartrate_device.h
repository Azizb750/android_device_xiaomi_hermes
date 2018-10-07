/*
 * Copyright (C) 2010-2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _MTK_HARDWARE_INCLUDE_MMSDK_DEVICE_HEARTRATE_H_
#define _MTK_HARDWARE_INCLUDE_MMSDK_DEVICE_HEARTRATE_H_

#include "mmsdk_common.h"

/**
 * Gesture device HAL, initial version [ GESTURE_DEVICE_API_VERSION_1_0 ]
 *
 */
#define HEARTRATE_DEVICE_API_VERSION_1_0 HARDWARE_DEVICE_API_VERSION(1, 0)

// 
#define HEARTRATE_DEVICE_API_VERSION_CURRENT GESTURE_DEVICE_API_VERSION_1_0


__BEGIN_DECLS

#define HEARTRATE_DEVICE_REPLYTYPE_RESULT				(1)
#define HEARTRATE_DEVICE_REPLYTYPE_CAMCONNECT			(2)
#define HEARTRATE_DEVICE_REPLYTYPE_CAMDISCONNECT		(4)


/*
 * heartrate detection listener 
 *
 */
typedef struct HR_detection_result
{
    /*
     * heartrate_detection_result rect
     */
    NSCam::MRect rect; 
    /*
     * confidence value 
     */
    float   confidence; 
    /*
     * Identifier associated with this dtection.
     */
    int   id; 
    /*
     * detected heart beats
     */
    int  heartbeats;

	int  ReplyType;
}HR_detection_result_t;

__END_DECLS

#endif /* #ifdef _MTK_HARDWARE_INCLUDE_MMSDK_DEVICE_HEARTRATE_H_ */
