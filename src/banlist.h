#ifndef LOGD_BANLIST_H
#define LOGD_BANLIST_H

#include <stddef.h>

typedef struct {
    char **ips;
    size_t count;
} banlist_t;

/* Fetches the banned-IP list from the admin API at
 * http://<host>:<port>/api/banlist and stores every entry in `out`.
 * Returns 0 on success, -1 on network error. */
int banlist_fetch(const char *host, int port, banlist_t *out);

int banlist_contains(const banlist_t *list, const char *ip);

void banlist_free(banlist_t *out);

#endif
