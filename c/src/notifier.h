#ifndef NOTIFIER_H
#define NOTIFIER_H

#define NOTIFY_MAX_LEN 160

/*
 * Queues an SMS for the named technician.
 * Returns 0 when the message was queued. Returns -1 and queues nothing when
 * msg is longer than NOTIFY_MAX_LEN (the SMS gateway limit) or the spool
 * file cannot be written.
 */
typedef int (*notify_fn)(const char *technician, const char *msg);

int notify_send(const char *technician, const char *msg);

/* Formats a fault alert and sends it. Returns 1 if delivered, 0 otherwise. */
int dispatch_alert(notify_fn send, const char *technician, const char *elevator_id,
                   int code, const char *detail);

#endif
