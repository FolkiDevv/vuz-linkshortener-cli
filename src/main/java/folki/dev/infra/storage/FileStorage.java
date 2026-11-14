package folki.dev.infra.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.FailToLoadStorageError;
import folki.dev.core.exceptions.FailToSaveStorageError;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileStorage implements Storage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File file;
    private final Map<String, Link> links = new HashMap<>();
    private final Map<UUID, User> users = new HashMap<>();

    private record FileData(List<UUID> users, List<LinkJson> links) {}

    private record LinkJson(
            UUID owner, String originURL, String shortURL, String creationDateTime, String expirationDateTime) {}

    public FileStorage(File file) {
        this.file = file;
    }

    public static @NonNull Storage load(@NonNull Path path) throws FailToLoadStorageError {
        FileStorage storage = new FileStorage(path.toFile());
        if (!path.toFile().exists() || path.toFile().length() == 0) {
            return storage;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            FileData data = GSON.fromJson(reader, FileData.class);

            for (UUID id : data.users()) {
                storage.users.put(id, new User(id));
            }
            for (LinkJson json : data.links()) {
                User owner = storage.users.computeIfAbsent(json.owner(), id -> new User(id));
                Link link = new Link(
                        owner,
                        json.originURL(),
                        json.shortURL(),
                        LocalDateTime.parse(json.creationDateTime()),
                        LocalDateTime.parse(json.expirationDateTime()));
                storage.links.put(link.shortURL, link);
            }
        } catch (IOException | RuntimeException e) {
            throw new FailToLoadStorageError("Failed to load storage from " + path + ": " + e);
        }
        return storage;
    }

    @Override
    public void save() throws FailToSaveStorageError {
        List<UUID> userIds = List.copyOf(users.keySet());
        List<LinkJson> linkJsons = links.values().stream()
                .map(link -> new LinkJson(
                        link.owner.id,
                        link.originURL,
                        link.shortURL,
                        link.creationDateTime.toString(),
                        link.expirationDateTime.toString()))
                .toList();

        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(new FileData(userIds, linkJsons), writer);
        } catch (IOException e) {
            throw new FailToSaveStorageError("Failed to save storage to " + file + ": " + e);
        }
    }

    @Override
    public List<Link> getAllLinks() {
        return List.copyOf(links.values());
    }

    @Override
    public List<Link> getAllLinksByUser(User user) {
        return links.values().stream().filter(link -> link.owner.equals(user)).toList();
    }

    @Override
    @Nullable
    public Link getByShortURL(String shortURL) {
        return links.get(shortURL);
    }

    @Override
    public void saveLink(Link link) {
        users.putIfAbsent(link.owner.id, link.owner); // владелец ссылки тоже должен попасть в файл
        links.put(link.shortURL, link);
        save();
    }

    @Override
    public void deleteLink(@NonNull Link link) {
        links.remove(link.shortURL);
        save();
    }

    @Override
    public void saveUser(User user) {
        users.put(user.id, user);
        save();
    }

    @Override
    public void deleteUser(@NonNull User user) {
        users.remove(user.id);
        links.values().removeIf(link -> link.owner.equals(user));
        save();
    }

    @Override
    public List<User> getAllUsers() {
        return List.copyOf(users.values());
    }

    @Override
    @Nullable
    public User getUserById(UUID id) {
        return users.get(id);
    }
}
