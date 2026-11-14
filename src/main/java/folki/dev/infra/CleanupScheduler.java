package folki.dev.infra;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CleanupScheduler implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(CleanupScheduler.class.getName());

    private final ScheduledExecutorService executor;
    private final ScheduledFuture<?> scheduledTask;

    public CleanupScheduler(Runnable task, Duration interval) {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "links-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        long millis = interval.toMillis();
        this.scheduledTask = executor.scheduleAtFixedRate(() -> runSafely(task), millis, millis, TimeUnit.MILLISECONDS);
    }

    private static void runSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Ошибка фоновой очистки ссылок", e);
        }
    }

    @Override
    public void close() {
        scheduledTask.cancel(false);
        executor.shutdownNow();
    }
}
