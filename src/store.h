#ifndef LINKC_STORE_H
#define LINKC_STORE_H

/* Opens (creating if necessary) the sqlite database at path and
 * ensures the urls table exists. Returns 0 on success. */
int store_open(const char *path);

/* Persists a code -> url mapping. Returns 0 on success, -1 if the
 * write failed. Callers are expected to have already checked
 * store_code_exists() for custom codes. */
int store_save_url(const char *code, const char *url);

/* Returns 1 if code is already present in the store, 0 if it is
 * not, or -1 if the check itself could not be completed. */
int store_code_exists(const char *code);

void store_close(void);

#endif
