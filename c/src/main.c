#include <curl/curl.h>
#include <stdio.h>
#include <stdlib.h>

#include "meterval.h"

#define REGISTRY_PATH "/etc/meterval/registry.csv"

static int http_curl_get(const char *url, const char *token, char **body)
{
    size_t len = 0;
    char auth[256];
    CURLcode rc = CURLE_OUT_OF_MEMORY;
    CURL *curl = curl_easy_init();
    FILE *sink = curl != NULL ? open_memstream(body, &len) : NULL;
    snprintf(auth, sizeof auth, "Authorization: Bearer %s", token);
    struct curl_slist *headers = curl_slist_append(NULL, auth);
    if (sink != NULL) {
        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, sink);
        curl_easy_setopt(curl, CURLOPT_FAILONERROR, 1L);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);
        rc = curl_easy_perform(curl);
        fclose(sink);
        if (rc != CURLE_OK)
            free(*body);
    }
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    return rc == CURLE_OK ? 0 : -1;
}

int main(int argc, char **argv)
{
    struct meter_config cfg;
    struct reading *readings = NULL;
    struct batch_result res;
    size_t n = 0;
    const char *zone = argc == 2 ? argv[1] : NULL;
    if (zone == NULL || config_load(&cfg) != 0 || !config_zone_allowed(&cfg, zone)) {
        fprintf(stderr, "usage: %s <zone> (zone must be listed in METER_ALLOWED_ZONES)\n", argv[0]);
        return 64;
    }
    if (registry_load(REGISTRY_PATH) < 0 ||
        meter_api_fetch_zone(&cfg, http_curl_get, zone, &readings, &n) != 0 ||
        validator_run(&cfg, registry_status, readings, n, &res) != 0) {
        fprintf(stderr, "meterval: cannot validate zone %s\n", zone);
        free(readings);
        return 69;
    }
    free(readings);

    int rc = 0;
    if (res.rejected_count == 0) {
        printf("zone %s: all %zu readings valid\n", zone, res.accepted);
    } else {
        printf("zone %s: %zu accepted, %zu rejected\n", zone, res.accepted, res.rejected_count);
        rc = archive_rejected(&cfg, zone, &res) == 0 ? 2 : 74;
    }
    batch_result_free(&res);
    return rc;
}
