package folki.dev.infra.config;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class PropertiesConfig extends BaseConfig {

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
