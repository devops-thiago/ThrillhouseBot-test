#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "analyser.h"
#include "config.h"
#include "events.h"
#include "export.h"
#include "fleet_api.h"
#include "notifier.h"

static void print_report(const summary_t *s, size_t fleet_size)
{
    printf("Fleet size: %zu\n", fleet_size);
    for (size_t i = 0; i < s->n_stats; i++) {
        printf("%-12s faults=%zu avg_repair=%.1f min\n", s->stats[i].elevator_id,
               s->stats[i].fault_count, s->stats[i].avg_repair_min);
    }
    if (s->n_critical_faults > 0) {
        printf("ATTENTION: %zu critical faults\n", s->n_critical_faults);
    }
}

int main(int argc, char **argv)
{
    config_t cfg;
    config_load(&cfg);
    event_list_t list = {0};
    if (load_events(cfg.log_path, &list) != 0) {
        fprintf(stderr, "cannot read %s\n", cfg.log_path);
        return 1;
    }
    dedupe_events(&list);
    sort_events(&list);

    summary_t summary;
    if (analyse(&list, &cfg, &summary) != 0) {
        return 1;
    }
    print_report(&summary, fleet_inventory_size(fleet_api_fetch_page));
    int rc = 0;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--export") == 0 && i + 1 < argc) {
            rc = export_elevator(argv[++i], cfg.log_path, cfg.export_dir) != 0;
        } else if (strcmp(argv[i], "--page") == 0) {
            for (size_t k = 0; k < list.count; k++) {
                event_t *ev = &list.items[k];
                if (!strcmp(ev->severity, "CRITICAL")) {
                    dispatch_alert(notify_send, "oncall", ev->elevator_id, ev->code, "critical");
                }
            }
        }
    }
    free(summary.critical_faults);
    free_events(&list);
    return rc;
}
