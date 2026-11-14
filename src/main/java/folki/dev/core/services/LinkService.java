package folki.dev.core.services;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.CodeGenerationError;
import folki.dev.core.exceptions.LinkLimitExceededError;
import folki.dev.core.exceptions.LinkNotFoundError;
import folki.dev.infra.Notifier;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.Storage;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;

public final class LinkService {
    private static final Logger LOG = Logger.getLogger(LinkService.class.getName());
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final Storage storage;
    private final CodeGenerator codeGenerator;
    private final ShortenerSettings settings;
    private final Notifier notifier;

    public LinkService(Storage storage, CodeGenerator codeGenerator, ShortenerSettings settings, Notifier notifier) {
        this.storage = storage;
        this.codeGenerator = codeGenerator;
        this.settings = settings;
        this.notifier = notifier;
    }

    public synchronized @NonNull Link createLink(@NonNull User owner, @NonNull String originURL) {
        LocalDateTime now = LocalDateTime.now();
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
