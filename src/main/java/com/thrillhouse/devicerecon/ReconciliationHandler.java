package com.thrillhouse.devicerecon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.thrillhouse.devicerecon.ReconciliationService.ReconciliationReport;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Operator endpoints: look up a person's devices, run a reconciliation, read the last one. */
public class ReconciliationHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(ReconciliationHandler.class.getName());
    /** Longest address the directory accepts, so anything longer cannot be a real owner. */
    private static final int MAX_EMAIL_LENGTH = 254;

    private final ReconciliationService service;
    private final DeviceInventory inventory;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ReconciliationHandler(ReconciliationService service, DeviceInventory inventory) {
        this.service = service;
        this.inventory = inventory;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/devices".equals(path)) {
                listDevices(exchange);
            } else if ("POST".equals(method) && "/reconciliations".equals(path)) {
                respond(exchange, 200, service.reconcile());
            } else if ("GET".equals(method) && "/reconciliations/latest".equals(path)) {
                latestReport(exchange);
            } else {
                respond(exchange, 404, Map.of("error", "no route for " + method + " " + path));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            respond(exchange, 503, Map.of("error", "reconciliation interrupted"));
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> method + " " + path + " failed");
            respond(exchange, 500, Map.of("error", "internal error"));
        }
    }

    private void listDevices(HttpExchange exchange) throws Exception {
        String owner = query(exchange.getRequestURI().getRawQuery())
                .getOrDefault("owner", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (owner.isEmpty() || owner.length() > MAX_EMAIL_LENGTH) {
            respond(exchange, 400, Map.of("error", "owner is required and must be an email address"));
            return;
        }
        respond(exchange, 200, Map.of("owner", owner, "devices", inventory.findByOwner(owner)));
    }

    private void latestReport(HttpExchange exchange) throws IOException {
        ReconciliationReport report = service.lastReport();
        if (report == null) {
            respond(exchange, 404, Map.of("error", "no reconciliation has completed yet"));
            return;
        }
        respond(exchange, 200, report);
    }

    private void respond(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private static Map<String, String> query(String rawQuery) {
        Map<String, String> parsed = new HashMap<>();
        if (rawQuery == null) {
            return parsed;
        }
        for (String pair : rawQuery.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                parsed.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
        }
        return parsed;
    }
}
