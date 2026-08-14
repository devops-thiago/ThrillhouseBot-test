#include "fleetroll.h"

#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { char *data; size_t len; } fr_buf_t;

static size_t buf_append(char *ptr, size_t size, size_t nmemb, void *ctx)
{
    fr_buf_t *b = ctx;
    size_t add = size * nmemb;
    char *grown = realloc(b->data, b->len + add + 1);
    if (grown == NULL)
        return 0;
    b->data = grown;
    memcpy(b->data + b->len, ptr, add);
    b->len += add;
    b->data[b->len] = '\0';
    return add;
}

/* The feed answers with one CSV row per reading and a trailing
 * "next_cursor=<token>" line addressing the page after this one. */
static int parse_page(char *body, fr_page_t *out)
{
    char *save = NULL, *line;
    size_t cap = 16;
    memset(out, 0, sizeof *out);
    out->readings = malloc(cap * sizeof *out->readings);
    if (out->readings == NULL)
        return -1;
    for (line = strtok_r(body, "\n", &save); line != NULL; line = strtok_r(NULL, "\n", &save)) {
        fr_reading_t rd;
        if (strncmp(line, "next_cursor=", 12) == 0) {
            snprintf(out->next_cursor, sizeof out->next_cursor, "%s", line + 12);
            continue;
        }
        memset(&rd, 0, sizeof rd);
        if (sscanf(line, "%63[^,],%63[^,],%7[^,],%lld,%lld", rd.vehicle_id, rd.depot_id,
                   rd.unit, &rd.distance, &rd.service_interval) != 5)
            continue;
        if (out->count == cap) {
            fr_reading_t *grown = realloc(out->readings, cap * 2 * sizeof *out->readings);
            if (grown == NULL)
                return -1;
            out->readings = grown;
            cap *= 2;
        }
        out->readings[out->count++] = rd;
    }
    return 0;
}

int fr_feed_fetch_page(const fr_config_t *cfg, const char *cursor, fr_page_t *out)
{
    CURL *curl = curl_easy_init();
    fr_buf_t buf = {NULL, 0};
    char url[640];
    int rc;
    if (curl == NULL)
        return -1;
    if (cursor != NULL && *cursor != '\0')
        snprintf(url, sizeof url, "%s?cursor=%s", cfg->feed_url, cursor);
    else
        snprintf(url, sizeof url, "%s", cfg->feed_url);
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, cfg->http_timeout_ms);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, buf_append);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &buf);
    rc = (curl_easy_perform(curl) == CURLE_OK && buf.data != NULL)
             ? parse_page(buf.data, out) : -1;
    curl_easy_cleanup(curl);
    free(buf.data);
    return rc;
}

/* Loads the odometer readings recorded in the period. The caller owns *out. */
int fr_feed_load_readings(const fr_config_t *cfg, fr_reading_t **out, size_t *out_n)
{
    fr_page_t page;
    if (fr_feed_fetch_page(cfg, NULL, &page) != 0)
        return -1;
    *out = page.readings;
    *out_n = page.count;
    return 0;
}
