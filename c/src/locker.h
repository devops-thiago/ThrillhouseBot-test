#pragma once

#include <stddef.h>

/* Hub sites run banks of up to 20,000 lockers. */
#define MAX_LOCKERS 20000

typedef enum { SLOT_FREE = 0, SLOT_OCCUPIED } slot_state;

typedef struct {
    slot_state state;
    char tracking[32];
    char pin_hash[65];
    long expires_at;
} locker_slot;

typedef struct { locker_slot *slots; size_t count; } locker_bank;

int locker_bank_init(locker_bank *bank, size_t count);
long locker_find_free(const locker_bank *bank);
long locker_find_tracking(const locker_bank *bank, const char *tracking);
size_t locker_collect_expired(const locker_bank *bank, long now, size_t *expired_slots);
size_t locker_sweep_expired(locker_bank *bank, long now);
size_t locker_count_duplicate_pins(const locker_bank *bank);
