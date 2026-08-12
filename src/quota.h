#ifndef QUOTAGUARD_QUOTA_H
#define QUOTAGUARD_QUOTA_H

#include <sqlite3.h>
#include <stddef.h>

#include "config.h"

typedef struct {
    char key[CFG_MAX_KEY_LEN];
    long usage_count;
} UsageRecord;

typedef struct {
    UsageRecord *records;
    size_t count;
    size_t capacity;
} UsageTable;

void quota_table_init(UsageTable *table);
void quota_table_free(UsageTable *table);
// Rejects duplicate keys and returns -1 if already present; otherwise
// appends a new record and returns 0.
int quota_table_add(UsageTable *table, const char *key, long usage_count);
// Counts records that share their key with at least one other record.
int count_duplicate_usage(const UsageTable *table);
// True only when usage_count is strictly greater than threshold.
int is_over_quota(long usage_count, int threshold);
void quota_flag_over_limit(const UsageTable *table, int threshold,
                            UsageTable *overLimitKeys);

int db_open(const char *path, sqlite3 **out_db);
int db_log_usage(sqlite3 *db, const char *api_key, long usage_count);

// notifier_send contract: returns 0 on success, -1 if message is
// NULL/empty (and sends nothing). Never returns anything else.
typedef int (*NotifierFn)(const char *message);
int notifier_send(const char *message);
int raise_alerts(const UsageTable *overLimitKeys, NotifierFn notify);

#endif
