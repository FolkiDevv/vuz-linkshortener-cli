package folki.dev.core.services;

import folki.dev.core.exceptions.ValidationError;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.NonNull;

public final class UrlValidator {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private UrlValidator() {}

    public static @NonNull URI validate(@NonNull String url) {
        String trimmed = url.strip();
        if (trimmed.isEmpty()) {
            throw new ValidationError("Ссылка не может быть пустой");
        }

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new ValidationError("Некорректная ссылка: " + trimmed);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new ValidationError("Поддерживаются только ссылки http:// и https://, например https://example.com");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ValidationError("В ссылке не указан домен: " + trimmed);
        }
        return uri;
    }
}
