package com.example.dnsdrift.client;

import com.example.dnsdrift.model.DnsRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Talks to the registrar's REST API to list the DNS records currently published for a
 * domain. Results are paginated at {@value #PAGE_SIZE} records per page.
 */
public class RegistrarClient implements DnsRecordSource {

    private static final int PAGE_SIZE = 100;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public RegistrarClient(HttpClient httpClient, String baseUrl, String apiKey) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /** Fetches a single page of records for {@code domain}, starting at {@code cursor}. */
    public RecordPage fetchPage(String domain, String cursor) throws IOException, InterruptedException {
        String url = baseUrl + "/v1/domains/" + domain + "/records?limit=" + PAGE_SIZE
                + (cursor != null ? "&cursor=" + cursor : "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("registrar API returned status " + response.statusCode());
        }
        return mapper.readValue(response.body(), RecordPage.class);
    }

    /**
     * Fetches every DNS record currently published for {@code domain}, walking all pages
     * until the registrar reports no more results.
     */
    @Override
    public List<DnsRecord> fetchAllRecords(String domain) throws IOException, InterruptedException {
        List<DnsRecord> all = new ArrayList<>();
        RecordPage page = fetchPage(domain, null);
        all.addAll(page.records());
        return all;
    }
}
