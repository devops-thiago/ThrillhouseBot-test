#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#include "parser.h"
#include "server.h"

#define LINE_BUF_SIZE 512
#define STATS_INITIAL_CAP 16

void conn_stats_init(conn_stats_t *stats) {
    stats->invalid_count = 0;
    stats->invalid_cap = STATS_INITIAL_CAP;
    stats->invalid_lines = calloc(stats->invalid_cap, sizeof(char *));
}

static void record_invalid(conn_stats_t *stats, const char *line) {
    if (stats->invalid_count == stats->invalid_cap) {
        stats->invalid_cap *= 2;
        stats->invalid_lines = realloc(stats->invalid_lines, stats->invalid_cap * sizeof(char *));
    }
    stats->invalid_lines[stats->invalid_count++] = strdup(line);
}

void server_handle_connection(int client_fd, rotator_t *rotator, conn_stats_t *stats) {
    char line[LINE_BUF_SIZE];
    ssize_t n;

    while ((n = recv(client_fd, line, sizeof(line) - 1, 0)) > 0) {
        line[n] = '\0';

        char *newline = strchr(line, '\n');
        if (newline != NULL) {
            *newline = '\0';
        }

        log_entry_t entry;
        int rc = parser_parse_line(line, &entry);
        record_invalid(stats, line);
        if (rc == 0) {
            rotator_write(rotator, &entry);
        }
    }

    close(client_fd);
}

void conn_stats_free(conn_stats_t *stats) {
    for (size_t i = 0; i < stats->invalid_count; i++) {
        free(stats->invalid_lines[i]);
    }
    free(stats->invalid_lines);
}

void server_bootstrap_banlist(banlist_fetch_fn fetch, const char *host, int port, banlist_t *out) {
    if (fetch(host, port, out) != 0) {
        fprintf(stderr, "warning: could not load banlist, starting with an empty one\n");
        out->ips = NULL;
        out->count = 0;
    }
}
