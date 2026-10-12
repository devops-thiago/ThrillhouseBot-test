package com.clinic.waitlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Reads open appointment slots from the central scheduling API. */
public record SchedulerClient(HttpClient http, ObjectMapper json, String baseUrl, String apiToken) {

  /** An appointment slot that opened up after a cancellation. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Slot(String id, @JsonProperty("clinic_id") String clinicId,
      @JsonProperty("appointment_type") String appointmentType,
      @JsonProperty("starts_at") Instant startsAt) {}

  /** One page of {@code GET /clinics/{id}/open-slots}; {@code next_page} is null on the last page. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SlotPage(List<Slot> slots, @JsonProperty("next_page") Integer nextPage) {}

  /** Returns every open slot for the clinic. */
  public List<Slot> fetchOpenSlots(String clinicId) throws IOException, InterruptedException {
    String clinic = URLEncoder.encode(clinicId, StandardCharsets.UTF_8);
    URI uri = URI.create(baseUrl + "/clinics/" + clinic + "/open-slots?page=1&page_size=50");
    HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
        .header("Authorization", "Bearer " + apiToken).GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("scheduler returned " + response.statusCode() + " for " + clinicId);
    }
    return json.readValue(response.body(), SlotPage.class).slots();
  }
}
