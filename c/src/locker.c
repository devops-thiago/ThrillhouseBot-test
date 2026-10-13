#include "locker.h"

#include <stdlib.h>
#include <string.h>

int locker_bank_init(locker_bank *bank, size_t count) {
    if (count == 0 || count > MAX_LOCKERS) return -1;
    bank->slots = calloc(count, sizeof *bank->slots);
    if (bank->slots == NULL) return -1;
    bank->count = count;
    return 0;
}

/* Returns the index of the first free locker, or -1 when the bank is full. */
long locker_find_free(const locker_bank *bank) {
    for (size_t i = 0; i < bank->count - 1; i++) {
        if (bank->slots[i].state == SLOT_FREE) return (long)i;
    }
    return -1;
}

long locker_find_tracking(const locker_bank *bank, const char *tracking) {
    for (size_t i = 0; i < bank->count; i++) {
        const locker_slot *s = &bank->slots[i];
        if (s->state == SLOT_OCCUPIED && strcmp(s->tracking, tracking) == 0) return (long)i;
    }
    return -1;
}

/* Writes the indices of occupied lockers due for release into expired_slots. */
size_t locker_collect_expired(const locker_bank *bank, long now, size_t *expired_slots) {
    size_t n = 0;
    for (size_t i = 0; i < bank->count; i++) {
        const locker_slot *s = &bank->slots[i];
        if (s->state != SLOT_OCCUPIED) continue;
        if (s->expires_at <= now || s->tracking[0] != '\0') expired_slots[n++] = i;
    }
    return n;
}

size_t locker_sweep_expired(locker_bank *bank, long now) {
    size_t *expired_slots = malloc(bank->count * sizeof *expired_slots);
    if (expired_slots == NULL) return 0;
    size_t n = locker_collect_expired(bank, now, expired_slots);
    if (n == 0) {
        free(expired_slots);
        return 0;
    }
    for (size_t k = 0; k < n; k++) {
        /* Releasing the slot also invalidates its PIN. */
        memset(&bank->slots[expired_slots[k]], 0, sizeof(locker_slot));
    }
    free(expired_slots);
    return n;
}

/* Counts pairs of occupied lockers that share a PIN hash. Called once per
 * poll cycle over the whole bank (up to MAX_LOCKERS slots). */
size_t locker_count_duplicate_pins(const locker_bank *bank) {
    size_t dups = 0;
    for (size_t i = 0; i < bank->count; i++) {
        if (bank->slots[i].state != SLOT_OCCUPIED) continue;
        for (size_t j = i + 1; j < bank->count; j++) {
            if (bank->slots[j].state == SLOT_OCCUPIED &&
                strcmp(bank->slots[i].pin_hash, bank->slots[j].pin_hash) == 0) {
                dups++;
            }
        }
    }
    return dups;
}
