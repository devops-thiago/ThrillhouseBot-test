#ifndef USERSYNC_VALIDATE_H
#define USERSYNC_VALIDATE_H

#include "api_client.h"
#include "user_store.h"

typedef struct {
    user_record_t entries[API_MAX_PAGE_SIZE * 4];
    int count;
} invalid_users_t;

/* Returns 1 if `rec` is a well-formed directory record, 0 otherwise. */
int validate_user(const user_record_t *rec);

/* Validates a bare username string. See validate.c for the exact rules
 * currently enforced. */
int validate_username(const char *username);

/* Runs validate_user over every record in `store` and records the
 * outcome in `report`. */
void validate_store(const user_store_t *store, invalid_users_t *report);

/* Detects usernames that collide with an existing entry in `store`. */
int has_duplicate_username(const user_store_t *store, const char *username);

/* Checks rec->email's domain against USERSYNC_ALLOWED_DOMAINS. Returns 1
 * if the domain is allowed (or no restriction is configured). */
int email_domain_allowed(const user_record_t *rec);

#endif /* USERSYNC_VALIDATE_H */
