package com.thrillhouse.devicerecon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Runtime settings, read from the process environment at start-up. */
public record ReconcilerConfig(
        String mdmBaseUrl,
        String mdmToken,
        String directoryBaseUrl,
        String directoryToken,
        String databaseUrl,
        int retirementGraceDays,
        List<String> exemptPlatforms,
        long intervalMinutes,
        int httpPort,
        boolean dryRun) {

    private static final int DEFAULT_GRACE_DAYS = 30;
    private static final String DEFAULT_EXEMPT_PLATFORMS = "shared-ipad,kiosk";
    private static final long DEFAULT_INTERVAL_MINUTES = 360L;
    private static final int DEFAULT_HTTP_PORT = 8080;

    public static ReconcilerConfig fromEnvironment(Map<String, String> env) {
        List<String> exemptPlatforms =
                Arrays.stream(env.getOrDefault("RECON_EXEMPT_PLATFORMS", DEFAULT_EXEMPT_PLATFORMS).split(","))
                        .map(String::trim)
                        .filter(platform -> !platform.isEmpty())
                        .toList();
        return new ReconcilerConfig(
                required(env, "RECON_MDM_BASE_URL"),
                required(env, "RECON_MDM_TOKEN"),
                required(env, "RECON_DIRECTORY_BASE_URL"),
                required(env, "RECON_DIRECTORY_TOKEN"),
                env.getOrDefault("RECON_DB_URL", "jdbc:postgresql://localhost:5432/devices"),
                Math.toIntExact(positive(env, "RECON_RETIREMENT_GRACE_DAYS", DEFAULT_GRACE_DAYS)),
                exemptPlatforms,
                positive(env, "RECON_INTERVAL_MINUTES", DEFAULT_INTERVAL_MINUTES),
                Math.toIntExact(positive(env, "RECON_HTTP_PORT", DEFAULT_HTTP_PORT)),
                Boolean.parseBoolean(env.getOrDefault("RECON_DRY_RUN", "false")));
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
