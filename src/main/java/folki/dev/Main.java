package folki.dev;

import folki.dev.cli.CliApp;
import folki.dev.infra.config.Config;
import folki.dev.infra.config.PropertiesConfig;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;

import java.nio.file.Path;

public class Main {
    static void main() {
        Config config = new PropertiesConfig();
        Path storageFile = Path.of("test.json");
        Storage storage = FileStorage.load(storageFile);
        CliApp app = new CliApp(config, storage);
        app.mainCycle();
    }
}
