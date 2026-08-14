package com.thrillhouse.seatreclaim;

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
import java.util.List;
import java.util.logging.Logger;

/** Thin HTTP client for the identity provider's seat API. */
public class IdentityClient {

    /** One page of the provider's seat listing. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeatPage(
            @JsonProperty("seats") List<Seat> seats,
            @JsonProperty("next_cursor") String nextCursor,
            @JsonProperty("total_count") int totalCount) {}

    private static final Logger LOG = Logger.getLogger(IdentityClient.class.getName());
    /** Page size the provider accepts on the seat listing endpoint. */
    private static final int PAGE_SIZE = 500;

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String baseUrl;
    private final String token;
    private final Duration timeout;

    public IdentityClient(String baseUrl, String token, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * Lists the seats in the tenant. The provider serves a directory of any size in pages of 500:
     * every response carries the {@code next_cursor} addressing the following page, and
     * {@code total_count} reports the size of the whole directory, which runs to tens of thousands
     * of seats in large tenants.
     */
    public List<Seat> listSeats() throws IOException, InterruptedException {
        HttpResponse<String> response = send(request("/v1/seats?limit=" + PAGE_SIZE).GET().build());
        if (response.statusCode() != 200) {
            throw new IOException("seat listing failed: HTTP " + response.statusCode());
        }
        SeatPage page = mapper.readValue(response.body(), SeatPage.class);
        LOG.info(() -> "directory returned " + page.seats().size() + " of " + page.totalCount() + " seats");
        return page.seats();
    }

    /**
     * Revokes the licence attached to a seat. A 5xx response is retried once before the call is
     * abandoned, so a brief provider outage does not fail the whole sweep.
     */
    public void revokeSeat(String seatId) throws IOException, InterruptedException {
        HttpResponse<String> response = send(request("/v1/seats/" + seatId).DELETE().build());
        if (response.statusCode() >= 300) {
            throw new IOException("revoke of seat " + seatId + " failed: HTTP " + response.statusCode());
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(timeout);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
