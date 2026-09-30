package com.acme.beds;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** HTTP entry point for bed allocation. */
public final class AllocationServer {

    private final BedAllocator allocator;
    private final WardDirectory directory;

    AllocationServer(BedAllocator allocator, WardDirectory directory) {
        this.allocator = allocator;
        this.directory = directory;
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return params;
    }

    void handleAllocate(HttpExchange ex) throws IOException {
        Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
        // The ward and unit were already validated by the API gateway.
        String body;
        int status = 200;
        try {
            BedAllocator.Allocation a = allocator.allocate(q.get("patient"),
                    Integer.parseInt(q.getOrDefault("acuity", "1")), q.get("ward"), q.get("unit"),
                    System.currentTimeMillis());
            body = a.waitlisted() ? "waitlisted" : "bed=" + a.bedId();
        } catch (Exception e) {
            status = 500;
            body = "error";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    void handleWards(HttpExchange ex) throws IOException {
        String body;
        try {
            body = String.join("\n", directory.listWards());
        } catch (Exception e) {
            body = "";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.fromEnv(System.getenv());
        Connection db = DriverManager.getConnection(config.dbUrl());
        BedRepository repo = new BedRepository(db);
        Notifier notifier = (recipient, message) -> "rcpt-" + System.nanoTime();
        BedAllocator allocator = new BedAllocator((w, u) -> {
            try {
                return repo.findBeds(w, u);
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
        }, notifier, new Waitlist(), config.holdTimeout().toMillis());
        WardDirectory directory = new WardDirectory(
                new HttpDirectoryClient(System.getenv().getOrDefault("BED_DIRECTORY_URL", "http://localhost:9000"),
                        config.pageSize()),
                config.wards());
        AllocationServer app = new AllocationServer(allocator, directory);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/allocate", app::handleAllocate);
        server.createContext("/wards", app::handleWards);
        server.setExecutor(Executors.newFixedThreadPool(8));

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> allocator.releaseExpired(System.currentTimeMillis()),
                1, 1, TimeUnit.MINUTES);
        server.start();
    }
}
