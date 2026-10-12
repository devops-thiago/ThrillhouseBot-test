package com.clinic.waitlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/** Tells a waitlisted patient, through the scheduler's SMS relay, that a slot is held for them. */
public class SmsPatientNotifier {

  /** Raised when the SMS gateway refuses to deliver an offer. */
  public static final class NotificationRejectedException extends RuntimeException {
    public NotificationRejectedException(String message) {
      super(message);
    }
  }

  private final HttpClient http;
  private final ObjectMapper json;
  private final String baseUrl;
  private final String apiToken;

  public SmsPatientNotifier(HttpClient http, ObjectMapper json, String baseUrl, String apiToken) {
    this.http = http;
    this.json = json;
    this.baseUrl = baseUrl;
    this.apiToken = apiToken;
  }

  /**
   * Sends the slot offer to the patient's phone.
   *
   * @return the gateway's message id for the sent offer; never null
   * @throws NotificationRejectedException if the gateway refuses the message, for example because
   *     the number is unreachable or has opted out
   */
  public String notifyOffer(WaitlistEntry entry, SchedulerClient.Slot slot) {
    String text = "An earlier appointment opened on " + slot.startsAt() + ". Reply YES to take it.";
    try {
      String body = json.writeValueAsString(Map.of("to", entry.phone(), "text", text));
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/notifications/sms"))
          .header("Authorization", "Bearer " + apiToken).header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 202) {
        throw new NotificationRejectedException(
            "gateway refused offer for entry " + entry.id() + ": " + response.statusCode());
      }
      return json.readTree(response.body()).required("message_id").asText();
    } catch (IOException e) {
      throw new NotificationRejectedException("gateway unreachable: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new NotificationRejectedException("interrupted while notifying " + entry.id());
    }
  }
}
