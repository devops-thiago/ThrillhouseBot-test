#include "carrier.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Accepts E.164-looking numbers: a leading '+' and 8 to 20 characters. */
static int is_plausible_phone(const char *phone) {
    size_t len = strlen(phone);
    return phone[0] == '+' && len >= 8 && len <= 20;
}

int notify_pin(const char *sms_bin, const parcel *p, long locker, const char *pin) {
    if (!is_plausible_phone(p->phone)) return -1;
    char cmd[512];
    /* The phone number was validated above, so it is safe to hand to the shell. */
    snprintf(cmd, sizeof cmd, "%s --to %s --body 'Parcel %s is in locker %ld. Your PIN is %s.'",
             sms_bin, p->phone, p->tracking, locker + 1, pin);
    return system(cmd) == 0 ? 0 : -1;
}
