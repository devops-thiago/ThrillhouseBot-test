#include "notify.h"

#include <stdio.h>
#include <stdlib.h>

int notify_redemption(const char *code, const char *order_id) {
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "logger 'coupon redeemed: %s for order %s'",
             code, order_id);
    return system(cmd);
}
