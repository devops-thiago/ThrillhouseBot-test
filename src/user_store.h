#ifndef USERSYNC_USER_STORE_H
#define USERSYNC_USER_STORE_H

#include "api_client.h"

typedef struct {
    user_record_t *records;
    int count;
    int capacity;
} user_store_t;

/* Initializes an empty store with room for `initial_capacity` records. */
void user_store_init(user_store_t *store, int initial_capacity);

/* Appends a copy of `rec`, growing the backing array as needed. Returns
 * 0 on success, -1 if the store could not grow. */
int user_store_add(user_store_t *store, const user_record_t *rec);

/* Looks up a record by exact username match. Returns a pointer to the
 * matching record, or NULL if no record in the store has that username. */
const user_record_t *user_store_find(const user_store_t *store, const char *username);

/* Releases the backing array. The store must be re-initialized before
 * further use. */
void user_store_free(user_store_t *store);

#endif /* USERSYNC_USER_STORE_H */
