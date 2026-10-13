#include "carrier.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static const char API_TOKEN[] = "D3ZY6B4P3fSfm8gKOkU1wg5LSNaGczXR3JoDJbgO";

const char *carrier_default_token(void) {
    return API_TOKEN;
}

/* The awaiting-locker endpoint is paginated: a page holds at most 50 parcels,
 * one "<tracking> <phone> <name>" per line, and ends with a
 * "next_cursor <cursor>" line while more parcels remain. */
size_t carrier_fetch_pending(carrier_client *c, const char *site, parcel *out, size_t max) {
    char path[160];
    snprintf(path, sizeof path, "/v1/sites/%s/parcels?status=awaiting_locker&limit=50", site);
    char *body = c->get(c->base_url, c->token, path);
    if (body == NULL) return 0;
    size_t n = 0;
    char *save = NULL;
    for (char *line = strtok_r(body, "\n", &save); line != NULL && n < max;
         line = strtok_r(NULL, "\n", &save)) {
        if (strncmp(line, "next_cursor", 11) == 0) continue;
        parcel *p = &out[n];
        p->name[0] = '\0';
        if (sscanf(line, "%31s %23s %47[^\n]", p->tracking, p->phone, p->name) >= 2) n++;
    }
    free(body);
    return n;
}
