package com.thrillhouse.devicerecon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Thin HTTP client for the MDM's device API. */
public class MdmClient {

    /** One page of the device listing. {@code nextCursor} is null on the last page. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DevicePage(
            @JsonProperty("devices") List<EnrolledDevice> devices,
            @JsonProperty("next_cursor") String nextCursor) {}

    private static final Logger LOG = Logger.getLogger(MdmClient.class.getName());
    /** Largest page the MDM will serve; asking for more is answered with a 400. */
    private static final int PAGE_SIZE = 200;

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String baseUrl;
    private final String token;
    private final Duration timeout;

    public MdmClient(String baseUrl, String token, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /**
     * Every device the MDM currently has enrolled, following the listing's cursor to the end. A
     * mid-size fleet is a few thousand devices, which is small enough to hold in memory and compare
     * against the directory in one pass.
     */
    public List<EnrolledDevice> listEnrolledDevices() throws IOException, InterruptedException {
        List<EnrolledDevice> devices = new ArrayList<>();
        String cursor = null;
        do {
            String path = "/v1/devices?limit=" + PAGE_SIZE
                    + (cursor == null ? "" : "&cursor=" + URLEncoder.encode(cursor, StandardCharsets.UTF_8));
            HttpResponse<String> response = send(request(path).GET().build());
            if (response.statusCode() != 200) {
                throw new IOException("device listing failed: HTTP " + response.statusCode());
            }
            DevicePage page = mapper.readValue(response.body(), DevicePage.class);
            devices.addAll(page.devices());
            cursor = page.nextCursor();
        } while (cursor != null);
        LOG.info(() -> "MDM reported " + devices.size() + " enrolled devices");
        return devices;
    }

    /**
     * Retires a device: the MDM wipes the corporate profile at the next check-in and stops counting
     * the device against the licence pool. The reason is written to the MDM's audit log, which is
     * what the compliance team reads when a retirement is queried.
     */
    public void retireDevice(String deviceId, String reason) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(new RetireRequest(reason));
        HttpResponse<String> response = send(request("/v1/devices/" + deviceId + "/retire")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
        if (response.statusCode() >= 300) {
            throw new IOException("retire of device " + deviceId + " failed: HTTP " + response.statusCode());
        }
    }

    private record RetireRequest(@JsonProperty("reason") String reason) {}

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
