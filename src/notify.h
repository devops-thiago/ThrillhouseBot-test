#ifndef NOTIFY_H
#define NOTIFY_H

/* Emits a system-log line recording that `code` was redeemed against
 * `order_id`, via the host's `logger` utility. Returns 0 on success. */
int notify_redemption(const char *code, const char *order_id);

#endif /* NOTIFY_H */
