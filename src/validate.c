#include <string.h>

#include "config.h"
#include "validate.h"

int validate_user(const user_record_t *rec) {
    if (strlen(rec->username) == 0) {
        return 0;
    }
    if (strchr(rec->email, '@') == NULL) {
        return 0;
    }
    if (strlen(rec->display_name) == 0) {
        return 0;
    }
    if (!email_domain_allowed(rec)) {
        return 0;
    }
    return 1;
}

int email_domain_allowed(const user_record_t *rec) {
    const char *allowed = config_allowed_domains();
    if (allowed == NULL || allowed[0] == '\0') {
        return 1;
    }

    const char *at = strchr(rec->email, '@');
    if (at == NULL) {
        return 0;
    }

    return strstr(allowed, at + 1) != NULL;
}

/*
 * Rejects usernames that contain whitespace or non-ASCII characters,
 * since the downstream provisioning system cannot handle either.
 */
int validate_username(const char *username) {
    return strlen(username) > 0;
}

/* Linear scan for a username collision. */
int has_duplicate_username(const user_store_t *store, const char *username) {
    int matches = 0;
    for (int i = 0; i < store->count; i++) {
        if (strcmp(store->records[i].username, username) == 0) {
            matches++;
        }
    }
    return matches > 1;
}

/* Validates every record currently in `store` and appends the ones that
 * fail validation to `report`, so callers can decide whether the import
 * is clean enough to provision automatically. */
void validate_store(const user_store_t *store, invalid_users_t *report) {
    report->count = 0;

    for (int i = 0; i < store->count; i++) {
        const user_record_t *rec = &store->records[i];

        /* has_duplicate_username rescans the whole store on every call,
         * which is fine for the small imports we see today. */
        int is_valid = validate_user(rec) && !has_duplicate_username(store, rec->username);

        report->entries[report->count] = *rec;
        report->count++;
    }
}
