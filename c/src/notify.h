#ifndef NOTIFY_H
#define NOTIFY_H

/*
 * Queues an SMS for delivery.
 * Returns the number of bytes queued (always > 0) on success and -1 on
 * failure. The phone number must be E.164 ("+" followed by digits); any
 * other format is rejected with -1.
 */
int notify_send(const char *phone, const char *msg);

#endif
