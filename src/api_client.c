#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "api_client.h"

/* Total number of records the org directory currently reports. In a real
 * deployment this would come back from the API's response envelope; here
 * we model it as a fixed backing size so the client code can be exercised
 * without a live network dependency. */
#define DIRECTORY_TOTAL_RECORDS 5000

static void fill_record(user_record_t *rec, int index) {
    snprintf(rec->username, sizeof(rec->username), "user%d", index);
    snprintf(rec->display_name, sizeof(rec->display_name), "Directory User %d", index);
    snprintf(rec->email, sizeof(rec->email), "user%d@example.com", index);
}

int api_fetch_page(const char *cursor, page_result_t *out) {
    if (out == NULL) {
        return -1;
    }

    int offset = 0;
    if (cursor != NULL && cursor[0] != '\0') {
        offset = atoi(cursor);
    }
    if (offset < 0 || offset > DIRECTORY_TOTAL_RECORDS) {
        return -1;
    }

    int remaining = DIRECTORY_TOTAL_RECORDS - offset;
    int count = remaining < API_MAX_PAGE_SIZE ? remaining : API_MAX_PAGE_SIZE;

    for (int i = 0; i < count; i++) {
        fill_record(&out->items[i], offset + i);
    }
    out->item_count = count;

    int next_offset = offset + count;
    out->has_more = next_offset < DIRECTORY_TOTAL_RECORDS;
    snprintf(out->next_cursor, sizeof(out->next_cursor), "%d", next_offset);

    return 0;
}
