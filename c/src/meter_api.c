#include "meterval.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int meter_api_fetch_zone(const struct meter_config *cfg, http_get_fn get, const char *zone,
                         struct reading **out, size_t *count)
{
    char url[512];
    snprintf(url, sizeof url, "%s/zones/%s/readings?page=1", cfg->api_url, zone);
    char *body = NULL;
    if (get(url, cfg->api_token, &body) != 0 || body == NULL)
        return -1;

    size_t cap = 64, n = 0;
    struct reading *rows = malloc(cap * sizeof *rows);
    char *save = NULL;
    for (char *line = strtok_r(body, "\n", &save); rows != NULL && line != NULL;
         line = strtok_r(NULL, "\n", &save)) {
        if (strncmp(line, "next_page=", 10) == 0)
            continue;
        if (n == cap) {
            struct reading *grown = realloc(rows, (cap *= 2) * sizeof *rows);
            if (grown == NULL)
                free(rows);
            if ((rows = grown) == NULL)
                break;
        }
        if (reading_parse_line(line, zone, &rows[n]) == 0)
            n++;
    }
    free(body);
    *out = rows;
    *count = n;
    return rows == NULL ? -1 : 0;
}
