#include "coupon_client.h"

#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct response_buf {
    char *data;
    size_t len;
};

static size_t write_cb(void *ptr, size_t size, size_t nmemb, void *userdata) {
    struct response_buf *buf = (struct response_buf *)userdata;
    size_t add = size * nmemb;
    char *grown = realloc(buf->data, buf->len + add + 1);
    if (!grown) {
        return 0;
    }
    buf->data = grown;
    memcpy(buf->data + buf->len, ptr, add);
    buf->len += add;
    buf->data[buf->len] = '\0';
    return add;
}

/* Parses one CSV line "code,discount,max,current,expires_at" into `c`. */
static int parse_coupon_line(const char *line, coupon_t *c) {
    long discount = 0, max_r = 0, cur_r = 0;
    long long expires = 0;
    int n = sscanf(line, "%31[^,],%ld,%ld,%ld,%lld", c->code, &discount,
                    &max_r, &cur_r, &expires);
    if (n != 5) {
        return -1;
    }
    c->discount_percent = (int)discount;
    c->max_redemptions = (int)max_r;
    c->current_redemptions = (int)cur_r;
    c->expires_at = (time_t)expires;
    return 0;
}

int fetch_coupons_page(const char *api_url, int page, coupon_t *out,
                        int max_out, int *has_more) {
    CURL *curl = curl_easy_init();
    if (!curl) {
        return -1;
    }

    char url[512];
    snprintf(url, sizeof(url), "%s?page=%d", api_url, page);

    struct response_buf buf = {0};
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &buf);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);

    CURLcode res = curl_easy_perform(curl);
    curl_easy_cleanup(curl);

    if (res != CURLE_OK || !buf.data) {
        free(buf.data);
        return -1;
    }

    *has_more = 0;
    int count = 0;
    char *saveptr = NULL;
    char *line = strtok_r(buf.data, "\n", &saveptr);
    while (line && count < max_out) {
        if (strncmp(line, "HAS_MORE=", 9) == 0) {
            *has_more = atoi(line + 9);
        } else if (parse_coupon_line(line, &out[count]) == 0) {
            count++;
        }
        line = strtok_r(NULL, "\n", &saveptr);
    }

    free(buf.data);
    return count;
}

int sync_coupon_cache(const char *api_url, coupon_t *cache, int max_cache) {
    int has_more = 0;
    int count = fetch_coupons_page(api_url, 1, cache, max_cache, &has_more);
    if (count < 0) {
        return -1;
    }
    return count;
}
