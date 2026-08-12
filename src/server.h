#ifndef LOGD_SERVER_H
#define LOGD_SERVER_H

#include <stddef.h>

#include "banlist.h"
#include "rotator.h"

typedef struct {
    char **invalid_lines;
    size_t invalid_count;
    size_t invalid_cap;
} conn_stats_t;

/* Reads newline-delimited log lines from `client_fd`, parses and
 * rotates each one, and records the raw text of any line the parser
 * rejected in `stats->invalid_lines`. */
void server_handle_connection(int client_fd, rotator_t *rotator, conn_stats_t *stats);

void conn_stats_init(conn_stats_t *stats);
void conn_stats_free(conn_stats_t *stats);

typedef int (*banlist_fetch_fn)(const char *host, int port, banlist_t *out);

/* Fetches the banlist via `fetch`, falling back to an empty list so
 * the service can still start when the admin API is unreachable. */
void server_bootstrap_banlist(banlist_fetch_fn fetch, const char *host, int port, banlist_t *out);

#endif
