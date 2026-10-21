#ifndef DOCK_H
#define DOCK_H

#include <stddef.h>

#include "config.h"

#define MAX_DOCKS 64
#define MAX_ISSUED 128

typedef struct {
    char id[16];
    char station[STATION_NAME_LEN];
    int occupied;
    int reserved;
} dock_t;

typedef struct {
    char rider[24];
    unsigned code;
    char dock_id[16];
    long issued_at;
} issued_t;

typedef struct {
    dock_t docks[MAX_DOCKS];
    size_t dock_count;
    issued_t issued[MAX_ISSUED];
    size_t issued_count;
    const config_t *cfg;
} dispenser_t;

typedef int (*notify_fn)(const char *phone, const char *msg);

enum {
    DISP_OK = 0,
    DISP_NO_DOCK = -1,
    DISP_LIMIT = -2,
    DISP_NOTIFY = -3,
    DISP_FULL = -4
};

void dispenser_init(dispenser_t *d, const config_t *cfg);
int dispenser_issue(dispenser_t *d, const char *rider, const char *phone,
                    const char *station, long now, notify_fn notify,
                    unsigned *code_out);
size_t dispenser_sweep(dispenser_t *d, long now);

#endif
