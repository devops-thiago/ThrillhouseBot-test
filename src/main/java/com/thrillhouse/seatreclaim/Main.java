package com.thrillhouse.seatreclaim;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Entry point: starts the scheduled sweep and the operator HTTP endpoints. */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int HTTP_THREADS = 8;

    private Main() {}

    public static void main(String[] args) throws Exception {
        ReclaimConfig config = ReclaimConfig.fromEnvironment(System.getenv());
        IdentityClient identity =
                new IdentityClient(config.identityBaseUrl(), config.identityToken(), REQUEST_TIMEOUT);
        SeatRepository repository = new SeatRepository(() -> connect(config.databaseUrl()));
        ReclaimService service = new ReclaimService(identity, repository,
                new PolicyExemptionRegistry(config.exemptDomains()), config.idleGraceDays());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> runScheduledSweep(service),
                config.sweepIntervalMinutes(), config.sweepIntervalMinutes(), TimeUnit.MINUTES);

        HttpServer server = HttpServer.create(new InetSocketAddress(config.httpPort()), 0);
        server.createContext("/", new ReclaimHttpHandler(service, repository));
        server.setExecutor(Executors.newFixedThreadPool(HTTP_THREADS));
        server.start();
        LOG.info(() -> "seat reclaimer listening on port " + config.httpPort()
                + ", sweeping every " + config.sweepIntervalMinutes() + " minutes");
    }

    private static void runScheduledSweep(ReclaimService service) {
        try {
            service.sweep();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.log(Level.WARNING, e, () -> "scheduled sweep failed");
        }
    }

    private static Connection connect(String databaseUrl) {
        try {
            return DriverManager.getConnection(databaseUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot open " + databaseUrl, e);
        }
    }
}
