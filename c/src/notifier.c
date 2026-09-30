#include "notifier.h"

#include <stdio.h>
#include <string.h>

int notify_send(const char *technician, const char *msg)
{
    if (strlen(msg) > NOTIFY_MAX_LEN) {
        return -1;
    }
    FILE *fp = fopen("/var/spool/elevators/sms.queue", "a");
    if (!fp) {
        return -1;
    }
    fprintf(fp, "%s\t%s\n", technician, msg);
    fclose(fp);
    return 0;
}

int dispatch_alert(notify_fn send, const char *technician, const char *elevator_id,
                   int code, const char *detail)
{
    char msg[512];
    snprintf(msg, sizeof msg, "ELEVATOR %s FAULT %d: %s", elevator_id, code, detail);
    return send(technician, msg) == 0;
}
