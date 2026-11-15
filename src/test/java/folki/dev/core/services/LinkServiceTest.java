package folki.dev.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.AccessDeniedError;
import folki.dev.core.exceptions.CodeGenerationError;
import folki.dev.core.exceptions.LinkExpiredError;
import folki.dev.core.exceptions.LinkLimitExceededError;
import folki.dev.core.exceptions.LinkNotFoundError;
import folki.dev.core.exceptions.ValidationError;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.FileStorage;
import folki.dev.infra.storage.Storage;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LinkServiceTest {
    private static final String URL = "https://example.com/some/long/path";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final int DEFAULT_LIMIT = 10;

    @TempDir
    Path tempDir;

    private final List<String> notifications = new CopyOnWriteArrayList<>();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
    private final User alice = new User();
    private final User bob = new User();

    private Storage storage;
    private LinkService service;

    @BeforeEach
    void setUp() {
        storage = new FileStorage(tempDir.resolve("storage.json").toFile());
        service = newService(new RandomCodeGenerator(6));
    }

    private LinkService newService(CodeGenerator generator) {
        ShortenerSettings settings = new ShortenerSettings(
                6,
                "https://s.test",
                Duration.ofSeconds(1),
                DEFAULT_TTL,
                DEFAULT_LIMIT,
                tempDir.resolve("storage.json"));
        return new LinkService(storage, generator, settings, notifications::add, clock);
    }

    @Test
    void sameUrlForTwoUsersGivesDifferentShortLinks() {
        Link aliceLink = service.createLink(alice, URL);
        Link bobLink = service.createLink(bob, URL);

        assertNotEquals(aliceLink.shortURL, bobLink.shortURL);
        assertEquals(alice, aliceLink.owner);
        assertEquals(bob, bobLink.owner);
    }

    @Test
    void regeneratesCodeOnCollision() {
        Iterator<String> codes = List.of("aaaaaa", "aaaaaa", "bbbbbb").iterator();
        LinkService serviceWithCollision = newService(codes::next);

        serviceWithCollision.createLink(alice, URL);
        Link second = serviceWithCollision.createLink(bob, URL);

        assertEquals("bbbbbb", second.shortURL);
    }

    @Test
    void failsWhenNoFreeCodeFound() {
        LinkService alwaysSameCode = newService(() -> "aaaaaa");
        alwaysSameCode.createLink(alice, URL);

        assertThrows(CodeGenerationError.class, () -> alwaysSameCode.createLink(bob, URL));
    }

    @Test
    void usesDefaultsFromSettings() {
        Link link = service.createLink(alice, URL);

        assertEquals(DEFAULT_LIMIT, link.maxClicks);
        assertEquals(LocalDateTime.now(clock).plus(DEFAULT_TTL), link.expirationDateTime);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "example.com", "ftp://example.com", "https://", "http://exa mple.com"})
    void rejectsInvalidUrls(String url) {
        assertThrows(ValidationError.class, () -> service.createLink(alice, url));
    }

    @Test
    void rejectsOutOfRangeLimitAndTtl() {
        assertThrows(ValidationError.class, () -> service.createLink(alice, URL, 0, null));
        assertThrows(ValidationError.class, () -> service.createLink(alice, URL, -5, null));
        assertThrows(
                ValidationError.class, () -> service.createLink(alice, URL, LinkService.MAX_CLICKS_LIMIT + 1, null));
        assertThrows(ValidationError.class, () -> service.createLink(alice, URL, null, Duration.ZERO));
        assertThrows(
                ValidationError.class, () -> service.createLink(alice, URL, null, LinkService.MAX_TTL.plusSeconds(1)));
    }

    @Test
    void fourthClickIsRejectedWhenLimitIsThree() {
        Link link = service.createLink(alice, URL, 3, null);

        for (int i = 0; i < 3; i++) {
            assertEquals(URL, service.openLink(link.shortURL).originURL);
        }

        assertThrows(LinkLimitExceededError.class, () -> service.openLink(link.shortURL));
        assertEquals(3, link.clickCount.get());
        assertFalse(link.isActive());
        assertTrue(notifications.stream().anyMatch(message -> message.contains("лимит")));
    }

    @Test
    void concurrentClicksNeverExceedLimit() throws InterruptedException {
        int limit = 50;
        int attempts = 200;
        Link link = service.createLink(alice, URL, limit, null);
        AtomicInteger successful = new AtomicInteger();
        Callable<Void> click = () -> {
            try {
                service.openLink(link.shortURL);
                successful.incrementAndGet();
            } catch (LinkLimitExceededError ignored) {
            }
            return null;
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            pool.invokeAll(Collections.nCopies(attempts, click));
        }

        assertEquals(limit, successful.get());
        assertEquals(limit, link.clickCount.get());
    }

    @Test
    void openingUnknownLinkFails() {
        assertThrows(LinkNotFoundError.class, () -> service.openLink("nope00"));
    }

    @Test
    void expiredLinkIsNotOpenedEvenBeforeCleanup() {
        Link link = service.createLink(alice, URL, null, Duration.ofSeconds(2));
        clock.advance(Duration.ofSeconds(2));

        LinkExpiredError error = assertThrows(LinkExpiredError.class, () -> service.openLink(link.shortURL));
        assertTrue(error.getMessage().contains("истекла"));
        assertNull(storage.getLinkByShortURL(link.shortURL));
    }

    @Test
    void cleanupRemovesOnlyExpiredLinksAndNotifies() {
        Link shortLived = service.createLink(alice, URL, null, Duration.ofSeconds(1));
        Link longLived = service.createLink(alice, URL, null, Duration.ofHours(1));
        clock.advance(Duration.ofSeconds(5));

        assertEquals(1, service.removeExpiredLinks());

        assertNull(storage.getLinkByShortURL(shortLived.shortURL));
        assertNotNull(storage.getLinkByShortURL(longLived.shortURL));
        assertEquals(List.of("Ссылка " + shortLived.shortURL + " истекла и удалена"), notifications);
    }

    @Test
    void foreignUserCannotEditOrDelete() {
        Link link = service.createLink(alice, URL);

        assertThrows(AccessDeniedError.class, () -> service.editLink(bob, link.shortURL, 100, null));
        assertThrows(AccessDeniedError.class, () -> service.deleteLink(bob, link.shortURL));

        assertEquals(DEFAULT_LIMIT, link.maxClicks);
        assertNotNull(storage.getLinkByShortURL(link.shortURL));
    }

    @Test
    void ownerCanEditLimitAndTtl() {
        Link link = service.createLink(alice, URL, 2, null);
        service.openLink(link.shortURL);
        service.openLink(link.shortURL);
        assertFalse(link.isActive());

        service.editLink(alice, link.shortURL, 5, Duration.ofDays(1));

        assertEquals(5, link.maxClicks);
        assertTrue(link.isActive());
        assertEquals(LocalDateTime.now(clock).plusDays(1), link.expirationDateTime);
    }

    @Test
    void editRejectsLimitBelowClickCount() {
        Link link = service.createLink(alice, URL, 5, null);
        service.openLink(link.shortURL);
        service.openLink(link.shortURL);

        assertThrows(ValidationError.class, () -> service.editLink(alice, link.shortURL, 1, null));
        assertThrows(ValidationError.class, () -> service.editLink(alice, link.shortURL, null, null));
        assertEquals(5, link.maxClicks);
    }

    @Test
    void ownerCanDeleteLink() {
        Link link = service.createLink(alice, URL);

        service.deleteLink(alice, link.shortURL);

        assertNull(storage.getLinkByShortURL(link.shortURL));
        assertThrows(LinkNotFoundError.class, () -> service.openLink(link.shortURL));
    }

    @Test
    void userSeesOnlyOwnLinks() {
        Link aliceLink = service.createLink(alice, URL);
        service.createLink(bob, URL);
        service.createLink(bob, "https://example.org");

        assertEquals(List.of(aliceLink), service.getUserLinks(alice));
        assertEquals(2, service.getUserLinks(bob).size());
    }
}
