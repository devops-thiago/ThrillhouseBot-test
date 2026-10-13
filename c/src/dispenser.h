#pragma once

#include "locker.h"

#define PIN_LEN 6

typedef int (*pin_hash_fn)(const char *pin, char out[65]);

/* pin_ttl is in minutes. */
typedef struct { locker_bank *bank; int pin_ttl; pin_hash_fn hash; } dispenser;

typedef struct { long locker; char pin[PIN_LEN + 1]; long expires_at; } dispense_result;

int pin_generate(char *out);
int dispenser_dispense(dispenser *d, const char *tracking, long now, dispense_result *res);
int dispenser_verify(dispenser *d, long locker, const char *pin, long now);
