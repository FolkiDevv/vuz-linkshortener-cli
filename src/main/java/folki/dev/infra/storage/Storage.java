package folki.dev.infra.storage;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public interface Storage extends Serializable, AutoCloseable {
    @Contract(pure = true)
    static @Nullable Storage load(Path path) {
        return null;
    }

    void save();

    List<Link> getAllLinks();

    List<Link> getAllLinksByUser(User user);

    @Nullable
    Link getByShortURL(String shortURL);

    void saveLink(Link link);

    void deleteLink(Link link);

    void saveUser(User user);

    void deleteUser(User user);

    List<User> getAllUsers();

    @Nullable
    User getUserById(UUID id);

    @Override
    default void close() {
        save();
    }
}
