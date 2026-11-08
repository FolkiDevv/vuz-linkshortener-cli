package folki.dev.infra.storage;

import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.FailToLoadStorageError;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileStorage implements Storage {
    private final File file;
    private final Map<String, Link> links = new HashMap<>();
    private final Map<UUID, User> users = new HashMap<>();

    public static Storage load(File file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (Storage) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new FailToLoadStorageError("Failed to load storage");
        }
    }

    public FileStorage(File file) {
        this.file = file;
    }

    @Override
    public List<Link> getAllLinks() {
        return List.copyOf(links.values());
    }

    @Override
    public List<Link> getAllLinksByUser(User user) {
        return List.copyOf(links.values().stream()
                .filter(link -> link.owner.equals(user))
                .toList());
    }

    @Override
    public Link getByShortURL(String shortURL) {
        return links.get(shortURL);
    }

    @Override
    public void saveLink(Link link) {
        links.put(link.shortURL, link);
        save();
    }

    @Override
    public void deleteLink(Link link) {
        links.remove(link.shortURL);
        save();
    }

    @Override
    public void saveUser(User user) {
        users.put(user.id, user);
        save();
    }

    @Override
    public void deleteUser(User user) {
        users.remove(user.id);
        save();
    }

    @Override
    public List<User> getAllUsers() {
        return List.copyOf(users.values());
    }

    @Override
    public User getUserById(UUID id) {
        return users.get(id);
    }

    @Override
    public void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
