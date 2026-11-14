package folki.dev.cli;

import folki.dev.core.Interaction;
import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.AppException;
import folki.dev.core.exceptions.UserNotFoundError;
import folki.dev.core.services.LinkService;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.Storage;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

public final class CliApp {
    private final Interaction intr;
    private final Storage storage;
    private final ShortenerSettings settings;
    private final LinkService linkService;
    private final User user;

    public CliApp(Interaction intr, ShortenerSettings settings, Storage storage, LinkService linkService) {
        this.intr = intr;
        this.settings = settings;
        this.storage = storage;
        this.linkService = linkService;
        this.user = intr.askForBoolean("У вас есть UUID?") ? userLogin() : userRegister();
    }

    public void mainCycle() {
        while (mainMenu()) {}
    }

    private @NonNull User userLogin() {
        UUID user_id = intr.askForUUID("Введите ваш UUID для авторизации");
        User user = storage.getUserById(user_id);

        if (user == null) {
            throw new UserNotFoundError("Пользователь с таким логином не зарегистрирован в системе!");
        }

        intr.print("Вход выполнен успешно! Ваш UUID: " + user.id);

        return user;
    }

    private @NonNull User userRegister() {
        User user = new User();
        intr.print("Вы успешно зарегистрированы в системе! Ваш логин: " + user.id);
        intr.print("Обязательно сохраните свой логин!\n");
        storage.saveUser(user);

        return user;
    }

    private boolean mainMenu() {
        intr.print("""
            Доступный функционал:
            1. Открыть ссылку
            2. Мои ссылки
            3. Сократить ссылку
            4. Выход из программы
            """);
        switch (intr.askForInt("Выберите действие")) {
            case 1:
                openLink();
                break;
            case 2:
                showMyLinks();
                break;
            case 3:
                createShortLink();
                break;
            case 4:
                storage.save();
                intr.print("Все данные сохранены.");
                return false;
            default:
                intr.print("Неизвестная команда");
        }
        return true;
    }

    private void openLink() {
        String link = intr.askForString("Введите короткую ссылку или код");
        Link linkToOpen;
        try {
            linkToOpen = linkService.openLink(extractCode(link));
        } catch (AppException e) {
            intr.printError(e.getMessage());
            return;
        }

        intr.print("Открываю ссылку: " + linkToOpen.originURL);
        try {
            Desktop.getDesktop().browse(URI.create(linkToOpen.originURL));
        } catch (IOException e) {
            intr.print("Не удалось открыть ссылку");
        }
    }

    private void showMyLinks() {
        intr.print("Ваши ссылки:");
        for (Link link : storage.getAllLinksByUser(user)) {
            intr.print(link.toString());
        }
    }

    private void createShortLink() {
        String originURL = intr.askForString("Введите ссылку");
        Link link = linkService.createLink(user, originURL);
        intr.print("Ссылка успешно сокращена: " + toFullShortUrl(link));
    }

    private @NonNull String toFullShortUrl(@NonNull Link link) {
        return settings.toFullShortUrl(link.shortURL);
    }

    private static @NonNull String extractCode(@NonNull String input) {
        String trimmed = input.strip();
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }
}
