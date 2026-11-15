package folki.dev.infra.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.FailToLoadStorageError;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsUsersAndLinks() {
        Path file = tempDir.resolve("storage.json");
        User user = new User();
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        Link link = new Link(user, "https://example.com", "aB3x9Z", created, created.plusHours(1), 5, 2);

        Storage storage = FileStorage.load(file);
        storage.saveUser(user);
        storage.saveLink(link);

        Storage reloaded = FileStorage.load(file);
        Link loaded = reloaded.getLinkByShortURL("aB3x9Z");

        assertNotNull(loaded);
        assertEquals(user, loaded.owner);
        assertEquals("https://example.com", loaded.originURL);
        assertEquals(created.plusHours(1), loaded.expirationDateTime);
        assertEquals(5, loaded.maxClicks);
        assertEquals(2, loaded.clickCount.get());
        assertNotNull(reloaded.getUserById(user.id));
    }

    @Test
    void missingFileGivesEmptyStorage() {
        Storage storage = FileStorage.load(tempDir.resolve("absent.json"));

        assertTrue(storage.getAllLinks().isEmpty());
        assertTrue(storage.getAllUsers().isEmpty());
    }

    @Test
    void corruptedFileIsReportedWithAppException() throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ not json");

        assertThrows(FailToLoadStorageError.class, () -> FileStorage.load(file));
    }
}
