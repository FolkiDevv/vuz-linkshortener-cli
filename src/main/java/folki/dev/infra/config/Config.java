package folki.dev.infra.config;

import java.util.Map;
import java.util.Optional;

public interface Config {
    Config loadConfig(Map<String, String> config);

    String get(String key);

    Optional<String> getOptional(String key);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);
}
