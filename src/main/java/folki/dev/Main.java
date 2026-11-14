package folki.dev;

import folki.dev.cli.CliApp;
import folki.dev.core.services.CodeGenerator;
import folki.dev.core.services.LinkService;
import folki.dev.core.services.RandomCodeGenerator;
import folki.dev.infra.config.Config;
import folki.dev.infra.config.PropertiesConfig;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;
import java.nio.file.Path;

public class Main {
    static void main() {
        Config config = PropertiesConfig.load(PropertiesConfig.DEFAULT_RESOURCE, Path.of("config.properties"));
        ShortenerSettings settings = ShortenerSettings.fromConfig(config);

        Storage storage = FileStorage.load(settings.storagePath());
        CodeGenerator codeGenerator = new RandomCodeGenerator(settings.codeLength());
        LinkService linkService = new LinkService(storage, codeGenerator, settings);
        CliApp app = new CliApp(settings, storage, linkService);
        app.mainCycle();
    }
}
