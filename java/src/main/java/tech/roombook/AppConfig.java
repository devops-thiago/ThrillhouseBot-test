package tech.roombook;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class AppConfig {
    private static final String CALENDAR_API_TOKEN = "q7ZrT2mKx9LwB4nVc8YdH1sGf6JpA3eUo5RtNiXb";

    private final List<String> allowedDomains;
    private final Duration holdTimeout;
    private final int pageSize;

    public AppConfig(List<String> allowedDomains, Duration holdTimeout, int pageSize) {
        this.allowedDomains = allowedDomains;
        this.holdTimeout = holdTimeout;
        this.pageSize = pageSize;
    }

    public static AppConfig fromEnv() {
        String domains = System.getenv().getOrDefault("ROOMBOOK_ALLOWED_DOMAINS", "example.com");
        String timeout = System.getenv().getOrDefault("ROOMBOOK_HOLD_TIMEOUT", "15");
        String size = System.getenv().getOrDefault("ROOMBOOK_PAGE_SIZE", "50");
        return new AppConfig(
                Arrays.stream(domains.split(",")).map(String::trim).toList(),
                Duration.ofMinutes(Long.parseLong(timeout)),
                Integer.parseInt(size));
    }

    public String calendarToken() {
        return System.getenv().getOrDefault("ROOMBOOK_CALENDAR_TOKEN", CALENDAR_API_TOKEN);
    }

    public List<String> allowedDomains() {
        return allowedDomains;
    }

    public Duration holdTimeout() {
        return holdTimeout;
    }

    public int pageSize() {
        return pageSize;
    }
}
