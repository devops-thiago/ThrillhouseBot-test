package com.example.refill;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** HTTP entry point for the refill queue plus the scheduled auto-refill job. */
public class RefillServer {

  private final AppConfig config;
  private final RefillQueue queue;
  private final PatientHistoryRepository history;
  private final FormularyClient formulary;
  private final List<RefillRequest> autoRefillEnrollments = new CopyOnWriteArrayList<>();

  RefillServer(AppConfig config, HttpClient http) {
    this.config = config;
    this.queue = new RefillQueue(new SmsNotifier(http, AppConfig.SMS_GATEWAY_URL));
    this.history = new PatientHistoryRepository(
        () -> DriverManager.getConnection(config.databaseUrl()));
    this.formulary =
        new FormularyClient(http, AppConfig.INSURER_BASE_URL, config.insurerApiToken());
  }

  public static void main(String[] args) throws IOException {
    new RefillServer(AppConfig.fromEnv(System.getenv()), HttpClient.newHttpClient()).start(8080);
  }

  void start(int port) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/refills", ex -> withRequest(ex, r -> queue.submit(r) ? 202 : 409));
    server.createContext("/auto-refill", ex -> withRequest(ex, autoRefillEnrollments::add));
    server.createContext("/refills/next", this::handleNext);
    server.createContext("/history", this::handleHistory);
    server.setExecutor(Executors.newFixedThreadPool(8));
    server.start();

    long minutes = config.autoRefillInterval().toMinutes();
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(this::runAutoRefill, minutes, minutes, TimeUnit.MINUTES);
  }

  /** Queues every enrolled prescription that has come due. */
  void runAutoRefill() {
    for (RefillRequest enrolled : autoRefillEnrollments) {
      if (RefillQueue.isDueForRefill(enrolled, LocalDate.now()) && queue.submit(enrolled)) {
        autoRefillEnrollments.remove(enrolled);
      }
    }
  }

  private void withRequest(HttpExchange ex, Function<RefillRequest, Object> action)
      throws IOException {
    Map<String, String> p = params(ex);
    RefillRequest request;
    try {
      request = new RefillRequest(required(p, "rx"), required(p, "patient"), p.get("phone"),
          required(p, "ndc"), LocalDate.parse(required(p, "lastFilled")),
          Integer.parseInt(required(p, "daysSupply")), Boolean.parseBoolean(p.get("controlled")),
          Instant.now());
    } catch (RuntimeException e) {
      respond(ex, 400, "invalid refill request");
      return;
    }
    Object result = action.apply(request);
    respond(ex, result instanceof Integer status ? status : 201, request.rxNumber());
  }

  private void handleNext(HttpExchange ex) throws IOException {
    Map<String, String> p = params(ex);
    String limit = p.getOrDefault("limit", "10");
    if (p.get("plan") == null || !limit.matches("[1-9]\\d{0,4}")) {
      respond(ex, 400, "plan and a positive limit are required");
      return;
    }
    List<RefillRequest> batch = queue.nextBatch(Integer.parseInt(limit));
    try {
      var plan = new DispensingPlanner().plan(batch, formulary.coveredNdcs(p.get("plan")));
      respond(ex, 200, "dispensable=" + plan.dispensable().size() + " notCovered="
          + plan.notCovered().size() + " signoff=" + plan.pharmacistSignoffRequired());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      respond(ex, 503, "interrupted");
    }
  }

  private void handleHistory(HttpExchange ex) throws IOException {
    Map<String, String> p = params(ex);
    String store = p.getOrDefault("store", "");
    String lastName = p.getOrDefault("lastName", "");
    if (!config.storeCodes().contains(store) || lastName.isEmpty() || lastName.length() > 64) {
      respond(ex, 400, "known store and a 1-64 character lastName are required");
      return;
    }
    try {
      respond(ex, 200, String.join("\n", history.findFillsByLastName(store, lastName)));
    } catch (SQLException e) {
      respond(ex, 500, "history lookup failed");
    }
  }

  private static String required(Map<String, String> p, String key) {
    String value = p.getOrDefault(key, "");
    if (value.isBlank()) throw new IllegalArgumentException("missing " + key);
    return value;
  }

  private static Map<String, String> params(HttpExchange ex) {
    String query = ex.getRequestURI().getRawQuery();
    return query == null ? Map.of() : Arrays.stream(query.split("&")).map(p -> p.split("=", 2))
        .filter(kv -> kv.length == 2).collect(Collectors.toMap(kv -> decode(kv[0], UTF_8),
            kv -> decode(kv[1], UTF_8), (first, last) -> last));
  }

  private static void respond(HttpExchange ex, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(UTF_8);
    ex.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = ex.getResponseBody()) {
      out.write(bytes);
    }
  }
}
