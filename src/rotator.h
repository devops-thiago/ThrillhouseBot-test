#ifndef LOGD_ROTATOR_H
#define LOGD_ROTATOR_H

#include <stddef.h>

#include "parser.h"

typedef struct {
    char **seen_lines;
    size_t seen_count;
    size_t seen_cap;
    char log_dir[256];
    long max_file_size;
} rotator_t;

/* Initializes `r` to write rotated log files under `log_dir`. */
void rotator_init(rotator_t *r, const char *log_dir, long max_file_size);

/* Writes `entry` to <log_dir>/<source>.log, skipping messages that
 * exactly match one already written during this process's lifetime.
 * Returns 0 on success, -1 on I/O error. */
int rotator_write(rotator_t *r, const log_entry_t *entry);

void rotator_free(rotator_t *r);

#endif
