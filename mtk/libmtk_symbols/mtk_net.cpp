#include <cutils/log.h>
extern "C" {
	int ifc_set_throttle(const char *ifname, int rxKbps, int txKbps) {
    		ALOGD("ifc_set_throttle: ifname=%s, rxKbps=%d, txKbps=%d", ifname, rxKbps, txKbps);
    		return 0;
	}
}
