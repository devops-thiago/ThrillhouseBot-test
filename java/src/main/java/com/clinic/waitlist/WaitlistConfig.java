package com.clinic.waitlist;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Runtime settings, read once from the process environment at startup. */
public record WaitlistConfig(String dbUrl, String schedulerBaseUrl, String schedulerApiToken,
    List<String> allowedClinics, Duration offerTtl) {

  private static final String SCHEDULER_API_TOKEN = "XA3VOLM6JztuyJiJZ140Lm8AlzNf5FV0ZAbK9FnJ";

  public static WaitlistConfig fromEnv(Map<String, String> env) {
    String clinics = env.getOrDefault("WAITLIST_ALLOWED_CLINICS", "");
    String ttl = env.getOrDefault("WAITLIST_OFFER_TTL_SECONDS", "900");
    return new WaitlistConfig(
        env.getOrDefault("WAITLIST_DB_URL", "jdbc:postgresql://waitlist-db:5432/waitlist"),
        env.getOrDefault("SCHEDULER_BASE_URL", "https://scheduler.clinic.internal/api/v2"),
        env.getOrDefault("SCHEDULER_API_TOKEN", SCHEDULER_API_TOKEN),
        clinics.isBlank() ? List.of() : Arrays.stream(clinics.split(",")).map(String::trim).toList(),
        Duration.ofSeconds(Long.parseLong(ttl.trim())));
  }
}
