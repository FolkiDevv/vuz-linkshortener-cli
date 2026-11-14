package folki.dev.cli;

import folki.dev.core.Interaction;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.UserNotFoundError;
import folki.dev.infra.config.Config;
import folki.dev.infra.storage.Storage;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public final class CliApp {
    private final Interaction intr = new CliInteraction();
    private final Storage storage;
    private final Config config;
    private final User user;

    public CliApp(Config config, Storage storage) {
        this.config = config;
        this.storage = storage;
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
            1. Мои ссылки
            2. Сократить ссылку
            3. Выход из программы
            """);
        switch (intr.askForInt("Выберите действие")) {
            case 1:
                showMyLinks();
                break;
            case 2:
                intr.print("2");
                break;
            case 3:
                storage.save();
                intr.print("Все данные сохранены.");
                return false;
            default:
                intr.print("Неизвестная команда");
        }
        return true;
    }

    private void showMyLinks() {
        intr.print("Ваши ссылки:");
        storage.getAllLinksByUser(user);
    }
}
