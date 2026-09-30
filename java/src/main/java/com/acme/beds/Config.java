package com.acme.beds;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Runtime settings read from environment variables. */
public record Config(List<String> wards, Duration holdTimeout, int pageSize, String dbUrl) {

    public static Config fromEnv(Map<String, String> env) {
        String rawWards = env.getOrDefault("BED_WARDS", "");
        List<String> wards = rawWards.isBlank()
                ? List.of()
                : Arrays.stream(rawWards.split(",")).map(String::trim).toList();
        Duration hold = Duration.ofMinutes(Long.parseLong(env.getOrDefault("BED_HOLD_TIMEOUT", "30")));
        int pageSize = Integer.parseInt(env.getOrDefault("BED_PAGE_SIZE", "50"));
        String dbUrl = env.getOrDefault("BED_DB_URL", "jdbc:postgresql://localhost:5432/beds");
        return new Config(wards, hold, pageSize, dbUrl);
    }
}
