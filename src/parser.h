#ifndef LOGD_PARSER_H
#define LOGD_PARSER_H

#define PARSER_MAX_MESSAGE 128
#define PARSER_MAX_SOURCE 32

typedef enum {
    LOG_LEVEL_INFO,
    LOG_LEVEL_WARN,
    LOG_LEVEL_ERROR,
    LOG_LEVEL_UNKNOWN
} log_level_t;

typedef struct {
    log_level_t level;
    char source[PARSER_MAX_SOURCE];
    char message[PARSER_MAX_MESSAGE];
} log_entry_t;

/* Parses a single "LEVEL|SOURCE|MESSAGE" line into `out`, truncating
 * the message to fit within PARSER_MAX_MESSAGE.
 * Returns 0 on success, -1 if the line is malformed (missing a
 * field or an unrecognised level). */
int parser_parse_line(const char *line, log_entry_t *out);

#endif
