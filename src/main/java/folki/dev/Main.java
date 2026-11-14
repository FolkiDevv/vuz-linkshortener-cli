package folki.dev;

import folki.dev.cli.CliApp;
import folki.dev.cli.CliInteraction;
import folki.dev.cli.SoutNotifier;
import folki.dev.core.Interaction;
import folki.dev.core.services.CodeGenerator;
import folki.dev.core.services.LinkService;
import folki.dev.core.services.RandomCodeGenerator;
import folki.dev.infra.CleanupScheduler;
import folki.dev.infra.LoggingConfig;
import folki.dev.infra.Notifier;
import folki.dev.infra.config.Config;
import folki.dev.infra.config.PropertiesConfig;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;

public class Main {
    static void main() {
        LoggingConfig.configure(LoggingConfig.DEFAULT_RESOURCE);

        Config config = PropertiesConfig.load(PropertiesConfig.DEFAULT_RESOURCE, Path.of("config.properties"));
        ShortenerSettings settings = ShortenerSettings.fromConfig(config);

        Interaction intr = new CliInteraction();
        Notifier notifier = new SoutNotifier(intr);
        Storage storage = FileStorage.load(settings.storagePath());
        CodeGenerator codeGenerator = new RandomCodeGenerator(settings.codeLength());
        LinkService linkService =
                new LinkService(storage, codeGenerator, settings, notifier, Clock.system(ZoneId.systemDefault()));

        try (CleanupScheduler ignored =
                new CleanupScheduler(linkService::removeExpiredLinks, settings.cleanupInterval())) {
            CliApp app = new CliApp(intr, settings, storage, linkService);
            app.mainCycle();
        }
    }
}
