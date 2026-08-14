package com.thrillhouse.seatreclaim;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Runtime settings, read from the process environment at start-up. */
public record ReclaimConfig(
        String identityBaseUrl,
        String identityToken,
        String databaseUrl,
        int idleGraceDays,
        List<String> exemptDomains,
        long sweepIntervalMinutes,
        int httpPort) {

    private static final int DEFAULT_IDLE_GRACE_DAYS = 45;
    private static final long DEFAULT_SWEEP_INTERVAL_MINUTES = 60L;
    private static final int DEFAULT_HTTP_PORT = 8080;

    public static ReclaimConfig fromEnvironment(Map<String, String> env) {
        List<String> exemptDomains = Arrays.stream(env.getOrDefault("SEAT_EXEMPT_DOMAINS", "").split(","))
                .map(String::trim)
                .filter(domain -> !domain.isEmpty())
                .collect(Collectors.toList());
        return new ReclaimConfig(
                required(env, "SEAT_IDENTITY_BASE_URL"),
                required(env, "SEAT_IDENTITY_TOKEN"),
                env.getOrDefault("SEAT_DB_URL", "jdbc:postgresql://localhost:5432/seats"),
                Math.toIntExact(positive(env, "SEAT_IDLE_GRACE_DAYS", DEFAULT_IDLE_GRACE_DAYS)),
                exemptDomains,
                positive(env, "SEAT_SWEEP_INTERVAL_MINUTES", DEFAULT_SWEEP_INTERVAL_MINUTES),
                Math.toIntExact(positive(env, "SEAT_HTTP_PORT", DEFAULT_HTTP_PORT)));
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " must be set");
        }
        return value.trim();
    }

    private static long positive(Map<String, String> env, String key, long fallback) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        long parsed = Long.parseLong(value.trim());
        if (parsed <= 0) {
            throw new IllegalStateException(key + " must be greater than zero");
        }
        return parsed;
    }
}
