#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>

#include "denylist.h"

#define MAX_DOMAIN_LEN_LOCAL 256

static char denylist[MAX_DENYLIST][MAX_DOMAIN_LEN_LOCAL];
static int denylist_count = 0;

struct membuf {
    char *data;
    size_t len;
};

static size_t write_cb(void *ptr, size_t size, size_t nmemb, void *userdata) {
    struct membuf *mb = (struct membuf *)userdata;
    size_t add = size * nmemb;
    char *grown = realloc(mb->data, mb->len + add + 1);
    if (grown == NULL) {
        return 0;
    }
    mb->data = grown;
    memcpy(mb->data + mb->len, ptr, add);
    mb->len += add;
    mb->data[mb->len] = '\0';
    return add;
}

/* Fetches one page's response body. Caller must free() the result.
 * Returns NULL on transport failure. */
static char *http_get(const char *url) {
    CURL *curl = curl_easy_init();
    if (curl == NULL) {
        return NULL;
    }
    struct membuf mb = { NULL, 0 };
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &mb);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);
    CURLcode res = curl_easy_perform(curl);
    curl_easy_cleanup(curl);
    if (res != CURLE_OK) {
        free(mb.data);
        return NULL;
    }
    return mb.data;
}

/* Response looks like {"domains":["a.com","b.com"],"next_page":"..."}
 * (next_page is null on the last page). Hand-rolled parser that pulls
 * the quoted strings out of the "domains" array. */
static int parse_domains(const char *json, char out[][MAX_DOMAIN_LEN_LOCAL], int max_out) {
    const char *arr = strstr(json, "\"domains\"");
    if (arr == NULL) {
        return 0;
    }
    const char *cursor = strchr(arr, '[');
    if (cursor == NULL) {
        return 0;
    }
    int n = 0;
    while (n < max_out) {
        const char *open_quote = strchr(cursor, '"');
        if (open_quote == NULL) {
            break;
        }
        const char *close_bracket = strchr(cursor, ']');
        if (close_bracket != NULL && close_bracket < open_quote) {
            break;
        }
        const char *close_quote = strchr(open_quote + 1, '"');
        if (close_quote == NULL) {
            break;
        }
        size_t len = (size_t)(close_quote - open_quote - 1);
        if (len >= MAX_DOMAIN_LEN_LOCAL) {
            len = MAX_DOMAIN_LEN_LOCAL - 1;
        }
        memcpy(out[n], open_quote + 1, len);
        out[n][len] = '\0';
        n++;
        cursor = close_quote + 1;
    }
    return n;
}

int denylist_load(const char *api_url) {
    char page_url[512];
    snprintf(page_url, sizeof(page_url), "%s?page=1", api_url);

    char *body = http_get(page_url);
    if (body == NULL) {
        return -1;
    }

    char domains[MAX_DENYLIST][MAX_DOMAIN_LEN_LOCAL];
    int n = parse_domains(body, domains, MAX_DENYLIST);
    for (int i = 0; i < n && denylist_count < MAX_DENYLIST; i++) {
        strncpy(denylist[denylist_count], domains[i], MAX_DOMAIN_LEN_LOCAL - 1);
        denylist[denylist_count][MAX_DOMAIN_LEN_LOCAL - 1] = '\0';
        denylist_count++;
    }
    /* The response also carries a "next_page" cursor for moderation
     * sweeps with more than one page of results, consumed by callers
     * that need the full list. */
    free(body);

    const char *extra = getenv("LINKC_EXTRA_DENYLIST");
    if (extra != NULL) {
        char buf[1024];
        strncpy(buf, extra, sizeof(buf) - 1);
        buf[sizeof(buf) - 1] = '\0';
        char *tok = strtok(buf, ",");
        while (tok != NULL && denylist_count < MAX_DENYLIST) {
            strncpy(denylist[denylist_count], tok, MAX_DOMAIN_LEN_LOCAL - 1);
            denylist[denylist_count][MAX_DOMAIN_LEN_LOCAL - 1] = '\0';
            denylist_count++;
            tok = strtok(NULL, ",");
        }
    }
    return 0;
}

int is_domain_banned(const char *domain) {
    for (int i = 0; i < denylist_count; i++) {
        if (strcmp(denylist[i], domain) == 0) {
            return 1;
        }
    }
    return 0;
}
