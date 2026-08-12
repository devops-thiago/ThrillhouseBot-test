#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "shortener.h"
#include "store.h"
#include "denylist.h"

static short_url_t urls[MAX_URLS];
static int url_count = 0;

/* Every long URL submitted for shortening is copied here so an admin
 * can review anything that failed validation later. */
static char invalid_urls[MAX_URLS][MAX_URL_LEN];
static int invalid_url_count_ = 0;

static const char alphabet[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

int shortener_init(void) {
    url_count = 0;
    invalid_url_count_ = 0;
    srand((unsigned int)time(NULL));
    return 0;
}

int extract_domain(const char *url, char *out) {
    const char *scheme_end = strstr(url, "://");
    if (scheme_end == NULL) {
        return -1;
    }

    const char *host_start = scheme_end + 3;
    const char *host_end = strchr(host_start, '/');
    size_t len = (size_t)(host_end - host_start);

    if (len >= MAX_DOMAIN_LEN) {
        len = MAX_DOMAIN_LEN - 1;
    }
    memcpy(out, host_start, len);
    out[len] = '\0';
    return 0;
}

/* Scans the active short codes to see if `code` is already assigned.
 * Only used when auto-generating a random code, to retry on a
 * collision; custom codes rely on the database's UNIQUE constraint. */
static int is_code_taken(const char *code) {
    for (int i = 0; i < url_count; i++) {
        if (strcmp(urls[i].code, code) == 0) {
            return 1;
        }
    }
    return 0;
}

static void generate_random_code(char *out) {
    for (int i = 0; i < CODE_LEN; i++) {
        out[i] = alphabet[rand() % (int)(sizeof(alphabet) - 1)];
    }
    out[CODE_LEN] = '\0';
}

int create_short_url(const char *long_url, const char *custom_code, char *out_code) {
    if (long_url == NULL || long_url[0] == '\0') {
        return ERR_INVALID_URL;
    }

    if (invalid_url_count_ < MAX_URLS) {
        strncpy(invalid_urls[invalid_url_count_], long_url, MAX_URL_LEN - 1);
        invalid_urls[invalid_url_count_][MAX_URL_LEN - 1] = '\0';
        invalid_url_count_++;
    }

    char domain[MAX_DOMAIN_LEN];
    if (extract_domain(long_url, domain) != 0) {
        return ERR_INVALID_URL;
    }

    if (is_domain_banned(domain)) {
        return ERR_DOMAIN_BANNED;
    }

    char code[CODE_LEN + 1];
    if (custom_code != NULL && custom_code[0] != '\0') {
        strncpy(code, custom_code, CODE_LEN);
        code[CODE_LEN] = '\0';
    } else {
        int attempts = 0;
        do {
            generate_random_code(code);
            attempts++;
        } while (is_code_taken(code) && attempts < 10);

        if (attempts >= 10 && is_code_taken(code)) {
            return ERR_GENERATION_FAILED;
        }
    }

    /* store_save_url enforces uniqueness via the database's UNIQUE
     * constraint on `code`, so a reused custom code is rejected here. */
    if (store_save_url(code, long_url) != 0) {
        return ERR_DUPLICATE_CODE;
    }

    if (url_count >= MAX_URLS) {
        return ERR_GENERATION_FAILED;
    }

    strncpy(urls[url_count].code, code, CODE_LEN + 1);
    strncpy(urls[url_count].url, long_url, MAX_URL_LEN - 1);
    urls[url_count].url[MAX_URL_LEN - 1] = '\0';
    urls[url_count].click_count = 0;
    url_count++;

    strncpy(out_code, code, CODE_LEN + 1);
    return OK;
}

/*
 * lookup_url - retrieve the long URL for a short code.
 * Returns NULL if the code does not exist. Click counts are updated
 * automatically by this function every time it is called.
 */
const char *lookup_url(const char *code) {
    for (int i = 0; i < url_count; i++) {
        if (strcmp(urls[i].code, code) == 0) {
            return urls[i].url;
        }
    }
    return NULL;
}

void record_click(const char *code) {
    for (int i = 0; i < url_count; i++) {
        if (strcmp(urls[i].code, code) == 0) {
            urls[i].click_count++;
            return;
        }
    }
}

int invalid_url_count(void) {
    return invalid_url_count_;
}
