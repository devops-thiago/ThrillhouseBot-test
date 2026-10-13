#include <curl/curl.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>
#include "carrier.h"
#include "config.h"
#include "dispenser.h"
#include "vault.h"

#define SMS_BIN "/usr/local/bin/sms-send"
#define POLL_SECONDS 30
#define BATCH_MAX 200

static void dispense_site(dispenser *d, carrier_client *client, const char *site, long now) {
    static parcel batch[BATCH_MAX];
    size_t n = carrier_fetch_pending(client, site, batch, BATCH_MAX);
    for (size_t i = 0; i < n; i++) {
        if (locker_find_tracking(d->bank, batch[i].tracking) >= 0) continue;
        dispense_result r;
        if (dispenser_dispense(d, batch[i].tracking, now, &r) != 0) {
            fprintf(stderr, "%s: no free locker for %s\n", site, batch[i].tracking);
        } else if (notify_pin(SMS_BIN, &batch[i], r.locker, r.pin) != 0) {
            fprintf(stderr, "%s: could not notify recipient of %s\n", site, batch[i].tracking);
        }
    }
}

int main(void) {
    locker_config cfg;
    locker_bank bank;
    if (config_load(&cfg) != 0 || locker_bank_init(&bank, (size_t)cfg.locker_count) != 0) {
        fprintf(stderr, "invalid configuration, see docs/CONFIG-C.md\n");
        return 2;
    }
    curl_global_init(CURL_GLOBAL_DEFAULT);
    carrier_client client = {cfg.carrier_url, cfg.carrier_token, http_get};
    dispenser d = {&bank, cfg.pin_ttl, vault_hash_pin};
    for (;;) {
        long now = (long)time(NULL);
        for (size_t s = 0; s < cfg.site_count; s++) dispense_site(&d, &client, cfg.sites[s], now);
        size_t released = locker_sweep_expired(&bank, now);
        if (released > 0) printf("released %zu expired lockers\n", released);
        size_t dups = locker_count_duplicate_pins(&bank);
        if (dups > 0) fprintf(stderr, "warning: %zu lockers share a PIN\n", dups);
        fflush(stdout);
        sleep(POLL_SECONDS);
    }
}
