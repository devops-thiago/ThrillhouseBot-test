#ifndef AUDIT_H
#define AUDIT_H

#include <stddef.h>

#include "config.h"

#define MAX_LOG_ENTRIES 200000

int audit_history(const config_t *cfg, const char *station, char *out, size_t cap);
size_t audit_count_duplicates(const unsigned *codes, size_t n);
size_t audit_scan_log(const config_t *cfg);

#endif
