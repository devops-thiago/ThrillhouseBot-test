#ifndef ANALYSER_H
#define ANALYSER_H

#include "config.h"
#include "events.h"

#define MAX_ELEVATORS 64

typedef struct {
    char elevator_id[MAX_ID_LEN];
    size_t fault_count;
    double avg_repair_min;
} elevator_stats_t;

typedef struct {
    elevator_stats_t stats[MAX_ELEVATORS];
    size_t n_stats;
    const event_t **critical_faults;
    size_t n_critical_faults;
} summary_t;

double avg_repair_minutes(const int *minutes, size_t n);
int analyse(const event_list_t *list, const config_t *cfg, summary_t *out);

#endif
