package folki.dev;

import folki.dev.core.entities.User;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;

import java.nio.file.Path;

public class Main {
    static void main(String[] args) {
        Path path = Path.of("test.json");
        try (Storage storage = FileStorage.load(path)) {
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
