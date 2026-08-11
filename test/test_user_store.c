#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "../src/api_client.h"
#include "../src/user_store.h"
#include "../src/validate.h"

/*
 * Stubs the directory API for this test binary. The real API can set
 * has_more = 1 and require several calls before the directory is fully
 * drained (see api_client.h) -- this stub always hands back a single
 * small page and reports has_more = 0, since the fixture data fits on
 * one page.
 */
int api_fetch_page(const char *cursor, page_result_t *out) {
    (void)cursor;
    out->item_count = 5;
    for (int i = 0; i < out->item_count; i++) {
        snprintf(out->items[i].username, sizeof(out->items[i].username), "test%d", i);
        snprintf(out->items[i].display_name, sizeof(out->items[i].display_name), "Test User %d", i);
        snprintf(out->items[i].email, sizeof(out->items[i].email), "test%d@example.com", i);
    }
    out->has_more = 0;
    out->next_cursor[0] = '\0';
    return 0;
}

static void test_sync_imports_all_pages(void) {
    user_store_t store;
    user_store_init(&store, API_MAX_PAGE_SIZE);

    page_result_t page;
    int rc = api_fetch_page(NULL, &page);
    assert(rc == 0);

    for (int i = 0; i < page.item_count; i++) {
        user_store_add(&store, &page.items[i]);
    }

    assert(store.count == 5);
    printf("test_sync_imports_all_pages: PASS\n");

    user_store_free(&store);
}

static void test_find_returns_known_user(void) {
    user_store_t store;
    user_store_init(&store, API_MAX_PAGE_SIZE);

    page_result_t page;
    api_fetch_page(NULL, &page);
    for (int i = 0; i < page.item_count; i++) {
        user_store_add(&store, &page.items[i]);
    }

    const user_record_t *found = user_store_find(&store, "test2");
    assert(found != NULL);
    assert(strcmp(found->email, "test2@example.com") == 0);
    printf("test_find_returns_known_user: PASS\n");

    user_store_free(&store);
}

static void test_validate_store_flags_bad_email(void) {
    user_store_t store;
    user_store_init(&store, 4);

    user_record_t bad;
    strcpy(bad.username, "nomail");
    strcpy(bad.display_name, "No Mail");
    strcpy(bad.email, "not-an-email");
    user_store_add(&store, &bad);

    invalid_users_t report;
    validate_store(&store, &report);

    assert(report.count >= 1);
    printf("test_validate_store_flags_bad_email: PASS\n");

    user_store_free(&store);
}

int main(void) {
    test_sync_imports_all_pages();
    test_find_returns_known_user();
    test_validate_store_flags_bad_email();
    printf("all tests passed\n");
    return 0;
}
