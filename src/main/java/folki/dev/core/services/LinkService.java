package folki.dev.core.services;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.CodeGenerationError;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.Storage;
import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

public final class LinkService {
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final Storage storage;
    private final CodeGenerator codeGenerator;
    private final ShortenerSettings settings;

    public LinkService(Storage storage, CodeGenerator codeGenerator, ShortenerSettings settings) {
        this.storage = storage;
        this.codeGenerator = codeGenerator;
        this.settings = settings;
    }

    public synchronized @NonNull Link createLink(@NonNull User owner, @NonNull String originURL) {
        LocalDateTime now = LocalDateTime.now();
        Link link = new Link(owner, originURL, generateUniqueCode(), now, now.plus(settings.defaultTtl()));
        storage.saveLink(link);
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
