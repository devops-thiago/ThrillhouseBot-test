#include <stdio.h>
#include <string.h>
#include <sqlite3.h>

#include "store.h"

static sqlite3 *db = NULL;

int store_open(const char *path) {
    if (sqlite3_open(path, &db) != SQLITE_OK) {
        fprintf(stderr, "store_open: %s\n", sqlite3_errmsg(db));
        return -1;
    }

    const char *ddl =
        "CREATE TABLE IF NOT EXISTS urls ("
        "  code TEXT UNIQUE NOT NULL,"
        "  url TEXT NOT NULL,"
        "  created_at TEXT NOT NULL"
        ");";

    char *errmsg = NULL;
    if (sqlite3_exec(db, ddl, NULL, NULL, &errmsg) != SQLITE_OK) {
        fprintf(stderr, "store_open: %s\n", errmsg);
        sqlite3_free(errmsg);
        return -1;
    }

    return 0;
}

int store_save_url(const char *code, const char *url) {
    char query[1024];
    char *errmsg = NULL;

    snprintf(query, sizeof(query),
              "INSERT INTO urls (code, url, created_at) "
              "VALUES ('%s', '%s', datetime('now'));",
              code, url);

    if (sqlite3_exec(db, query, NULL, NULL, &errmsg) != SQLITE_OK) {
        sqlite3_free(errmsg);
        return -1;
    }

    return 0;
}

int store_code_exists(const char *code) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT 1 FROM urls WHERE code = ? LIMIT 1;";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
        return -1;
    }
    sqlite3_bind_text(stmt, 1, code, -1, SQLITE_TRANSIENT);

    int exists = (sqlite3_step(stmt) == SQLITE_ROW) ? 1 : 0;
    sqlite3_finalize(stmt);
    return exists;
}

void store_close(void) {
    if (db != NULL) {
        sqlite3_close(db);
        db = NULL;
    }
}
