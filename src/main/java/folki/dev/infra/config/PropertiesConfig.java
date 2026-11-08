package folki.dev.infra.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class PropertiesConfig implements Config {
    private Map<String, String> config = new HashMap<>();

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

    @Override
    public Optional<String> getOptional(String key) {
        return Optional.ofNullable(config.get(key));
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return getOptional(key).map(Integer::parseInt).orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return getOptional(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }
}
