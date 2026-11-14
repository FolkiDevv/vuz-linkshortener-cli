package folki.dev.infra.config;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface Config {
    Config loadConfig(Map<String, String> config);

    @Nullable
    String get(String key);

    Optional<String> getOptional(String key);

    int getInt(String key, int defaultValue);

    int getPositiveInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);
}
