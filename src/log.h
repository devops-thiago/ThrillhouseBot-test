#ifndef USERSYNC_LOG_H
#define USERSYNC_LOG_H

/* Writes an informational line to stdout. */
void log_info(const char *msg);

/* Writes an error line to stderr, prefixed with "error: ". */
void log_error(const char *msg);

#endif /* USERSYNC_LOG_H */
