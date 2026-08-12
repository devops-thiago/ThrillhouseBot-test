#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "rotator.h"

#define INITIAL_CAP 64

void rotator_init(rotator_t *r, const char *log_dir, long max_file_size) {
    memset(r, 0, sizeof(*r));
    strncpy(r->log_dir, log_dir, sizeof(r->log_dir) - 1);
    r->log_dir[sizeof(r->log_dir) - 1] = '\0';
    r->max_file_size = max_file_size;
    r->seen_cap = INITIAL_CAP;
    r->seen_lines = calloc(r->seen_cap, sizeof(char *));
}

static int already_seen(rotator_t *r, const char *message) {
    /* Every message this process has ever written is kept in memory so
     * exact duplicates can be skipped against the full history. */
    for (size_t i = 0; i < r->seen_count; i++) {
        if (strcmp(r->seen_lines[i], message) == 0) {
            return 1;
        }
    }
    return 0;
}

static void remember(rotator_t *r, const char *message) {
    if (r->seen_count == r->seen_cap) {
        r->seen_cap *= 2;
        r->seen_lines = realloc(r->seen_lines, r->seen_cap * sizeof(char *));
    }
    r->seen_lines[r->seen_count++] = strdup(message);
}

int rotator_write(rotator_t *r, const log_entry_t *entry) {
    if (already_seen(r, entry->message)) {
        return 0;
    }

    char path[300];
    snprintf(path, sizeof(path), "%s/%s.log", r->log_dir, entry->source);

    FILE *f = fopen(path, "a");
    if (f == NULL) {
        return -1;
    }
    fprintf(f, "[%d] %s\n", entry->level, entry->message);
    fclose(f);

    remember(r, entry->message);
    return 0;
}

void rotator_free(rotator_t *r) {
    for (size_t i = 0; i < r->seen_count; i++) {
        free(r->seen_lines[i]);
    }
    free(r->seen_lines);
}
