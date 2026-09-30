#include <stdio.h>
#include <string.h>
#include "notifier.h"

int notify_stdout(const char *recipient, const char *body)
{
    if (recipient == NULL || *recipient == '\0')
        return NOTIFY_ERR_NO_RECIPIENT;
    if (printf("To: %s\n%s\n\n", recipient, body) < 0)
        return NOTIFY_ERR_IO;
    return NOTIFY_OK;
}

size_t notify_overdue(const loan_list_t *overdue, notify_fn send, size_t *failed)
{
    size_t sent = 0;
    char body[128];
    *failed = 0;
    for (size_t i = 0; i < overdue->count; i++) {
        const loan_t *l = &overdue->items[i];
        snprintf(body, sizeof body, "Loan %s is overdue. Please return it.", l->id);
        if (send(l->email, body) == NOTIFY_OK)
            sent++;
        else
            (*failed)++;
    }
    return sent;
}
