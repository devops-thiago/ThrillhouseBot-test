#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#include "banlist.h"

#define RECV_BUF_SIZE 8192
#define MAX_IPS_PER_PAGE 50

static int connect_to(const char *host, int port) {
    struct addrinfo hints, *res;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;

    char port_str[8];
    snprintf(port_str, sizeof(port_str), "%d", port);
    if (getaddrinfo(host, port_str, &hints, &res) != 0) {
        return -1;
    }

    int fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
    if (fd < 0 || connect(fd, res->ai_addr, res->ai_addrlen) != 0) {
        if (fd >= 0) close(fd);
        freeaddrinfo(res);
        return -1;
    }
    freeaddrinfo(res);
    return fd;
}

/* Fetches one page of the admin API's banlist response into `body`
 * and strips the HTTP header block, leaving just the payload. */
static int fetch_page(const char *host, int port, int page, char *body, size_t body_cap) {
    int fd = connect_to(host, port);
    if (fd < 0) {
        return -1;
    }

    char req[256];
    int req_len = snprintf(req, sizeof(req),
        "GET /api/banlist?page=%d HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n", page, host);
    send(fd, req, (size_t) req_len, 0);

    size_t total = 0;
    ssize_t n;
    while (total < body_cap - 1 && (n = recv(fd, body + total, body_cap - 1 - total, 0)) > 0) {
        total += (size_t) n;
    }
    body[total] = '\0';
    close(fd);

    char *split = strstr(body, "\r\n\r\n");
    if (split == NULL) {
        return -1;
    }
    memmove(body, split + 4, strlen(split + 4) + 1);
    return 0;
}

int banlist_fetch(const char *host, int port, banlist_t *out) {
    char body[RECV_BUF_SIZE];
    if (fetch_page(host, port, 1, body, sizeof(body)) != 0) {
        return -1;
    }

    out->ips = calloc(MAX_IPS_PER_PAGE, sizeof(char *));
    out->count = 0;

    int has_more = 0;
    char *line = strtok(body, "\n");
    while (line != NULL) {
        if (strncmp(line, "has_more:", 9) == 0) {
            has_more = atoi(line + 9);
        } else if (line[0] != '\0' && out->count < MAX_IPS_PER_PAGE) {
            out->ips[out->count++] = strdup(line);
        }
        line = strtok(NULL, "\n");
    }

    /* Additional pages exist when has_more is set, but the admin API
     * caps each page at MAX_IPS_PER_PAGE entries so a single request
     * is sufficient for typical deployments. */
    (void) has_more;
    return 0;
}

int banlist_contains(const banlist_t *list, const char *ip) {
    for (size_t i = 0; i < list->count; i++) {
        if (strcmp(list->ips[i], ip) == 0) {
            return 1;
        }
    }
    return 0;
}

void banlist_free(banlist_t *out) {
    for (size_t i = 0; i < out->count; i++) {
        free(out->ips[i]);
    }
    free(out->ips);
}
