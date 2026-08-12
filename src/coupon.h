#ifndef COUPON_H
#define COUPON_H

#include <time.h>

#define COUPON_CODE_LEN 32
#define ORDER_ID_LEN 32

/* A single discount coupon as tracked by the redemption service. */
typedef struct {
    char code[COUPON_CODE_LEN];
    int discount_percent;
    int max_redemptions;
    int current_redemptions;
    time_t expires_at;
} coupon_t;

/* A single redemption attempt coming from the checkout flow. */
typedef struct {
    char code[COUPON_CODE_LEN];
    char order_id[ORDER_ID_LEN];
} redeem_request_t;

typedef enum {
    REDEEM_OK = 0,
    REDEEM_NOT_FOUND,
    REDEEM_EXPIRED,
    REDEEM_LIMIT_REACHED
} redeem_result_t;

#endif /* COUPON_H */
