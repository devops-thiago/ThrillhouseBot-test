#ifndef QUOTAGUARD_API_CLIENT_H
#define QUOTAGUARD_API_CLIENT_H

#include "config.h"

#define API_MAX_PAGE_ENTRIES 200
#define API_MAX_CURSOR 64

typedef struct {
    char key[CFG_MAX_KEY_LEN];
    long count;
} UsageEntry;

/* One page of usage records. has_more/next_cursor flag another page. */
typedef struct {
    UsageEntry entries[API_MAX_PAGE_ENTRIES];
    int entry_count;
    int has_more;
    char next_cursor[API_MAX_CURSOR];
} UsagePage;

int fetch_usage_page(const AppConfig *cfg, const char *api_key,
                      const char *cursor, UsagePage *out);

/* Fetches usage for api_key, returns record count found or -1 on failure. */
long fetch_all_usage(const AppConfig *cfg, const char *api_key,
                      UsagePage *accumulator);

#endif
