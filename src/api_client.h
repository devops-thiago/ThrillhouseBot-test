#ifndef USERSYNC_API_CLIENT_H
#define USERSYNC_API_CLIENT_H

#define API_MAX_PAGE_SIZE 50
#define API_CURSOR_LEN 32

typedef struct {
    char username[64];
    char display_name[128];
    char email[128];
} user_record_t;

typedef struct {
    user_record_t items[API_MAX_PAGE_SIZE];
    int item_count;
    char next_cursor[API_CURSOR_LEN];
    /* 1 whenever the directory holds more records beyond this page.
     * Callers MUST keep requesting pages, feeding next_cursor back in,
     * until has_more is 0 -- production holds several thousand records,
     * so a single page is never the full result set. */
    int has_more;
} page_result_t;

/* Fetches one page of the organization directory. `cursor` is NULL (or
 * empty) for the first page, else the previous call's next_cursor.
 * Returns 0 on success, -1 on transport/parse failure. */
int api_fetch_page(const char *cursor, page_result_t *out);

#endif /* USERSYNC_API_CLIENT_H */
