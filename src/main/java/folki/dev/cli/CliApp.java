package folki.dev.cli;

import folki.dev.core.Interaction;
import folki.dev.core.entities.Link;
import folki.dev.core.entities.User;
import folki.dev.core.exceptions.AppException;
import folki.dev.core.exceptions.UserNotFoundError;
import folki.dev.core.exceptions.ValidationError;
import folki.dev.core.services.LinkService;
import folki.dev.infra.browser.Browser;
import folki.dev.infra.config.ShortenerSettings;
import folki.dev.infra.storage.Storage;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class CliApp {
    private static final Logger LOG = Logger.getLogger(CliApp.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROMPT = "> ";
    private static final Set<String> LINK_OPTIONS = Set.of("limit", "ttl");

    private static final String CREATE_USAGE = "create <url> [--limit N] [--ttl T]";
    private static final String OPEN_USAGE = "open <short>";
    private static final String EDIT_USAGE = "edit <short> [--limit N] [--ttl T]";
    private static final String DELETE_USAGE = "delete <short>";
    private static final String LOGIN_USAGE = "login <uuid>";

    private static final String HELP = """
            Команды:
              create <url> [--limit N] [--ttl T]   сократить ссылку (по умолчанию лимит и TTL из конфига)
              open <short>                         перейти по короткой ссылке (код или полная ссылка)
              list                                 показать ваши ссылки
              edit <short> [--limit N] [--ttl T]   изменить лимит переходов и/или TTL своей ссылки
              delete <short>                       удалить свою ссылку
              whoami                               показать ваш UUID
              login <uuid>                         войти под другим пользователем
              register                             создать нового пользователя
              help                                 эта справка
              exit                                 сохранить данные и выйти

            TTL: число секунд или число с суффиксом s/m/h/d. При edit срок отсчитывается от текущего момента.

            Примеры:
              create https://example.com/very/long/path --limit 5 --ttl 2h
              open aB3x9Z
              edit aB3x9Z --limit 20 --ttl 1d
              delete aB3x9Z
            """;

    private final Interaction intr;
    private final Storage storage;
    private final ShortenerSettings settings;
    private final LinkService linkService;
    private final Browser browser;
    private @Nullable User user;

    public CliApp(
            Interaction intr, ShortenerSettings settings, Storage storage, LinkService linkService, Browser browser) {
        this.intr = intr;
        this.settings = settings;
        this.storage = storage;
        this.linkService = linkService;
        this.browser = browser;
    }

    private @NonNull User currentUser() {
        if (user == null) {
            throw new IllegalStateException("Пользователь ещё не авторизован");
        }
        return user;
    }

    public void mainCycle() {
        try {
            user = intr.askForBoolean("У вас есть UUID?") ? userLogin() : userRegister();
            intr.print("Введите help, чтобы увидеть список команд.");
            while (handleLine()) {}
        } catch (NoSuchElementException e) {
            intr.print("");
        }
        storage.save();
        intr.print("Все данные сохранены.");
    }

    private @NonNull User userLogin() {
        while (true) {
            UUID userId = intr.askForUUID("Введите ваш UUID для авторизации");
            User user = storage.getUserById(userId);
            if (user != null) {
                intr.print("Вход выполнен успешно! Ваш UUID: " + user.id);
                return user;
            }
            intr.printError("Пользователь с таким UUID не зарегистрирован в системе!");
            if (!intr.askForBoolean("Попробовать ещё раз?")) {
                return userRegister();
            }
        }
    }

    private @NonNull User userRegister() {
        User user = new User();
        storage.saveUser(user);
        intr.print("Вы успешно зарегистрированы в системе! Ваш UUID: " + user.id);
        intr.print("Обязательно сохраните свой UUID!\n");

        return user;
    }

    private boolean handleLine() {
        intr.printPrompt(PROMPT);
        String line = intr.read();
        if (line.isBlank()) {
            return true;
        }

        try {
            return execute(Command.parse(line));
        } catch (AppException e) {
            intr.printError(e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Необработанная ошибка при выполнении команды: " + line, e);
            intr.printError("Внутренняя ошибка, подробности в логе. Попробуйте ещё раз.");
        }
        return true;
    }

    private boolean execute(@NonNull Command command) {
        switch (command.name()) {
            case "create" -> createShortLink(command);
            case "open" -> openLink(command);
            case "list" -> showMyLinks();
            case "edit" -> editLink(command);
            case "delete" -> deleteLink(command);
            case "whoami" -> intr.print("Ваш UUID: " + currentUser().id);
            case "login" -> login(command);
            case "register" -> user = userRegister();
            case "help" -> intr.print(HELP);
            case "exit", "quit" -> {
                return false;
            }
            default -> intr.printError("Неизвестная команда «" + command.name() + "», введите help");
        }
        return true;
    }

    private void createShortLink(@NonNull Command command) {
        command.allowOnly(LINK_OPTIONS, CREATE_USAGE);
        String originURL = command.requireArg(0, CREATE_USAGE);
        Link link = linkService.createLink(
                currentUser(), originURL, command.intOption("limit"), command.durationOption("ttl"));
        intr.print("Ссылка успешно сокращена: " + toFullShortUrl(link));
        intr.print(
                "Лимит переходов: " + link.maxClicks + ", действует до " + link.expirationDateTime.format(DATE_FORMAT));
    }

    private void openLink(@NonNull Command command) {
        command.allowOnly(Set.of(), OPEN_USAGE);
        Link link = linkService.openLink(extractCode(command.requireArg(0, OPEN_USAGE)));

        intr.print("Открываю ссылку: " + link.originURL);
        if (!browser.open(URI.create(link.originURL))) {
            intr.print("Не удалось открыть браузер, перейдите по ссылке вручную: " + link.originURL);
        }
    }

    private void showMyLinks() {
        List<Link> links = linkService.getUserLinks(currentUser());
        if (links.isEmpty()) {
            intr.print("У вас пока нет ссылок. Создайте первую: " + CREATE_USAGE);
            return;
        }
        intr.print("Ваши ссылки:");
        for (Link link : links) {
            intr.print(formatLink(link));
        }
    }

    private void editLink(@NonNull Command command) {
        command.allowOnly(LINK_OPTIONS, EDIT_USAGE);
        String code = extractCode(command.requireArg(0, EDIT_USAGE));
        Link link =
                linkService.editLink(currentUser(), code, command.intOption("limit"), command.durationOption("ttl"));
        intr.print("Ссылка изменена:\n" + formatLink(link));
    }

    private void deleteLink(@NonNull Command command) {
        command.allowOnly(Set.of(), DELETE_USAGE);
        String code = extractCode(command.requireArg(0, DELETE_USAGE));
        linkService.deleteLink(currentUser(), code);
        intr.print("Ссылка " + code + " удалена");
    }

    private void login(@NonNull Command command) {
        String rawId = command.requireArg(0, LOGIN_USAGE);
        UUID userId;
        try {
            userId = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new ValidationError("Введенное значение не является верным UUID: " + rawId);
        }
        User found = storage.getUserById(userId);
        if (found == null) {
            throw new UserNotFoundError("Пользователь " + userId + " не зарегистрирован в системе");
        }
        user = found;
        intr.print("Вход выполнен успешно! Ваш UUID: " + found.id);
    }

    private @NonNull String formatLink(@NonNull Link link) {
        String status = link.isActive() ? "активна" : "неактивна (лимит исчерпан)";
        return "  " + toFullShortUrl(link) + " -> " + link.originURL
                + " | переходы " + link.clickCount.get() + "/" + link.maxClicks
                + " | до " + link.expirationDateTime.format(DATE_FORMAT)
                + " | " + status;
    }

    private @NonNull String toFullShortUrl(@NonNull Link link) {
        return settings.toFullShortUrl(link.shortURL);
    }

    private static @NonNull String extractCode(@NonNull String input) {
        String trimmed = input.strip();
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }
}
