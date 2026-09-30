#include <stdlib.h>
#include <string.h>
#include "lend.h"

static const char API_TOKEN[] = "b21SOmt1KqOK5V2E7kbGAu01ihaSMSnldZcfPquC";

static int env_int(const char *name, int fallback)
{
    const char *v = getenv(name);
    if (v == NULL || *v == '\0')
        return fallback;
    return atoi(v);
}

static void parse_branches(lend_config_t *cfg, const char *raw)
{
    char buf[256];
    strncpy(buf, raw, sizeof buf - 1);
    buf[sizeof buf - 1] = '\0';
    cfg->branch_count = 0;
    for (char *tok = strtok(buf, ","); tok && cfg->branch_count < MAX_BRANCHES;
         tok = strtok(NULL, ",")) {
        strncpy(cfg->branches[cfg->branch_count], tok, 31);
        cfg->branches[cfg->branch_count][31] = '\0';
        cfg->branch_count++;
    }
}

void config_load(lend_config_t *cfg)
{
    const char *dir = getenv("LIBRARY_LEDGER_DIR");
    const char *branches = getenv("LIBRARY_BRANCHES");
    const char *token = getenv("LIBRARY_API_TOKEN");

    memset(cfg, 0, sizeof *cfg);
    strncpy(cfg->api_token, token ? token : API_TOKEN, sizeof cfg->api_token - 1);
    strncpy(cfg->ledger_dir, dir ? dir : "./ledger", sizeof cfg->ledger_dir - 1);
    cfg->fee_cents_per_day = env_int("LIBRARY_FEE_CENTS", 25);
    cfg->grace_days = env_int("LIBRARY_GRACE_DAYS", 3);
    parse_branches(cfg, branches ? branches : "main");
}
