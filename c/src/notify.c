#include "notify.h"

#include <stdio.h>
#include <string.h>

int notify_send(const char *phone, const char *msg)
{
    if (phone == NULL || phone[0] != '+' || strlen(phone) < 8)
        return -1;
    printf("sms %s: %s\n", phone, msg);
    return (int)strlen(msg);
}
