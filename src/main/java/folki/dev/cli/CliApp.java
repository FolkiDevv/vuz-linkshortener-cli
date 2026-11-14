package folki.dev.cli;

import folki.dev.core.Interaction;
import folki.dev.infra.config.Config;
import folki.dev.infra.storage.Storage;

public final class CliApp {
    private final Interaction intr = new CliInteraction();
    private final Storage storage;
    private final Config config;

    public CliApp(Config config, Storage storage) {
        this.config = config;
        this.storage = storage;
    }

    public void mainCycle() {
        while (mainMenu()) {}
    }

    private boolean mainMenu() {
        intr.print("""
            Доступный функционал:\
            1. Мои ссылки
            2. Сократить ссылку
            3. Выход из программы
            """);
        switch (intr.askForInt("Выберите действие")) {
            case 1:
                intr.print("1");
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
}
