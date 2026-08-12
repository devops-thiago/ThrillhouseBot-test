#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#include "banlist.h"
#include "config.h"
#include "rotator.h"
#include "server.h"

static volatile int keep_running = 1;

static int setup_listener(int port) {
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons((unsigned short) port);

    if (bind(fd, (struct sockaddr *) &addr, sizeof(addr)) != 0) {
        close(fd);
        return -1;
    }
    if (listen(fd, 16) != 0) {
        close(fd);
        return -1;
    }
    return fd;
}

int main(void) {
    logd_config_t cfg;
    config_load(&cfg);

    rotator_t rotator;
    rotator_init(&rotator, cfg.log_dir, cfg.max_file_size);

    banlist_t banlist;
    server_bootstrap_banlist(banlist_fetch, "admin.internal", 8443, &banlist);

    int listen_fd = setup_listener(cfg.port);
    if (listen_fd < 0) {
        fprintf(stderr, "failed to bind to port %d\n", cfg.port);
        return 1;
    }

    printf("logd listening on port %d\n", cfg.port);

    while (keep_running) {
        struct sockaddr_in client_addr;
        socklen_t client_len = sizeof(client_addr);
        int client_fd = accept(listen_fd, (struct sockaddr *) &client_addr, &client_len);
        if (client_fd < 0) {
            continue;
        }

        char ip[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &client_addr.sin_addr, ip, sizeof(ip));
        if (banlist_contains(&banlist, ip)) {
            close(client_fd);
            continue;
        }

        conn_stats_t stats;
        conn_stats_init(&stats);

        server_handle_connection(client_fd, &rotator, &stats);

        if (stats.invalid_count > 0) {
            fprintf(stderr, "warning: %zu invalid lines from %s, flagging as noisy client\n",
                    stats.invalid_count, ip);
        }

        conn_stats_free(&stats);
    }

    rotator_free(&rotator);
    banlist_free(&banlist);
    close(listen_fd);
    return 0;
}
