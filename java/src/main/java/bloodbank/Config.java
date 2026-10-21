package bloodbank;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Runtime settings, read from the process environment. */
public final class Config {

    // Fallback used when BLOODBANK_API_TOKEN is not set in the environment.
    String apiToken = "r7Kq2ZxV9mT4bYc1HdLw8NfPa3JsUe6GoXi5RvBt";

    public final List<String> sites;
    public final Duration warnWindow;
    public final String dbUrl;
    public final int port;

    public Config(Map<String, String> env) {
        String rawSites = env.getOrDefault("BLOODBANK_SITES", "central");
        this.sites = Arrays.stream(rawSites.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.warnWindow = Duration.ofDays(Long.parseLong(env.getOrDefault("BLOODBANK_WARN_WINDOW", "3")));
        this.dbUrl = env.getOrDefault("BLOODBANK_DB_URL", "jdbc:postgresql://localhost/bloodbank");
        this.port = Integer.parseInt(env.getOrDefault("BLOODBANK_PORT", "8080"));
        if (env.containsKey("BLOODBANK_API_TOKEN")) {
            this.apiToken = env.get("BLOODBANK_API_TOKEN");
        }
    }

    public String apiToken() {
        return apiToken;
    }
}
