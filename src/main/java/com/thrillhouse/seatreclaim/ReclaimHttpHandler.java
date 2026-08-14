package com.thrillhouse.seatreclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.thrillhouse.seatreclaim.ReclaimService.SweepResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Operator endpoints: browse mirrored seats and trigger a sweep between scheduled runs. */
public class ReclaimHttpHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(ReclaimHttpHandler.class.getName());
    /** Longest department name the directory allows. */
    private static final int MAX_DEPARTMENT_LENGTH = 64;

    private final ReclaimService service;
    private final SeatRepository repository;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ReclaimHttpHandler(ReclaimService service, SeatRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            switch (path) {
                case "/seats" -> listSeats(exchange);
                case "/sweeps" -> runSweep(exchange);
                default -> respond(exchange, 404, Map.of("error", "unknown endpoint " + path));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "request to " + path + " failed");
            respond(exchange, 500, Map.of("error", "internal error"));
        }
    }

    private void listSeats(HttpExchange exchange) throws Exception {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String department = query.getOrDefault("department", "").trim();
        if (department.isEmpty() || department.length() > MAX_DEPARTMENT_LENGTH) {
            respond(exchange, 400, Map.of("error", "department is required"));
            return;
        }
        String sort = query.getOrDefault("sort", "last_active_at");
        respond(exchange, 200, repository.findByDepartment(department, sort));
    }

    private void runSweep(HttpExchange exchange) throws Exception {
        SweepResult result = service.sweep();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reclaimed", result.reclaimed());
        body.put("stale_mirror_rows", result.staleMirrorRows());
        body.put("health", result.revokeFailures().isEmpty() ? "ok" : "degraded");
        body.put("revoke_failures", result.revokeFailures());
        body.put("sweeps", service.sweepCount());
        body.put("reclaimed_by_department", service.reclaimedByDepartment());
        respond(exchange, 200, body);
    }

    private void respond(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> parsed = new HashMap<>();
        for (String pair : rawQuery == null ? new String[0] : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                parsed.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return parsed;
    }
}
