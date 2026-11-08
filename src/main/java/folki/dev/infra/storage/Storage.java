package folki.dev.infra.storage;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;

import java.util.List;
import java.util.UUID;

public interface Storage {
    List<Link> getAllLinks();

    List<Link> getAllLinksByUser(User user);

    Link getByShortURL(String shortURL);

    void saveLink(Link link);

    void deleteLink(Link link);

    void saveUser(User user);

    void deleteUser(User user);

    List<User> getAllUsers();

    User getUserById(UUID id);
}
