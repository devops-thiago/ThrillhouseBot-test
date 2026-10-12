#include "meterval.h"

#include <stdio.h>
#include <string.h>

#define REGISTRY_MAX 4096

static struct {
    char serial[SERIAL_LEN + 1];
    enum registry_status status;
} entries[REGISTRY_MAX];
static size_t entry_count;

int registry_load(const char *path)
{
    char status[32];
    FILE *fp = fopen(path, "r");
    if (fp == NULL)
        return -1;
    for (entry_count = 0; entry_count < REGISTRY_MAX &&
                          fscanf(fp, " %32[^,],%31s", entries[entry_count].serial, status) == 2;
         entry_count++)
        entries[entry_count].status = strcmp(status, "active") == 0 ? REG_ACTIVE : REG_DECOMMISSIONED;
    fclose(fp);
    return (int)entry_count;
}

enum registry_status registry_status(const char *serial)
{
    for (size_t i = 0; i < entry_count; i++)
        if (strcmp(entries[i].serial, serial) == 0)
            return entries[i].status;
    return REG_UNKNOWN;
}
