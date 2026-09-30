package tech.roombook;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnv();
        RoomDirectory directory = page -> new RoomDirectory.Page(List.of(new Room("A-101", 8)), page, false);
        Notifier notifier = (address, message) -> "receipt-" + address.hashCode();
        BookingService service = new BookingService(new RoomCatalog(directory), notifier, config);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/bookings", new BookingHandler(new BookingRepository(null), service));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor();
        sweeper.scheduleAtFixedRate(() -> service.sweepExpired(Instant.now()), 30, 30, TimeUnit.SECONDS);
    }
}
