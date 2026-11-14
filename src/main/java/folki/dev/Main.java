package folki.dev;

import folki.dev.cli.CliApp;
import folki.dev.infra.config.Config;
import folki.dev.infra.config.PropertiesConfig;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;

import java.io.File;

public class Main {
    static void main() {
        Config config = new PropertiesConfig();
        File storageFile = new File("test.json");
        Storage storage = new FileStorage(storageFile);
        CliApp app = new CliApp(config, storage);
        app.mainCycle();
    }
}
