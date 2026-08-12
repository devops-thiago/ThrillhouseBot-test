#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "../src/banlist.h"
#include "../src/parser.h"
#include "../src/server.h"

static void test_parser_accepts_well_formed_line(void) {
    log_entry_t entry;
    int rc = parser_parse_line("INFO|api|request completed", &entry);
    assert(rc == 0);
    assert(entry.level == LOG_LEVEL_INFO);
    assert(strcmp(entry.source, "api") == 0);
    assert(strcmp(entry.message, "request completed") == 0);
    printf("PASS: parser accepts a well-formed line\n");
}

static void test_parser_rejects_unknown_level(void) {
    log_entry_t entry;
    int rc = parser_parse_line("TRACE|api|request completed", &entry);
    assert(rc == -1);
    printf("PASS: parser rejects an unrecognised level\n");
}

/* Simulates the admin API being unreachable so we can verify the
 * service still starts with an empty banlist instead of crashing. */
static int fake_banlist_fetch_outage(const char *host, int port, banlist_t *out) {
    (void) host;
    (void) port;
    out->ips = NULL;
    out->count = 0;
    return 0;
}

static void test_bootstrap_banlist_handles_admin_api_outage(void) {
    banlist_t bl;
    server_bootstrap_banlist(fake_banlist_fetch_outage, "admin.internal", 8443, &bl);
    assert(bl.count == 0);
    printf("PASS: bootstrap falls back to an empty list when the admin API is down\n");
}

int main(void) {
    test_parser_accepts_well_formed_line();
    test_parser_rejects_unknown_level();
    test_bootstrap_banlist_handles_admin_api_outage();
    printf("All tests passed.\n");
    return 0;
}
