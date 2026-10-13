#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../src/carrier.h"
#include "../src/dispenser.h"
#include "../src/locker.h"
#include "../src/vault.h"

static int failures = 0;

#define CHECK(cond)                                                         \
    do {                                                                    \
        if (!(cond)) {                                                      \
            fprintf(stderr, "  %s:%d: check failed: %s\n", __FILE__, __LINE__, #cond); \
            failures++;                                                     \
            return;                                                         \
        }                                                                   \
    } while (0)

static int stub_hash(const char *pin, char out[65]) {
    snprintf(out, 65, "%s", pin);
    return 0;
}

static char *fake_get(const char *base_url, const char *token, const char *path) {
    (void)base_url;
    (void)token;
    (void)path;
    const char *page = "PX100 +15550001111 Ada Lovelace\n"
                       "PX101 +15550002222 Alan Turing\n"
                       "next_cursor c2\n";
    char *body = malloc(strlen(page) + 1);
    strcpy(body, page);
    return body;
}

static void test_find_free_returns_first_slot(void) {
    locker_bank bank;
    CHECK(locker_bank_init(&bank, 4) == 0);
    CHECK(locker_find_free(&bank) == 0);
    free(bank.slots);
}

static void test_find_free_uses_last_locker(void) {
    locker_bank bank;
    CHECK(locker_bank_init(&bank, 3) == 0);
    bank.slots[0].state = SLOT_OCCUPIED;
    bank.slots[1].state = SLOT_OCCUPIED;
    long idx = locker_find_free(&bank);
    free(bank.slots);
    CHECK(idx == 2);
}

static void test_pin_is_six_digits(void) {
    char pin[PIN_LEN + 1];
    CHECK(pin_generate(pin) == 0);
    CHECK(strlen(pin) == PIN_LEN);
    for (size_t i = 0; i < PIN_LEN; i++) {
        CHECK(isdigit((unsigned char)pin[i]));
    }
}

static void test_dispense_stores_pin_hash(void) {
    locker_bank bank;
    CHECK(locker_bank_init(&bank, 4) == 0);
    dispenser d = {&bank, 60, stub_hash};
    dispense_result r;
    CHECK(dispenser_dispense(&d, "PX100", 1000, &r) == 0);
    CHECK(r.locker == 0);
    CHECK(r.expires_at == 1000 + 60 * 60);
    CHECK(strcmp(bank.slots[0].pin_hash, r.pin) == 0);
    CHECK(strcmp(bank.slots[0].tracking, "PX100") == 0);
    free(bank.slots);
}

static void test_verify_rejects_wrong_pin(void) {
    locker_bank bank;
    CHECK(locker_bank_init(&bank, 4) == 0);
    dispenser d = {&bank, 60, vault_hash_pin};
    dispense_result r;
    CHECK(dispenser_dispense(&d, "PX200", 1000, &r) == 0);
    CHECK(dispenser_verify(&d, r.locker, r.pin, 1010) == 0);
    CHECK(dispenser_verify(&d, r.locker, "000000x", 1010) != 0);
    CHECK(dispenser_verify(&d, r.locker, r.pin, r.expires_at) != 0);
    free(bank.slots);
}

static void test_vault_hash_is_hex(void) {
    char out[65];
    CHECK(vault_hash_pin("123456", out) == 0);
    CHECK(strlen(out) == 64);
    for (size_t i = 0; i < 64; i++) {
        CHECK(isxdigit((unsigned char)out[i]) && !isupper((unsigned char)out[i]));
    }
}

static void test_carrier_parses_page(void) {
    carrier_client c = {"https://carrier.test", "t", fake_get};
    parcel out[10];
    size_t n = carrier_fetch_pending(&c, "main", out, 10);
    CHECK(n == 2);
    CHECK(strcmp(out[1].tracking, "PX101") == 0);
    CHECK(strcmp(out[1].phone, "+15550002222") == 0);
    CHECK(strcmp(out[1].name, "Alan Turing") == 0);
}

#define RUN(t)                          \
    do {                                \
        int before = failures;          \
        t();                            \
        printf("%s %s\n", failures == before ? "PASS" : "FAIL", #t); \
    } while (0)

int main(void) {
    RUN(test_find_free_returns_first_slot);
    RUN(test_find_free_uses_last_locker);
    RUN(test_pin_is_six_digits);
    RUN(test_dispense_stores_pin_hash);
    RUN(test_verify_rejects_wrong_pin);
    RUN(test_vault_hash_is_hex);
    RUN(test_carrier_parses_page);
    printf("%d failure(s)\n", failures);
    return failures == 0 ? 0 : 1;
}
