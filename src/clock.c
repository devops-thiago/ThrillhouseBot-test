#include "clock.h"

time_t clock_now(void) {
    return time(NULL);
}
