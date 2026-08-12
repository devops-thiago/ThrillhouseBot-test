#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "parser.h"

static log_level_t parse_level(const char *tok) {
    if (strcmp(tok, "INFO") == 0) return LOG_LEVEL_INFO;
    if (strcmp(tok, "WARN") == 0) return LOG_LEVEL_WARN;
    if (strcmp(tok, "ERROR") == 0) return LOG_LEVEL_ERROR;
    return LOG_LEVEL_UNKNOWN;
}

int parser_parse_line(const char *line, log_entry_t *out) {
    if (line == NULL || out == NULL) {
        return -1;
    }

    char buf[256];
    strncpy(buf, line, sizeof(buf) - 1);
    buf[sizeof(buf) - 1] = '\0';

    char *level_tok = strtok(buf, "|");
    char *source_tok = strtok(NULL, "|");
    char *message_tok = strtok(NULL, "");

    if (level_tok == NULL || source_tok == NULL || message_tok == NULL) {
        return -1;
    }

    out->level = parse_level(level_tok);
    if (out->level == LOG_LEVEL_UNKNOWN) {
        return -1;
    }

    strncpy(out->source, source_tok, PARSER_MAX_SOURCE - 1);
    out->source[PARSER_MAX_SOURCE - 1] = '\0';

    /* Copy the message, clamping to the buffer capacity. */
    size_t len = strlen(message_tok);
    if (len > PARSER_MAX_MESSAGE) {
        len = PARSER_MAX_MESSAGE;
    }
    memcpy(out->message, message_tok, len);
    out->message[len] = '\0';

    return 0;
}
