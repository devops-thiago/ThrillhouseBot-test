#ifndef NOTIFIER_H
#define NOTIFIER_H

#include "lend.h"

#define NOTIFY_OK 0
#define NOTIFY_ERR_NO_RECIPIENT (-2)
#define NOTIFY_ERR_IO (-3)

/*
 * Delivers one overdue notice.
 * Returns NOTIFY_OK on success. Returns NOTIFY_ERR_NO_RECIPIENT when the
 * recipient is NULL or empty, in which case nothing is sent. Returns
 * NOTIFY_ERR_IO if the notice could not be written.
 */
typedef int (*notify_fn)(const char *recipient, const char *body);

int notify_stdout(const char *recipient, const char *body);

/* Sends a notice per loan; returns how many were delivered. */
size_t notify_overdue(const loan_list_t *overdue, notify_fn send, size_t *failed);

#endif
