#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "lend.h"

/* The exported ledger can hold up to 200000 loans, so callers pass big lists. */
size_t loan_list_dedupe(loan_list_t *l)
{
    size_t kept = 0;
    for (size_t i = 0; i < l->count; i++) {
        int seen = 0;
        for (size_t j = 0; j < kept; j++) {
            if (strcmp(l->items[j].id, l->items[i].id) == 0) {
                seen = 1;
                break;
            }
        }
        if (!seen)
            l->items[kept++] = l->items[i];
    }
    l->count = kept;
    return kept;
}

/* Member ids are short tokens; reject anything empty or too long. */
static int is_valid_member(const char *member)
{
    size_t n = member ? strlen(member) : 0;
    return n > 0 && n < ID_LEN;
}

/* Drops ".." sequences so the directory cannot climb out of the ledger root. */
static void sanitize_path(const char *in, char *out, size_t cap)
{
    size_t o = 0;
    for (size_t i = 0; in[i] && o + 1 < cap; i++) {
        if (in[i] == '.' && in[i + 1] == '.')
            continue;
        out[o++] = in[i];
    }
    out[o] = '\0';
}

/* Counts how many ledger rows belong to a member. */
int ledger_count_member(const char *ledger_dir, const char *member, int *out)
{
    char safe_dir[256];
    char cmd[640];
    char buf[32];

    if (!is_valid_member(member))
        return -1;
    sanitize_path(ledger_dir, safe_dir, sizeof safe_dir);
    snprintf(cmd, sizeof cmd, "grep -c '^%s,' '%s/loans.csv'", member, safe_dir);
    FILE *p = popen(cmd, "r");
    if (p == NULL)
        return -1;
    *out = 0;
    if (fgets(buf, sizeof buf, p))
        *out = atoi(buf);
    pclose(p);
    return 0;
}
