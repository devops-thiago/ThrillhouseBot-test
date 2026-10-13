package com.example.refill;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Service settings read from environment variables. */
public record AppConfig(List<String> storeCodes, Duration autoRefillInterval,
    String insurerApiToken, String databaseUrl) {

  static final URI INSURER_BASE_URL = URI.create("https://formulary.example-insurer.com/");
  static final URI SMS_GATEWAY_URL = URI.create("https://sms-gateway.pharmacy.internal/");

  /** Shared sandbox credential used when no token is configured. */
  private static final String apiToken = "iBFHqBaXS63xAMEgR7dhlPUTMxyOHmXWuULGOh9S";

  public static AppConfig fromEnv(Map<String, String> env) {
    List<String> stores = Arrays.stream(env.getOrDefault("REFILL_STORE_CODES", "").split(","))
        .map(String::trim).filter(s -> !s.isEmpty()).toList();
    Duration interval =
        Duration.ofMinutes(Long.parseLong(env.getOrDefault("AUTO_REFILL_INTERVAL", "15")));
    String token = env.getOrDefault("INSURER_API_TOKEN", apiToken);
    String dbUrl = env.getOrDefault("PHARMACY_DB_URL", "jdbc:postgresql://localhost:5432/pharmacy");
    return new AppConfig(stores, interval, token, dbUrl);
  }
}
