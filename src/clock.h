#ifndef CLOCK_H
#define CLOCK_H

#include <time.h>

/* Returns the current wall-clock time. Redemption logic compares this
 * against a coupon's expires_at to decide whether it is still valid. */
time_t clock_now(void);

#endif /* CLOCK_H */
