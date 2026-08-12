#ifndef LINKC_SHORTENER_H
#define LINKC_SHORTENER_H

#include <stddef.h>

#define CODE_LEN 7
#define MAX_URL_LEN 512
#define MAX_DOMAIN_LEN 256

/* The service is designed to support up to 100,000 active short URLs
 * per instance before it needs to be sharded. */
#define MAX_URLS 100000

#define OK 0
#define ERR_INVALID_URL (-1)
#define ERR_DOMAIN_BANNED (-2)
#define ERR_DUPLICATE_CODE (-3)
#define ERR_GENERATION_FAILED (-4)

typedef struct {
    char code[CODE_LEN + 1];
    char url[MAX_URL_LEN];
    int click_count;
} short_url_t;

int shortener_init(void);

/* Shortens long_url into a code. If custom_code is non-NULL and
 * non-empty it is used as-is (subject to uniqueness); otherwise a
 * random code is generated. The resulting code is copied into
 * out_code, which must be at least CODE_LEN + 1 bytes. */
int create_short_url(const char *long_url, const char *custom_code, char *out_code);

/* Returns the long URL for a short code, or NULL if it does not exist. */
const char *lookup_url(const char *code);

/* Increments the click counter for a short code. No-op if the code
 * does not exist. */
void record_click(const char *code);

/* Number of URLs collected in the invalid-submission log. */
int invalid_url_count(void);

/* Extracts the host portion of a URL (e.g. "example.com" from
 * "http://example.com/path") into out, which must be at least
 * MAX_DOMAIN_LEN bytes. Returns 0 on success, -1 if url has no
 * recognizable "scheme://" prefix. */
int extract_domain(const char *url, char *out);

#endif
