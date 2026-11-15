package folki.dev.infra.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShortenerSettingsTest {
    @Test
    void readsValuesFromConfig() {
        Config config = new PropertiesConfig()
                .loadConfig(Map.of(
                        "shortener.linkLength", "8",
                        "shortener.baseDomain", "https://s.test",
                        "shortener.cleanupInterval.seconds", "5",
                        "link.ttl.seconds", "180",
                        "link.useLimit", "3",
                        "storage.path", "data.json"));

        ShortenerSettings settings = ShortenerSettings.fromConfig(config);

        assertEquals(8, settings.codeLength());
        assertEquals("https://s.test", settings.baseDomain());
        assertEquals(Duration.ofSeconds(5), settings.cleanupInterval());
        assertEquals(Duration.ofMinutes(3), settings.defaultTtl());
        assertEquals(3, settings.defaultMaxClicks());
        assertEquals(Path.of("data.json"), settings.storagePath());
    }

    @Test
    void fallsBackToDefaultsForMissingOrInvalidValues() {
        Config config = new PropertiesConfig()
                .loadConfig(Map.of(
                        "shortener.linkLength", "abc",
                        "link.ttl.seconds", "-10",
                        "link.useLimit", "0",
                        "shortener.baseDomain", "  "));

        ShortenerSettings settings = ShortenerSettings.fromConfig(config);

        assertEquals(ShortenerSettings.DEFAULT_CODE_LENGTH, settings.codeLength());
        assertEquals(Duration.ofSeconds(ShortenerSettings.DEFAULT_TTL_SECONDS), settings.defaultTtl());
        assertEquals(ShortenerSettings.DEFAULT_MAX_CLICKS, settings.defaultMaxClicks());
        assertEquals(ShortenerSettings.DEFAULT_BASE_DOMAIN, settings.baseDomain());
        assertEquals(Path.of(ShortenerSettings.DEFAULT_STORAGE_PATH), settings.storagePath());
    }

    @Test
    void externalFileOverridesBundledConfig(@TempDir Path tempDir) throws Exception {
        Path external = tempDir.resolve("config.properties");
        Files.writeString(external, "link.useLimit=42\n");

        Config config = PropertiesConfig.load(PropertiesConfig.DEFAULT_RESOURCE, external);

        assertEquals(42, config.getInt("link.useLimit", 0));
        assertEquals(6, config.getInt("shortener.linkLength", 0));
    }

    @Test
    void buildsFullShortUrl() {
        ShortenerSettings settings = ShortenerSettings.fromConfig(
                new PropertiesConfig().loadConfig(Map.of("shortener.baseDomain", "https://s.test/")));

        assertEquals("https://s.test/aB3x9Z", settings.toFullShortUrl("aB3x9Z"));
    }
}
