package folki.dev.infra.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PropertiesConfig extends BaseConfig {
    private static final Logger LOG = Logger.getLogger(PropertiesConfig.class.getName());

    public static final String DEFAULT_RESOURCE = "config.properties";

    public static @NonNull PropertiesConfig load(@NonNull String resourceName, @NonNull Path externalFile) {
        Properties properties = new Properties();

        try (InputStream in = PropertiesConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } else {
                LOG.warning("Файл конфигурации " + resourceName
                        + " не найден в resources, используются значения по умолчанию");
            }
        } catch (IOException e) {
            LOG.warning("Не удалось прочитать " + resourceName + " из resources: " + e);
        }

        if (Files.isRegularFile(externalFile)) {
            try (Reader reader = Files.newBufferedReader(externalFile, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                LOG.warning("Не удалось прочитать " + externalFile + ": " + e);
            }
        }

        Map<String, String> values = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            values.put(key, properties.getProperty(key).strip());
        }

        PropertiesConfig config = new PropertiesConfig();
        config.config = values;
        return config;
    }

    @Override
    public Config loadConfig(Map<String, String> config) {
        PropertiesConfig configProperties = new PropertiesConfig();
        configProperties.config = config;

        return configProperties;
    }

    @Override
    public @Nullable String get(String key) {
        return config.get(key);
    }
}
