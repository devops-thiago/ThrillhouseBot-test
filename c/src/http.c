#include "carrier.h"

#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_BODY (1024 * 1024)

typedef struct { char *data; size_t len; } buffer;

static size_t on_data(char *ptr, size_t size, size_t nmemb, void *userdata) {
    buffer *b = userdata;
    size_t add = size * nmemb;
    if (b->len + add > MAX_BODY) return 0;
    char *grown = realloc(b->data, b->len + add + 1);
    if (grown == NULL) return 0;
    memcpy(grown + b->len, ptr, add);
    b->data = grown;
    b->data[b->len += add] = '\0';
    return add;
}

char *http_get(const char *base_url, const char *token, const char *path) {
    CURL *curl = curl_easy_init();
    if (curl == NULL) return NULL;
    char url[512], auth[256];
    snprintf(url, sizeof url, "%s%s", base_url, path);
    snprintf(auth, sizeof auth, "Authorization: Bearer %s", token);
    struct curl_slist *headers = curl_slist_append(NULL, auth);
    buffer b = {NULL, 0};
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, on_data);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &b);
    curl_easy_setopt(curl, CURLOPT_FAILONERROR, 1L);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);
    CURLcode rc = curl_easy_perform(curl);
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    if (rc != CURLE_OK) {
        free(b.data);
        return NULL;
    }
    return b.data;
}
