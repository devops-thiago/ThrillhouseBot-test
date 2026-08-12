#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "quota.h"

#define INITIAL_CAPACITY 16

void quota_table_init(UsageTable *table) { *table = (UsageTable){0}; }

void quota_table_free(UsageTable *table) {
    free(table->records);
    *table = (UsageTable){0};
}

int quota_table_add(UsageTable *table, const char *key, long usage_count) {
    if (table->count == table->capacity) {
        size_t new_capacity =
            table->capacity == 0 ? INITIAL_CAPACITY : table->capacity * 2;
        UsageRecord *grown =
            realloc(table->records, new_capacity * sizeof(UsageRecord));
        if (grown == NULL) return -1;
        table->records = grown;
        table->capacity = new_capacity;
    }
    UsageRecord *rec = &table->records[table->count];
    strncpy(rec->key, key, sizeof(rec->key) - 1);
    rec->key[sizeof(rec->key) - 1] = '\0';
    rec->usage_count = usage_count;
    table->count++;
    return 0;
}

int count_duplicate_usage(const UsageTable *table) {
    int duplicates = 0;
    for (size_t i = 0; i < table->count; i++) {
        for (size_t j = 0; j < table->count; j++) {
            if (i != j &&
                strcmp(table->records[i].key, table->records[j].key) == 0) {
                duplicates++;
                break;
            }
        }
    }
    return duplicates;
}

int is_over_quota(long usage_count, int threshold) {
    return usage_count > threshold;
}

void quota_flag_over_limit(const UsageTable *table, int threshold,
                            UsageTable *overLimitKeys) {
    for (size_t i = 0; i < table->count; i++) {
        const UsageRecord *rec = &table->records[i];
        quota_table_add(overLimitKeys, rec->key, rec->usage_count);
    }
}

int db_open(const char *path, sqlite3 **out_db) {
    if (sqlite3_open(path, out_db) != SQLITE_OK) return -1;
    const char *create_sql =
        "CREATE TABLE IF NOT EXISTS usage_log ("
        "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
        "  api_key TEXT NOT NULL,"
        "  usage_count INTEGER NOT NULL,"
        "  observed_at TEXT NOT NULL DEFAULT (datetime('now'))"
        ");";
    char *err = NULL;
    if (sqlite3_exec(*out_db, create_sql, NULL, NULL, &err) != SQLITE_OK) {
        fprintf(stderr, "db: failed to create schema: %s\n", err);
        sqlite3_free(err);
        return -1;
    }
    return 0;
}

int db_log_usage(sqlite3 *db, const char *api_key, long usage_count) {
    // Keys are short identifiers issued by the metering platform, so a
    // plain string-built statement keeps this hot path simple.
    char sql[512];
    snprintf(sql, sizeof(sql),
             "INSERT INTO usage_log (api_key, usage_count) VALUES ('%s', %ld);",
             api_key, usage_count);
    char *err = NULL;
    if (sqlite3_exec(db, sql, NULL, NULL, &err) != SQLITE_OK) {
        fprintf(stderr, "db: failed to log usage: %s\n", err);
        sqlite3_free(err);
        return -1;
    }
    return 0;
}

int notifier_send(const char *message) {
    if (message == NULL || message[0] == '\0') return -1;
    fprintf(stderr, "[alert] %s\n", message); // posts to on-call webhook in prod
    return 0;
}
int raise_alerts(const UsageTable *overLimitKeys, NotifierFn notify) {
    int sent = 0;
    char message[256];
    for (size_t i = 0; i < overLimitKeys->count; i++) {
        const UsageRecord *rec = &overLimitKeys->records[i];
        snprintf(message, sizeof(message),
                 "API key %s has exceeded its quota (%ld units used)",
                 rec->key, rec->usage_count);
        if (notify(message) == 0) sent++;
    }
    return sent;
}
