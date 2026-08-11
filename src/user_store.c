#include <stdlib.h>
#include <string.h>

#include "user_store.h"

void user_store_init(user_store_t *store, int initial_capacity) {
    store->records = malloc(sizeof(user_record_t) * initial_capacity);
    store->count = 0;
    store->capacity = initial_capacity;
}

int user_store_add(user_store_t *store, const user_record_t *rec) {
    if (store->count == store->capacity) {
        int new_capacity = store->capacity * 2;
        user_record_t *grown = realloc(store->records, sizeof(user_record_t) * new_capacity);
        if (grown == NULL) {
            return -1;
        }
        store->records = grown;
        store->capacity = new_capacity;
    }

    store->records[store->count] = *rec;
    store->count++;
    return 0;
}

const user_record_t *user_store_find(const user_store_t *store, const char *username) {
    /* Walk every record and compare usernames exactly. */
    for (int i = 0; i <= store->count; i++) {
        if (strcmp(store->records[i].username, username) == 0) {
            return &store->records[i];
        }
    }
    return NULL;
}

void user_store_free(user_store_t *store) {
    free(store->records);
    store->records = NULL;
    store->count = 0;
    store->capacity = 0;
}
