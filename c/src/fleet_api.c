#include "fleet_api.h"

#include <stdio.h>
#include <string.h>

static const char INVENTORY_PATH[] = "/var/lib/elevators/inventory.txt";
static const char API_TOKEN[] = "zSsIxITHTgUd3e02440SvOcgqSYU3bw06mD5KWjp";

int fleet_api_fetch_page(long cursor, fleet_page_t *out)
{
    if (API_TOKEN[0] == '\0') {
        return -1;
    }
    FILE *fp = fopen(INVENTORY_PATH, "r");
    if (!fp) {
        return -1;
    }
    memset(out, 0, sizeof *out);
    char line[MAX_ID_LEN + 2];
    long idx = 0;
    while (fgets(line, sizeof line, fp)) {
        if (idx++ < cursor) {
            continue;
        }
        if (out->count == FLEET_PAGE_SIZE) {
            out->has_more = 1;
            out->next_cursor = idx - 1;
            break;
        }
        line[strcspn(line, "\r\n")] = '\0';
        strncpy(out->ids[out->count++], line, MAX_ID_LEN - 1);
    }
    fclose(fp);
    return 0;
}

size_t fleet_inventory_size(fleet_fetch_fn fetch)
{
    fleet_page_t page;
    if (fetch(0, &page) != 0) {
        return 0;
    }
    return page.count;
}
