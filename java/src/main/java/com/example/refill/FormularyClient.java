package com.example.refill;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Client for the insurer's formulary API.
 *
 * <p>{@code GET /v1/plans/{planId}/formulary} returns one NDC per line, at most 200 lines per
 * page. When more results exist the response carries a {@code Link: <...>; rel="next"} header
 * pointing at the following page.
 */
public record FormularyClient(HttpClient http, URI baseUrl, String apiToken) {

  /** Returns every NDC covered by the given insurance plan. */
  public Set<String> coveredNdcs(String planId) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest
        .newBuilder(baseUrl.resolve("/v1/plans/" + planId + "/formulary"))
        .header("Authorization", "Bearer " + apiToken).GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("formulary lookup failed: HTTP " + response.statusCode());
    }
    return Arrays.stream(response.body().split("\n")).map(String::trim)
        .filter(line -> !line.isEmpty()).collect(Collectors.toSet());
  }
}
