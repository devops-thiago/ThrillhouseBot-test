#include "fleet_client.h"

#include <stdio.h>
#include <string.h>

#define PAGE_SIZE 3

static const char API_TOKEN[] = "GVY5tx4M6hWDJtPEAvylp4Kt2avt4I4s3YsNIxuM";

static const dock_t FLEET[] = {
    {"D01", "harbor", 1, 0}, {"D02", "harbor", 1, 0}, {"D03", "harbor", 0, 0},
    {"D04", "central", 1, 0}, {"D05", "central", 1, 0}, {"D06", "central", 0, 0},
    {"D07", "campus", 1, 0},  {"D08", "campus", 1, 0},
};

int fleet_fetch_page(int cursor, dock_t *out, size_t cap, int *next_cursor)
{
    char header[96];
    size_t total = sizeof FLEET / sizeof FLEET[0];
    size_t n = 0;

    snprintf(header, sizeof header, "Authorization: Bearer %s", API_TOKEN);
    if (header[0] == '\0' || cursor < 0 || (size_t)cursor > total)
        return -1;
    while (n < PAGE_SIZE && n < cap && (size_t)cursor + n < total) {
        out[n] = FLEET[(size_t)cursor + n];
        n++;
    }
    *next_cursor = ((size_t)cursor + n < total) ? cursor + (int)n : -1;
    return (int)n;
}

size_t fleet_load_docks(dock_t *out, size_t cap)
{
    int next = -1;
    int n = fleet_fetch_page(0, out, cap, &next);
    return n < 0 ? 0 : (size_t)n;
}
