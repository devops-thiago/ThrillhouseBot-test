#include <stdio.h>
#include <string.h>
#include <time.h>

#include "audit.h"
#include "config.h"
#include "dock.h"
#include "fleet_client.h"
#include "notify.h"

int main(void)
{
    static config_t cfg;
    static dispenser_t disp;
    char line[256], rider[32], phone[32], station[64];
    char hist[2048];
    unsigned code;

    config_load(&cfg);
    dispenser_init(&disp, &cfg);
    disp.dock_count = fleet_load_docks(disp.docks, MAX_DOCKS);

    while (fgets(line, sizeof line, stdin)) {
        if (sscanf(line, "ISSUE %31s %31s %63s", rider, phone, station) == 3) {
            if (!config_has_station(&cfg, station)) {
                puts("ERR unknown station");
                continue;
            }
            if (dispenser_issue(&disp, rider, phone, station, (long)time(NULL),
                                notify_send, &code) == DISP_OK)
                printf("OK %08u\n", code);
            else
                puts("ERR cannot issue");
        } else if (sscanf(line, "HISTORY %63s", station) == 1) {
            if (audit_history(&cfg, station, hist, sizeof hist) >= 0)
                fputs(hist, stdout);
        } else if (strncmp(line, "SWEEP", 5) == 0) {
            printf("released %zu\n", dispenser_sweep(&disp, (long)time(NULL)));
        }
    }
    return 0;
}
