package folki.dev;

import folki.dev.core.entities.User;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    static void main() {
        Path path = Path.of("test.dat");
        if (!path.toFile().exists()) {
            try {
                path.toFile().createNewFile();
                new FileStorage(path.toFile()).save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (Storage storage = FileStorage.load(path.toFile())) {
            if (storage.getAllUsers().isEmpty()) {
                storage.saveUser(new User());
                System.out.println("User created");
            } else {
                storage.getAllUsers().forEach(System.out::println);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
