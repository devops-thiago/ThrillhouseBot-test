#include "caldrift.h"

#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

/* The archive is laid out as one directory per collection round, with a file per
 * field kit inside it, so the walk only has to go a couple of levels deep. The
 * limit is here to stop a symlinked directory turning the walk into a loop. */
#define CD_WALK_DEPTH_MAX 8

typedef struct {
    cd_record_t *items;
    size_t count, capacity;
} record_list_t;

static int list_push(record_list_t *list, const cd_record_t *record)
{
    if (list->count == list->capacity) {
        size_t capacity = list->capacity == 0 ? 256 : list->capacity * 2;
        cd_record_t *grown = realloc(list->items, capacity * sizeof *grown);
        if (grown == NULL)
            return -1;
        list->items = grown;
        list->capacity = capacity;
    }
    list->items[list->count++] = *record;
    return 0;
}

static int has_csv_suffix(const char *name)
{
    size_t len = strlen(name);
    return len > 4 && strcmp(name + len - 4, ".csv") == 0;
}

/* Reads one archive file. Blank lines, comment lines and the column header the
 * newer field kits write are skipped; a row that does not parse is reported and
 * the rest of the file is still read. */
static int read_file(const char *path, record_list_t *list)
{
    char line[CD_LINE_MAX];
    cd_record_t record;
    FILE *fp = fopen(path, "r");
    if (fp == NULL) {
        fprintf(stderr, "caldrift: %s could not be opened\n", path);
        return -1;
    }
    while (fgets(line, sizeof line, fp) != NULL) {
        if (line[0] == '\0' || line[0] == '\n' || line[0] == '#')
            continue;
        if (strncmp(line, "sensor_id", 9) == 0)
            continue;
        if (cd_record_parse(line, &record) != 0) {
            fprintf(stderr, "caldrift: %s: malformed row skipped\n", path);
            continue;
        }
        if (list_push(list, &record) != 0) {
            fclose(fp);
            return -1;
        }
    }
    fclose(fp);
    return 0;
}

static int walk_dir(const char *dir, int depth, record_list_t *list)
{
    char path[CD_LINE_MAX];
    struct dirent *entry;
    struct stat info;
    DIR *dp;
    int rc = 0;

    if (depth > CD_WALK_DEPTH_MAX) {
        fprintf(stderr, "caldrift: %s is nested deeper than %d levels, not descending\n", dir, CD_WALK_DEPTH_MAX);
        return 0;
    }
    dp = opendir(dir);
    if (dp == NULL) {
        fprintf(stderr, "caldrift: %s could not be opened\n", dir);
        return -1;
    }
    while (rc == 0 && (entry = readdir(dp)) != NULL) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
            continue;
        snprintf(path, sizeof path, "%s/%s", dir, entry->d_name);
        if (stat(path, &info) != 0) {
            fprintf(stderr, "caldrift: %s could not be stat'd\n", path);
            continue;
        }
        if (S_ISDIR(info.st_mode))
            rc = walk_dir(path, depth + 1, list);
        else if (S_ISREG(info.st_mode) && has_csv_suffix(entry->d_name))
            rc = read_file(path, list);
    }
    closedir(dp);
    return rc;
}

/* Walks the archive and hands back every record it holds. The caller owns the
 * array and frees it. */
int cd_archive_walk(const cd_config_t *cfg, cd_record_t **out, size_t *out_n)
{
    record_list_t list = { NULL, 0, 0 };

    if (walk_dir(cfg->archive_dir, 0, &list) != 0) {
        free(list.items);
        return -1;
    }
    *out = list.items;
    *out_n = list.count;
    return 0;
}
