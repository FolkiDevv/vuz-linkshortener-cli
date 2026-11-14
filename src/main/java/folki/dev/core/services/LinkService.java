package folki.dev.core.services;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.CodeGenerationError;
import folki.dev.infra.config.Config;
import folki.dev.infra.storage.Storage;
import org.jspecify.annotations.NonNull;

public final class LinkService {
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final Storage storage;
    private final CodeGenerator codeGenerator;
    private final Config config;

    public LinkService(Storage storage, CodeGenerator codeGenerator, Config config) {
        this.storage = storage;
        this.codeGenerator = codeGenerator;
        this.config = config;
    }

    public synchronized @NonNull Link createLink(@NonNull User owner, @NonNull String originURL) {
        Link link = new Link(owner, originURL, generateUniqueCode(), config);
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
