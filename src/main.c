#include <stdio.h>
#include <stdlib.h>

#include "shortener.h"
#include "store.h"
#include "denylist.h"

static const char *env_or_default(const char *name, const char *fallback) {
    const char *v = getenv(name);
    return (v != NULL && v[0] != '\0') ? v : fallback;
}

/* A handful of submissions to exercise the pipeline on startup. In a
 * real deployment these would arrive over HTTP; this demo runner
 * keeps the example self-contained. */
static const char *demo_urls[] = {
    "http://example.com/promo/summer-sale",
    "http://blog.example.com",
    "https://news.example.org/latest",
    "http://shady-redirect.test/click?id=42",
};

int main(void) {
    const char *db_path = env_or_default("LINKC_DB_PATH", "./linkc.db");
    const char *port = env_or_default("LINKC_PORT", "8080");
    const char *denylist_api = env_or_default("LINKC_DENYLIST_API_URL",
                                               "https://moderation.internal/api/domains");

    if (shortener_init() != 0) {
        fprintf(stderr, "failed to initialize shortener\n");
        return 1;
    }

    if (store_open(db_path) != 0) {
        fprintf(stderr, "failed to open store at %s\n", db_path);
        return 1;
    }

    if (denylist_load(denylist_api) != 0) {
        fprintf(stderr, "warning: could not load denylist from %s\n", denylist_api);
    }

    int n = (int)(sizeof(demo_urls) / sizeof(demo_urls[0]));
    for (int i = 0; i < n; i++) {
        char code[CODE_LEN + 1];
        int rc = create_short_url(demo_urls[i], NULL, code);
        if (rc == OK) {
            printf("[linkc] %s -> %s (listening on port %s)\n", demo_urls[i], code, port);
        } else {
            printf("[linkc] rejected %s (code %d)\n", demo_urls[i], rc);
        }
    }

    int failed = invalid_url_count();
    if (failed == 0) {
        printf("[linkc] all submitted URLs passed validation\n");
    } else {
        printf("[linkc] %d URLs failed validation and were logged for review\n", failed);
    }

    store_close();
    return 0;
}
