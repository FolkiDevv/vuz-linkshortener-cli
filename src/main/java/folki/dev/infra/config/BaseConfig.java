package folki.dev.infra.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseConfig implements Config {
    protected Map<String, String> config = new HashMap<>();

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
