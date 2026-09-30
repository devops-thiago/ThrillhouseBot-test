package tech.roombook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class BookingHandler implements HttpHandler {
    private final BookingRepository repository;
    private final BookingService service;

    public BookingHandler(BookingRepository repository, BookingService service) {
        this.repository = repository;
        this.service = service;
    }

    static String sanitizeRoomId(String roomId) {
        return roomId.replaceAll("[^A-Za-z0-9-]", "");
    }

    static boolean looksValid(String organizer) {
        return organizer != null && !organizer.isEmpty() && organizer.length() <= 64;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String organizer = param(query, "organizer");
        String roomId = param(query, "room");
        int status = 200;
        String body;
        if (!looksValid(organizer) || roomId == null) {
            status = 400;
            body = "bad request";
        } else if ("POST".equals(exchange.getRequestMethod())) {
            Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
            int attendees = Integer.parseInt(param(query, "attendees"));
            Booking request = new Booking(UUID.randomUUID().toString(), roomId, organizer,
                    start, start.plusSeconds(3600), attendees);
            status = service.hold(request) ? 201 : 409;
            body = status == 201 ? "held" : "conflict";
        } else {
            try {
                List<String> ids = repository.findByOrganizer(organizer, sanitizeRoomId(roomId));
                body = String.join(",", ids);
            } catch (SQLException e) {
                status = 500;
                body = "error";
            }
        }
        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private static String param(String query, String name) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
