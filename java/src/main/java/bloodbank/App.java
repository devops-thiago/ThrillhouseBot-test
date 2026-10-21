package bloodbank;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Wires the HTTP endpoint and the periodic expiry sweep. */
public final class App {

    private App() { }

    public static void main(String[] args) throws IOException {
        Config config = new Config(System.getenv());
        ExpiryTracker tracker = new ExpiryTracker(Clock.systemUTC());
        AlertService alerts = new AlertService(msg -> System.out.println("ALERT " + msg));

        HttpServer server = HttpServer.create(new InetSocketAddress(config.port), 0);
        // Requests are served by a pool of 8 threads, so register() runs concurrently.
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/units", exchange -> {
            String[] parts = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).split(",");
            BloodUnit unit = new BloodUnit(parts[0], parts[1], parts[2], parts[3],
                    Instant.parse(parts[4]), Instant.parse(parts[5]));
            boolean added = tracker.register(unit);
            byte[] body = (added ? "created" : "duplicate").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(added ? 201 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        // The sweep runs every minute on its own thread while handlers keep registering units.
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> alerts.notifyExpired(tracker.sweep()), 1, 1, TimeUnit.MINUTES);

        server.start();
    }
}
