package folki.dev;

import folki.dev.cli.CliApp;
import folki.dev.core.services.CodeGenerator;
import folki.dev.core.services.LinkService;
import folki.dev.core.services.RandomCodeGenerator;
import folki.dev.infra.config.Config;
import folki.dev.infra.config.PropertiesConfig;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;
import java.nio.file.Path;

public class Main {
    private static final int DEFAULT_CODE_LENGTH = 6;

    static void main() {
        Config config = new PropertiesConfig();
        Path storageFile = Path.of("test.json");
        Storage storage = FileStorage.load(storageFile);
        CodeGenerator codeGenerator =
                new RandomCodeGenerator(config.getInt("shortener.linkLength", DEFAULT_CODE_LENGTH));
        LinkService linkService = new LinkService(storage, codeGenerator, config);
        CliApp app = new CliApp(config, storage, linkService);
        app.mainCycle();
    }
}
