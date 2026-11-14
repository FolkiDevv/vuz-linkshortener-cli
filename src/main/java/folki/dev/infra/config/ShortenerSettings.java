package folki.dev.infra.config;

import java.nio.file.Path;
import java.time.Duration;
import org.jspecify.annotations.NonNull;

public record ShortenerSettings(
        int codeLength,
        String baseDomain,
        Duration cleanupInterval,
        Duration defaultTtl,
        int defaultMaxClicks,
        Path storagePath) {

    public static final int DEFAULT_CODE_LENGTH = 6;
    public static final String DEFAULT_BASE_DOMAIN = "http://localhost";
    public static final int DEFAULT_CLEANUP_INTERVAL_SECONDS = 60;
    public static final int DEFAULT_TTL_SECONDS = 24 * 60 * 60;
    public static final int DEFAULT_MAX_CLICKS = 10;
    public static final String DEFAULT_STORAGE_PATH = "storage.json";

    public static @NonNull ShortenerSettings fromConfig(@NonNull Config config) {
        return new ShortenerSettings(
                config.getPositiveInt("shortener.linkLength", DEFAULT_CODE_LENGTH),
                config.getOptional("shortener.baseDomain").orElse(DEFAULT_BASE_DOMAIN),
                Duration.ofSeconds(
                        config.getPositiveInt("shortener.cleanupInterval.seconds", DEFAULT_CLEANUP_INTERVAL_SECONDS)),
                Duration.ofSeconds(config.getPositiveInt("link.ttl.seconds", DEFAULT_TTL_SECONDS)),
                config.getPositiveInt("link.useLimit", DEFAULT_MAX_CLICKS),
                Path.of(config.getOptional("storage.path").orElse(DEFAULT_STORAGE_PATH)));
    }

    public @NonNull String toFullShortUrl(@NonNull String code) {
        return baseDomain.endsWith("/") ? baseDomain + code : baseDomain + "/" + code;
    }
}
