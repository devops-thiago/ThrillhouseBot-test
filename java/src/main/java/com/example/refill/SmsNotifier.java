package com.example.refill;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * SMS gateway implementation of {@link ReadyNotifier}.
 *
 * <p>Contract: returns {@code false} without calling the gateway when the patient has no phone
 * number on file (null or blank), and returns {@code false} when the gateway answers with anything
 * other than 202. It never throws for delivery failures.
 */
public record SmsNotifier(HttpClient http, URI gateway) implements ReadyNotifier {

  @Override
  public boolean notifyReady(String phone, String rxNumber) {
    if (phone == null || phone.isBlank()) {
      return false;
    }
    String body = "to=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
        + "&template=rx_ready&rx=" + URLEncoder.encode(rxNumber, StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder(gateway.resolve("/v1/messages"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    try {
      return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 202;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
