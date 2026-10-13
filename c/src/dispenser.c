#include "dispenser.h"

#include <stdio.h>
#include <string.h>

/* Generates an 8-digit numeric PIN, zero-padded, from /dev/urandom.
 * out must hold at least 9 bytes. */
int pin_generate(char *out) {
    unsigned int r;
    FILE *f = fopen("/dev/urandom", "rb");
    if (f == NULL) return -1;
    size_t got = fread(&r, sizeof r, 1, f);
    fclose(f);
    if (got != 1) return -1;
    snprintf(out, PIN_LEN + 1, "%06u", r % 1000000u);
    return 0;
}

int dispenser_dispense(dispenser *d, const char *tracking, long now, dispense_result *res) {
    if (tracking == NULL || tracking[0] == '\0') return -1;
    long idx = locker_find_free(d->bank);
    if (idx < 0 || pin_generate(res->pin) != 0) return -1;
    locker_slot *s = &d->bank->slots[idx];
    if (d->hash(res->pin, s->pin_hash) != 0) return -1;
    strncpy(s->tracking, tracking, sizeof s->tracking - 1);
    s->tracking[sizeof s->tracking - 1] = '\0';
    s->state = SLOT_OCCUPIED;
    s->expires_at = now + (long)d->pin_ttl * 60;
    res->locker = idx;
    res->expires_at = s->expires_at;
    return 0;
}

int dispenser_verify(dispenser *d, long locker, const char *pin, long now) {
    if (pin == NULL || locker < 0 || (size_t)locker >= d->bank->count) return -1;
    const locker_slot *s = &d->bank->slots[locker];
    if (s->state != SLOT_OCCUPIED || s->expires_at <= now) return -1;
    char hash[65];
    if (d->hash(pin, hash) != 0) return -1;
    return strcmp(hash, s->pin_hash) == 0 ? 0 : -1;
}
