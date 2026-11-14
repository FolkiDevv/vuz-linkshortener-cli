package folki.dev.infra.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class BaseConfig implements Config {
    private static final Logger LOG = Logger.getLogger(BaseConfig.class.getName());

    protected Map<String, String> config = new HashMap<>();

    @Override
    public Optional<String> getOptional(String key) {
        return Optional.ofNullable(config.get(key)).filter(value -> !value.isBlank());
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Optional<String> value = getOptional(key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.get());
        } catch (NumberFormatException e) {
            LOG.warning("Некорректное значение " + key + "=" + value.get() + ", используется " + defaultValue);
            return defaultValue;
        }
    }

    @Override
    public int getPositiveInt(String key, int defaultValue) {
        int value = getInt(key, defaultValue);
        if (value <= 0) {
            LOG.warning("Значение " + key + " должно быть больше 0, используется " + defaultValue);
            return defaultValue;
        }
        return value;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return getOptional(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }
}
