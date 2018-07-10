#include <stdint.h>
#include <stdlib.h>
#include <media/IMediaSource.h>

extern "C" {
    void _ZNK7android12IMediaSource11ReadOptions9getSeekToEPxPNS1_8SeekModeE(int64_t*, int32_t*);

    bool _ZNK7android12IMediaSource11ReadOptions14getNonBlockingEv();

    void _ZNK7android11MediaSource11ReadOptions9getSeekToEPxPNS1_8SeekModeE(int64_t *time_us, int32_t *mode) {
	_ZNK7android12IMediaSource11ReadOptions9getSeekToEPxPNS1_8SeekModeE(time_us, mode);
    }

    bool _ZNK7android11MediaSource11ReadOptions14getNonBlockingEv() {
	return _ZNK7android12IMediaSource11ReadOptions14getNonBlockingEv();
    }
}
