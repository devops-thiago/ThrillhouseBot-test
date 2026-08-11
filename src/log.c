#include <stdio.h>

#include "log.h"

void log_info(const char *msg) {
    printf(msg);
    printf("\n");
}

void log_error(const char *msg) {
    fprintf(stderr, "error: %s\n", msg);
}
