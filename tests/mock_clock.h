#ifndef MOCK_CLOCK_H
#define MOCK_CLOCK_H

#include <time.h>

/* Configures the time that the test double's clock_now() should report,
 * so expiry checks can be exercised deterministically. */
void mock_clock_set(time_t t);

#endif /* MOCK_CLOCK_H */
