package folki.dev.core.services;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.AccessDeniedError;
import folki.dev.core.exceptions.CodeGenerationError;
import folki.dev.core.exceptions.LinkExpiredError;
import folki.dev.core.exceptions.LinkLimitExceededError;
import folki.dev.core.exceptions.LinkNotFoundError;
import folki.dev.core.exceptions.ValidationError;
import folki.dev.infra.Notifier;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.Storage;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class LinkService {
    private static final Logger LOG = Logger.getLogger(LinkService.class.getName());
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    public static final int MAX_CLICKS_LIMIT = 1_000_000;
    public static final Duration MAX_TTL = Duration.ofDays(365);

    private final Storage storage;
    private final CodeGenerator codeGenerator;
    private final ShortenerSettings settings;
    private final Notifier notifier;
    private final Clock clock;

    public LinkService(
            Storage storage, CodeGenerator codeGenerator, ShortenerSettings settings, Notifier notifier, Clock clock) {
        this.storage = storage;
        this.codeGenerator = codeGenerator;
        this.settings = settings;
        this.notifier = notifier;
        this.clock = clock;
    }

    public synchronized @NonNull Link createLink(@NonNull User owner, @NonNull String originURL) {
        LocalDateTime now = LocalDateTime.now(clock);
        Link link = new Link(
                owner,
                originURL,
                generateUniqueCode(),
                now,
                now.plus(settings.defaultTtl()),
                settings.defaultMaxClicks(),
                0);
        storage.saveLink(link);
        LOG.info("Создана ссылка " + link.shortURL + " пользователем " + owner.id);
        return link;
    }

    public synchronized @NonNull Link openLink(@NonNull String shortURL) {
        Link link = storage.getLinkByShortURL(shortURL);
        if (link == null) {
            throw new LinkNotFoundError("Ссылка " + shortURL + " не найдена");
        }

        if (link.isExpired(LocalDateTime.now(clock))) {
            storage.deleteLink(link);
            LOG.info("Ссылка " + shortURL + " истекла и удалена при попытке перехода");
            throw new LinkExpiredError("Ссылка " + shortURL + " истекла и удалена");
        }

        if (!link.tryRegisterClick()) {
            LOG.info("Переход по ссылке " + shortURL + " заблокирован: лимит исчерпан");
            throw new LinkLimitExceededError("Лимит переходов для ссылки " + shortURL + " исчерпан");
        }
        storage.saveLink(link);

        if (!link.isActive()) {
            LOG.info("Ссылка " + shortURL + " исчерпала лимит переходов");
            notifier.notify("Это был последний переход: лимит для ссылки " + shortURL + " исчерпан");
        }
        return link;
    }

    public synchronized @NonNull Link editLink(
            @NonNull User user, @NonNull String shortURL, @Nullable Integer newMaxClicks, @Nullable Duration newTtl) {
        if (newMaxClicks == null && newTtl == null) {
            throw new ValidationError("Укажите хотя бы один параметр: --limit или --ttl");
        }
        Link link = getOwnedLink(user, shortURL);

        if (newMaxClicks != null) {
            validateMaxClicks(newMaxClicks);
            if (newMaxClicks < link.clickCount.get()) {
                throw new ValidationError("Новый лимит " + newMaxClicks + " меньше уже совершённых переходов ("
                        + link.clickCount.get() + ")");
            }
        }
        if (newTtl != null) {
            validateTtl(newTtl);
        }

        if (newMaxClicks != null) {
            link.maxClicks = newMaxClicks;
        }
        if (newTtl != null) {
            link.expirationDateTime = LocalDateTime.now(clock).plus(newTtl);
        }
        storage.saveLink(link);
        LOG.info("Ссылка " + shortURL + " изменена владельцем: лимит " + link.maxClicks + ", истекает "
                + link.expirationDateTime);
        return link;
    }

    public synchronized void deleteLink(@NonNull User user, @NonNull String shortURL) {
        Link link = getOwnedLink(user, shortURL);
        storage.deleteLink(link);
        LOG.info("Ссылка " + shortURL + " удалена владельцем");
    }

    public @NonNull List<Link> getUserLinks(@NonNull User user) {
        return storage.getAllLinksByUser(user);
    }

    private @NonNull Link getOwnedLink(@NonNull User user, @NonNull String shortURL) {
        Link link = storage.getLinkByShortURL(shortURL);
        if (link == null) {
            throw new LinkNotFoundError("Ссылка " + shortURL + " не найдена");
        }
        if (!link.owner.equals(user)) {
            LOG.warning("Пользователь " + user.id + " пытался изменить чужую ссылку " + shortURL);
            throw new AccessDeniedError("Нет прав: ссылка " + shortURL + " принадлежит другому пользователю");
        }
        return link;
    }

    private static void validateMaxClicks(int maxClicks) {
        if (maxClicks <= 0 || maxClicks > MAX_CLICKS_LIMIT) {
            throw new ValidationError("Лимит переходов должен быть от 1 до " + MAX_CLICKS_LIMIT);
        }
    }

    private static void validateTtl(@NonNull Duration ttl) {
        if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(MAX_TTL) > 0) {
            throw new ValidationError("TTL должен быть от 1 секунды до " + MAX_TTL.toDays() + " дней");
        }
    }

    public synchronized int removeExpiredLinks() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Link> expired = storage.getAllLinks().stream()
                .filter(link -> link.isExpired(now))
                .toList();

        for (Link link : expired) {
            storage.deleteLink(link);
            LOG.info("Ссылка " + link.shortURL + " истекла и удалена фоновой очисткой");
            notifier.notify("Ссылка " + link.shortURL + " истекла и удалена");
        }
        return expired.size();
    }

    private @NonNull String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (storage.getLinkByShortURL(code) == null) {
                return code;
            }
        }
        throw new CodeGenerationError(
                "Не удалось подобрать уникальный короткий код, увеличьте shortener.linkLength в конфигурации");
    }
}
