package com.thrillhouse.devicerecon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Looks accounts up in the corporate directory. */
public class DirectoryClient {

    /** Response of the batch lookup. Addresses the directory does not know are simply absent. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LookupResponse(@JsonProperty("accounts") List<DirectoryAccount> accounts) {}

    /** Addresses per lookup call. The directory rejects a batch larger than this. */
    private static final int BATCH_SIZE = 100;

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String baseUrl;
    private final String token;
    private final Duration timeout;

    public DirectoryClient(String baseUrl, String token, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * Looks up every address in {@code emails}, keyed by the lowercase address. An address the
     * directory has never heard of is left out of the result rather than reported as departed:
     * "unknown" and "gone" are different situations and only one of them is safe to act on.
     */
    public Map<String, DirectoryAccount> lookup(Collection<String> emails) throws IOException, InterruptedException {
        Map<String, DirectoryAccount> accounts = new HashMap<>();
        List<String> batch = new ArrayList<>(BATCH_SIZE);
        for (String email : emails) {
            batch.add(email);
            if (batch.size() == BATCH_SIZE) {
                fetchBatch(batch, accounts);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            fetchBatch(batch, accounts);
        }
        return accounts;
    }

    private void fetchBatch(List<String> emails, Map<String, DirectoryAccount> into)
            throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(Map.of("emails", emails));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/accounts/lookup"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("directory lookup failed: HTTP " + response.statusCode());
        }
        for (DirectoryAccount account : mapper.readValue(response.body(), LookupResponse.class).accounts()) {
            into.put(account.key(), account);
        }
    }
}
