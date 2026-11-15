package folki.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.AccessDeniedError;
import folki.dev.core.exceptions.LinkLimitExceededError;
import folki.dev.core.services.LinkService;
import folki.dev.core.services.RandomCodeGenerator;
import folki.dev.infra.CleanupScheduler;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinkLifecycleIntegrationTest {
    private static final String URL = "https://example.com/page";
    private static final Duration CLEANUP_INTERVAL = Duration.ofMillis(200);
    private static final Duration WAIT_STEP = Duration.ofMillis(100);
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);

    @TempDir
    Path tempDir;

    private final List<String> notifications = new CopyOnWriteArrayList<>();
    private Path storagePath;
    private LinkService service;

    @BeforeEach
    void setUp() {
        storagePath = tempDir.resolve("storage.json");
        ShortenerSettings settings =
                new ShortenerSettings(6, "https://s.test", CLEANUP_INTERVAL, Duration.ofHours(1), 10, storagePath);
        Storage storage = FileStorage.load(storagePath);
        service = new LinkService(
                storage,
                new RandomCodeGenerator(settings.codeLength()),
                settings,
                notifications::add,
                Clock.system(ZoneId.systemDefault()));
    }

    @Test
    void createdOpenedNTimesThenBlocked() {
        User user = new User();
        Link link = service.createLink(user, URL, 3, null);

        for (int i = 0; i < 3; i++) {
            service.openLink(link.shortURL);
        }
        assertThrows(LinkLimitExceededError.class, () -> service.openLink(link.shortURL));

        Link persisted = FileStorage.load(storagePath).getLinkByShortURL(link.shortURL);
        assertNotNull(persisted);
        assertEquals(3, persisted.clickCount.get());
    }

    @Test
    void createdWaitedTtlLinkDisappears() throws InterruptedException {
        User user = new User();
        Link link = service.createLink(user, URL, null, Duration.ofSeconds(1));

        try (CleanupScheduler ignored = new CleanupScheduler(service::removeExpiredLinks, CLEANUP_INTERVAL)) {
            long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
            while (!service.getUserLinks(user).isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(WAIT_STEP);
            }
        }

        assertTrue(service.getUserLinks(user).isEmpty());
        assertNull(FileStorage.load(storagePath).getLinkByShortURL(link.shortURL));
        assertTrue(notifications.contains("Ссылка " + link.shortURL + " истекла и удалена"));
    }

    @Test
    void foreignUserCannotDelete() {
        User owner = new User();
        User stranger = new User();
        Link link = service.createLink(owner, URL);

        assertThrows(AccessDeniedError.class, () -> service.deleteLink(stranger, link.shortURL));

        assertNotNull(FileStorage.load(storagePath).getLinkByShortURL(link.shortURL));
        assertEquals(List.of(link), service.getUserLinks(owner));
    }
}
