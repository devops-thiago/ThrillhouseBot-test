#ifndef LINKC_DENYLIST_H
#define LINKC_DENYLIST_H

#define MAX_DENYLIST 2048

/* Fetches the moderation service's banned-domain list and populates
 * the in-memory denylist used by is_domain_banned(). Also merges in
 * any domains from the LINKC_EXTRA_DENYLIST env var. Returns 0 on
 * success, -1 if the moderation API could not be reached. */
int denylist_load(const char *api_url);

/* Returns 1 if domain is on the denylist, 0 otherwise. */
int is_domain_banned(const char *domain);

#endif
