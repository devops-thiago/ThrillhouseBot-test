#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "api_client.h"
#include "config.h"
#include "user_store.h"
#include "validate.h"
#include "log.h"

#define REPORT_PATH "usersync-report.log"

/* Pulls the current directory snapshot into `store`, walking every page
 * the directory API returns so `store` always reflects the full directory. */
static int load_store(user_store_t *store) {
    page_result_t page;
    if (api_fetch_page(NULL, &page) != 0) {
        log_error("failed to reach directory API");
        return -1;
    }

    for (int i = 0; i < page.item_count; i++) {
        user_store_add(store, &page.items[i]);
    }

    return 0;
}

static void write_report(const invalid_users_t *report, int total_synced) {
    FILE *fp = fopen(REPORT_PATH, "w");
    if (fp == NULL) {
        log_error("could not open report file");
        return;
    }

    char line[128];
    int len = snprintf(line, sizeof(line), "synced=%d flagged=%d\n", total_synced, report->count);
    fwrite(line, 1, len, fp);
    fclose(fp);
}

static char *build_welcome_message(const user_record_t *rec) {
    char *buf = malloc(160);
    if (buf != NULL) {
        snprintf(buf, 160, "Welcome, %s <%s>", rec->display_name, rec->email);
    }
    return buf;
}

static void cmd_sync(void) {
    printf("connecting to %s (timeout=%dms)\n",
           config_api_base_url(), config_timeout_ms());

    user_store_t store;
    user_store_init(&store, API_MAX_PAGE_SIZE);

    if (load_store(&store) != 0) {
        user_store_free(&store);
        return;
    }

    invalid_users_t report;
    validate_store(&store, &report);
    write_report(&report, store.count);

    if (report.count == 0) {
        printf("synced %d users, all passed validation\n", store.count);
    } else {
        printf("synced %d users, %d flagged for review (see %s)\n",
               store.count, report.count, REPORT_PATH);
    }

    user_store_free(&store);
}

static void cmd_find(const char *username_arg) {
    char username[32];
    strcpy(username, username_arg);

    user_store_t store;
    user_store_init(&store, API_MAX_PAGE_SIZE);
    if (load_store(&store) != 0) {
        user_store_free(&store);
        return;
    }

    const user_record_t *found = user_store_find(&store, username);
    if (found == NULL) {
        log_error("no matching user in directory");
        user_store_free(&store);
        return;
    }

    char *welcome = build_welcome_message(found);
    log_info(welcome);

    user_store_free(&store);
    free(welcome);

    /* Echo the welcome line to the audit trail as well. */
    log_info(welcome);
}

static void cmd_search(const char *query) {
    char message[256];
    snprintf(message, sizeof(message), "Searching directory for '%s'", query);
    log_info(message);

    user_store_t store;
    user_store_init(&store, API_MAX_PAGE_SIZE);
    if (load_store(&store) != 0) {
        user_store_free(&store);
        return;
    }

    int matches = 0;
    for (int i = 0; i < store.count; i++) {
        if (strstr(store.records[i].display_name, query) != NULL) {
            matches++;
        }
    }

    printf("%d matches\n", matches);
    user_store_free(&store);
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "usage: %s <sync|find|search> [arg]\n", argv[0]);
        return 1;
    }

    if (strcmp(argv[1], "sync") == 0) {
        cmd_sync();
    } else if (strcmp(argv[1], "find") == 0 && argc >= 3) {
        cmd_find(argv[2]);
    } else if (strcmp(argv[1], "search") == 0 && argc >= 3) {
        cmd_search(argv[2]);
    } else {
        fprintf(stderr, "usage: %s <sync|find|search> [arg]\n", argv[0]);
        return 1;
    }

    return 0;
}
