package folki.dev.cli;

import folki.dev.core.exceptions.ValidationError;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Command(String name, List<String> args, Map<String, String> options) {
    private static final String OPTION_PREFIX = "--";
    private static final Pattern DURATION = Pattern.compile("(\\d+)([smhd]?)");
    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static @NonNull Command parse(@NonNull String line) {
        String[] tokens = WHITESPACE.split(line.strip(), -1);
        List<String> args = new ArrayList<>();
        Map<String, String> options = new HashMap<>();

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            if (!token.startsWith(OPTION_PREFIX)) {
                args.add(token);
                continue;
            }
            String option = token.substring(OPTION_PREFIX.length());
            if (i + 1 >= tokens.length || tokens[i + 1].startsWith(OPTION_PREFIX)) {
                throw new ValidationError("Для параметра " + token + " не указано значение");
            }
            options.put(option, tokens[++i]);
        }
        return new Command(tokens[0].toLowerCase(Locale.ROOT), args, options);
    }

    public @NonNull String requireArg(int index, @NonNull String usage) {
        if (index >= args.size()) {
            throw new ValidationError("Не хватает аргументов. Использование: " + usage);
        }
        return args.get(index);
    }

    public void allowOnly(@NonNull Set<String> allowedOptions, @NonNull String usage) {
        for (String option : options.keySet()) {
            if (!allowedOptions.contains(option)) {
                throw new ValidationError("Неизвестный параметр --" + option + ". Использование: " + usage);
            }
        }
    }

    public @Nullable Integer intOption(@NonNull String option) {
        String value = options.get(option);
        if (value == null) {
            return null;
        }
        if (!DIGITS.matcher(value).matches()) {
            throw new ValidationError("--" + option + " должен быть положительным целым числом, получено: " + value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ValidationError("Слишком большое значение --" + option + ": " + value);
        }
    }

    public @Nullable Duration durationOption(@NonNull String option) {
        String value = options.get(option);
        if (value == null) {
            return null;
        }
        Matcher matcher = DURATION.matcher(value.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new ValidationError(
                    "Некорректное значение --" + option + ": " + value + ". Примеры: 90 (секунды), 30m, 2h, 7d");
        }
        try {
            long amount = Long.parseLong(matcher.group(1));
            return switch (matcher.group(2)) {
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> Duration.ofSeconds(amount);
            };
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ValidationError("Слишком большое значение --" + option + ": " + value);
        }
    }
}
