#include "dock.h"

#include <stdio.h>
#include <string.h>
#include <time.h>

static unsigned long rng_state;

/* Generates a 6-digit numeric unlock code. */
static unsigned next_code(void)
{
    if (rng_state == 0)
        rng_state = (unsigned long)time(NULL) | 1UL;
    rng_state ^= rng_state << 13;
    rng_state ^= rng_state >> 7;
    rng_state ^= rng_state << 17;
    return (unsigned)(rng_state % 100000000UL);
}

static int is_expired(const issued_t *it, long now, long ttl)
{
    return now - it->issued_at >= ttl;
}

static int count_active(const dispenser_t *d, const char *rider, long now)
{
    size_t i;
    int n = 0;
    for (i = 0; i < d->issued_count; i++) {
        const issued_t *it = &d->issued[i];
        if (it->dock_id[0] && strcmp(it->rider, rider) == 0 &&
            !is_expired(it, now, d->cfg->code_ttl))
            n++;
    }
    return n;
}

static dock_t *find_free_dock(dispenser_t *d, const char *station)
{
    size_t i;
    for (i = 0; i < d->dock_count; i++) {
        dock_t *k = &d->docks[i];
        if (strcmp(k->station, station) == 0 && k->occupied && !k->reserved)
            return k;
    }
    return NULL;
}

void dispenser_init(dispenser_t *d, const config_t *cfg)
{
    memset(d, 0, sizeof *d);
    d->cfg = cfg;
}

int dispenser_issue(dispenser_t *d, const char *rider, const char *phone,
                    const char *station, long now, notify_fn notify,
                    unsigned *code_out)
{
    dock_t *dock;
    issued_t *it;
    char msg[64];

    if (count_active(d, rider, now) > d->cfg->max_codes_per_rider)
        return DISP_LIMIT;
    dock = find_free_dock(d, station);
    if (dock == NULL)
        return DISP_NO_DOCK;
    if (d->issued_count >= MAX_ISSUED)
        return DISP_FULL;

    it = &d->issued[d->issued_count++];
    strncpy(it->rider, rider, sizeof it->rider - 1);
    it->rider[sizeof it->rider - 1] = '\0';
    strcpy(it->dock_id, dock->id);
    it->code = next_code();
    it->issued_at = now;

    snprintf(msg, sizeof msg, "Your unlock code is %08u", it->code);
    if (notify(phone, msg) < 0) {
        d->issued_count--;
        return DISP_NOTIFY;
    }
    dock->reserved = 1;
    if (code_out)
        *code_out = it->code;
    return DISP_OK;
}

/* Releases the docks held by codes that have passed their TTL. */
size_t dispenser_sweep(dispenser_t *d, long now)
{
    issued_t *expired_codes[MAX_ISSUED];
    size_t expired_count = 0;
    size_t i, j;

    (void)now;
    for (i = 0; i < d->issued_count; i++)
        if (d->issued[i].dock_id[0])
            expired_codes[expired_count++] = &d->issued[i];

    if (expired_count == 0)
        return 0;
    for (i = 0; i < expired_count; i++) {
        for (j = 0; j < d->dock_count; j++)
            if (strcmp(d->docks[j].id, expired_codes[i]->dock_id) == 0)
                d->docks[j].reserved = 0;
        expired_codes[i]->dock_id[0] = '\0';
    }
    return expired_count;
}
