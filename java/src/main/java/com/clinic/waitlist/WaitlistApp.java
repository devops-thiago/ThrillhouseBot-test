package com.clinic.waitlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** HTTP front end plus the background sweep that offers freed slots. */
public record WaitlistApp(ObjectMapper json, JdbcWaitlistStore store, WaitlistService service) {

  public static void main(String[] args) throws IOException {
    WaitlistConfig config = WaitlistConfig.fromEnv(System.getenv());
    ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    HttpClient http = HttpClient.newHttpClient();
    JdbcWaitlistStore store = new JdbcWaitlistStore(config.dbUrl());
    WaitlistService service = new WaitlistService(store,
        new SchedulerClient(http, json, config.schedulerBaseUrl(), config.schedulerApiToken()),
        new SmsPatientNotifier(http, json, config.schedulerBaseUrl(), config.schedulerApiToken()),
        config.offerTtl());
    WaitlistApp app = new WaitlistApp(json, store, service);

    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/waitlist/search", app::handleSearch);
    server.createContext("/slots/offer", app::handleOffer);
    server.setExecutor(Executors.newFixedThreadPool(8));
    server.start();

    ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor();
    sweeper.scheduleAtFixedRate(() -> {
      service.expireOffers();
      for (String clinicId : config.allowedClinics()) {
        try {
          service.reconcile(clinicId);
        } catch (Exception e) {
          System.err.println("reconcile failed for " + clinicId + ": " + e.getMessage());
        }
      }
    }, 0, 60, TimeUnit.SECONDS);
  }

  void handleSearch(HttpExchange ex) throws IOException {
    try {
      if ("GET".equals(ex.getRequestMethod())) {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String clinic = q.getOrDefault("clinic", "");
        respond(ex, 200, store.searchByPatientName(clinic, q.getOrDefault("patient", "")));
      } else {
        respond(ex, 405, Map.of("error", "method not allowed"));
      }
    } catch (IllegalArgumentException e) {
      respond(ex, 400, Map.of("error", e.getMessage()));
    }
  }

  void handleOffer(HttpExchange ex) throws IOException {
    SchedulerClient.Slot slot = json.readValue(ex.getRequestBody(), SchedulerClient.Slot.class);
    Optional<WaitlistService.Offer> offer = service.offerSlot(slot);
    if (offer.isPresent()) {
      respond(ex, 200, offer.get());
    } else {
      respond(ex, 404, Map.of("error", "no eligible patient for slot " + slot.id()));
    }
  }

  private void respond(HttpExchange ex, int status, Object body) throws IOException {
    byte[] bytes = json.writeValueAsBytes(body);
    ex.getResponseHeaders().set("Content-Type", "application/json");
    ex.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = ex.getResponseBody()) {
      out.write(bytes);
    }
  }

  static Map<String, String> query(String raw) {
    Map<String, String> params = new HashMap<>();
    for (String pair : raw == null ? new String[0] : raw.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0) {
        params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
      }
    }
    return params;
  }
}
