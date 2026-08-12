#ifndef LINKC_STORE_H
#define LINKC_STORE_H

/* Opens (creating if necessary) the sqlite database at path and
 * ensures the urls table exists. Returns 0 on success. */
int store_open(const char *path);

/*
 * Persists a code -> url mapping.
 * Returns 0 on success, or -1 if the code already exists (the `code`
 * column has a UNIQUE constraint) or the write otherwise failed.
 */
int store_save_url(const char *code, const char *url);

void store_close(void);

#endif
