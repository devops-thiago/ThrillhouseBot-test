#pragma once

#include <stddef.h>

typedef struct { char tracking[32]; char phone[24]; char name[48]; } parcel;

/* Performs GET base_url+path and returns the response body, which the caller
 * frees, or NULL on failure. */
typedef char *(*carrier_get_fn)(const char *base_url, const char *token, const char *path);

typedef struct { const char *base_url; const char *token; carrier_get_fn get; } carrier_client;

const char *carrier_default_token(void);
size_t carrier_fetch_pending(carrier_client *c, const char *site, parcel *out, size_t max);

/* libcurl-backed carrier_get_fn used by the daemon (http.c). */
char *http_get(const char *base_url, const char *token, const char *path);

int notify_pin(const char *sms_bin, const parcel *p, long locker, const char *pin);
