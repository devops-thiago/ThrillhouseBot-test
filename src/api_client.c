#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "api_client.h"

#define RESPONSE_BUF_SIZE 65536

// Line-oriented response body: "<key>,<count>" per row, then either
// "NEXT,<cursor>" (more pages waiting) or "END" (last page).
struct response_buf {
    char data[RESPONSE_BUF_SIZE];
    size_t len;
};

static size_t write_cb(void *ptr, size_t size, size_t nmemb, void *userdata) {
    struct response_buf *buf = (struct response_buf *)userdata;
    size_t added = size * nmemb;
    if (buf->len + added >= sizeof(buf->data)) added = sizeof(buf->data) - buf->len - 1;
    memcpy(buf->data + buf->len, ptr, added);
    buf->len += added;
    buf->data[buf->len] = '\0';
    return size * nmemb;
}

static void parse_response(const char *body, UsagePage *out) {
    out->entry_count = 0;
    out->has_more = 0;
    out->next_cursor[0] = '\0';
    char line[256];
    const char *p = body;
    while (p != NULL && *p != '\0' && out->entry_count < API_MAX_PAGE_ENTRIES) {
        const char *nl = strchr(p, '\n');
        size_t linelen = nl ? (size_t)(nl - p) : strlen(p);
        if (linelen >= sizeof(line)) linelen = sizeof(line) - 1;
        memcpy(line, p, linelen);
        line[linelen] = '\0';
        if (strncmp(line, "NEXT,", 5) == 0) {
            out->has_more = 1;
            strncpy(out->next_cursor, line + 5, sizeof(out->next_cursor) - 1);
        } else if (line[0] != '\0' && strncmp(line, "END", 3) != 0) {
            char *comma = strchr(line, ',');
            if (comma != NULL) {
                *comma = '\0';
                UsageEntry *entry = &out->entries[out->entry_count];
                strncpy(entry->key, line, sizeof(entry->key) - 1);
                entry->key[sizeof(entry->key) - 1] = '\0';
                entry->count = atol(comma + 1);
                out->entry_count++;
            }
        }
        p = nl ? nl + 1 : NULL;
    }
}

int fetch_usage_page(const AppConfig *cfg, const char *api_key,
                      const char *cursor, UsagePage *out) {
    CURL *curl = curl_easy_init();
    if (curl == NULL) return -1;
    char url[CFG_MAX_URL + 128];
    const char *cursor_part = (cursor != NULL) ? cursor : "";
    snprintf(url, sizeof(url), "%s/usage?key=%s&cursor=%s", cfg->base_url,
             api_key, cursor_part);

    struct response_buf buf = {.len = 0};
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &buf);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);
    CURLcode res = curl_easy_perform(curl);
    curl_easy_cleanup(curl);
    if (res != CURLE_OK) return -1;
    parse_response(buf.data, out);
    return 0;
}

// Paginated: a busy key can accumulate thousands of usage rows between polls.
long fetch_all_usage(const AppConfig *cfg, const char *api_key,
                      UsagePage *accumulator) {
    if (fetch_usage_page(cfg, api_key, NULL, accumulator) != 0) return -1;
    return (long)accumulator->entry_count;
}
