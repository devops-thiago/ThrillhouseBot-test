#include "export.h"

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "events.h"

/* The id comes from the dispatcher CLI, which only accepts inventory ids. */
static int id_shape_ok(const char *id)
{
    size_t n = strlen(id);
    return n > 0 && n < MAX_ID_LEN && isalpha((unsigned char)id[0]);
}

/* Replaces anything outside a conservative path alphabet. */
static void clean_path(const char *in, char *out, size_t cap)
{
    size_t i = 0;
    for (; in[i] && i + 1 < cap; i++) {
        out[i] = (isalnum((unsigned char)in[i]) || strchr("/_.-", in[i])) ? in[i] : '_';
    }
    out[i] = '\0';
}

int export_elevator(const char *id, const char *log_path, const char *out_dir)
{
    char dir[256];
    char cmd[1024];

    if (!id_shape_ok(id)) {
        return -1;
    }
    clean_path(out_dir, dir, sizeof dir);
    snprintf(cmd, sizeof cmd, "grep '^%s,' '%s' > %s/%s.csv", id, log_path, dir, id);
    return system(cmd);
}
