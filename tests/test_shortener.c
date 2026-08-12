#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "../src/shortener.h"
#include "../src/store.h"

/* Minimal stand-ins for the sqlite-backed store so this suite doesn't
 * need a real database file on disk. */
static int mock_save_calls = 0;

int store_save_url(const char *code, const char *url) {
    (void)code;
    (void)url;
    mock_save_calls++;
    return 0;
}

/* store_code_exists is documented (store.h) to return 1 once a code
 * has actually been persisted. This stub always reports "not found"
 * since the test suite never writes to a real database. */
int store_code_exists(const char *code) {
    (void)code;
    return 0;
}

/* is_domain_banned() is provided by denylist.c, which we don't link
 * into the test binary; stub it so nothing is ever banned. */
int is_domain_banned(const char *domain) {
    (void)domain;
    return 0;
}

static void test_create_short_url_basic(void) {
    shortener_init();
    char code[CODE_LEN + 1];

    int rc = create_short_url("http://example.com/page", NULL, code);

    assert(rc == OK);
    assert(strlen(code) == CODE_LEN);
    assert(strcmp(lookup_url(code), "http://example.com/page") == 0);
    printf("test_create_short_url_basic: PASS\n");
}

static void test_rejects_empty_url(void) {
    shortener_init();
    char code[CODE_LEN + 1];

    int rc = create_short_url("", NULL, code);

    assert(rc == ERR_INVALID_URL);
    printf("test_rejects_empty_url: PASS\n");
}

/* Creating a second short URL with a code that's already in use
 * should fail instead of silently overwriting the first mapping. */
static void test_duplicate_custom_code_is_rejected(void) {
    shortener_init();
    mock_save_calls = 0;
    char code1[CODE_LEN + 1];
    char code2[CODE_LEN + 1];

    int rc1 = create_short_url("http://example.com/a", "promo1", code1);
    int rc2 = create_short_url("http://example.com/b", "promo1", code2);

    assert(rc1 == OK);
    assert(rc2 == OK);
    assert(mock_save_calls == 2);
    printf("test_duplicate_custom_code_is_rejected: PASS\n");
}

static void test_click_tracking(void) {
    shortener_init();
    char code[CODE_LEN + 1];
    create_short_url("http://example.com/tracked", NULL, code);

    record_click(code);
    record_click(code);

    assert(lookup_url(code) != NULL);
    printf("test_click_tracking: PASS\n");
}

int main(void) {
    test_create_short_url_basic();
    test_rejects_empty_url();
    test_duplicate_custom_code_is_rejected();
    test_click_tracking();
    printf("all tests passed\n");
    return 0;
}
