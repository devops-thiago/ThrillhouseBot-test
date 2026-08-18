package com.thrillhouse.devicerecon;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Entry point: starts the scheduled reconciliation and the operator endpoints. */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int HTTP_THREADS = 4;

    private Main() {}

    public static void main(String[] args) throws Exception {
        ReconcilerConfig config = ReconcilerConfig.fromEnvironment(System.getenv());
        DeviceInventory inventory = new DeviceInventory(() -> connect(config.databaseUrl()));
        ReconciliationService service = new ReconciliationService(
                new MdmClient(config.mdmBaseUrl(), config.mdmToken(), REQUEST_TIMEOUT),
                new DirectoryClient(config.directoryBaseUrl(), config.directoryToken(), REQUEST_TIMEOUT),
                inventory,
                new RetirementPolicy(config.exemptPlatforms(), config.retirementGraceDays()),
                Clock.systemUTC(),
                config.dryRun());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> runScheduled(service),
                config.intervalMinutes(), config.intervalMinutes(), TimeUnit.MINUTES);

        HttpServer server = HttpServer.create(new InetSocketAddress(config.httpPort()), 0);
        server.createContext("/", new ReconciliationHandler(service, inventory));
        server.setExecutor(Executors.newFixedThreadPool(HTTP_THREADS));
        server.start();

        LOG.info(() -> "device reconciler listening on " + config.httpPort()
                + ", reconciling every " + config.intervalMinutes() + " minutes"
                + (config.dryRun() ? " (dry run: nothing will be retired)" : ""));
    }

    private static void runScheduled(ReconciliationService service) {
        try {
            service.reconcile();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Letting this escape would cancel the schedule until the next deploy.
            LOG.log(Level.WARNING, e, () -> "scheduled reconciliation failed");
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
