package folki.dev.infra;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public final class LoggingConfig {
    public static final String DEFAULT_RESOURCE = "logging.properties";

    private LoggingConfig() {}

    public static void configure(String resourceName) {
        try (InputStream in = LoggingConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
            }
        } catch (IOException e) {
            System.err.println("Не удалось настроить логирование: " + e.getMessage());
        }
    }
}
