package folki.dev.cli;

import folki.dev.core.Interaction;
import java.util.Scanner;

public class CliInteraction implements Interaction {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void say(String message) {
        System.out.println(message);
    }

    @Override
    public String read() {
        return scanner.nextLine();
    }

    @Override
    public String askForString(String prompt) {
        say(prompt);
        return read();
    }

    @Override
    public String askForString(String prompt, String defaultValue) {
        String input = askForString(prompt);
        return input.isEmpty() ? defaultValue : input;
    }

    @Override
    public int askForInt(String prompt) {
        String input = askForString(prompt);
        return input.isEmpty() ? 0 : Integer.parseInt(input);
    }

    @Override
    public int askForInt(String prompt, int defaultValue) {
        String input = askForString(prompt);
        return input.isEmpty() ? defaultValue : Integer.parseInt(input);
    }

    @Override
    public boolean askForBoolean(String prompt) {
        String input = askForString(prompt);
        return input.equalsIgnoreCase("true");
    }

    @Override
    public boolean askForBoolean(String prompt, boolean defaultValue) {
        String input = askForString(prompt);
        return input.isEmpty() ? defaultValue : Boolean.parseBoolean(input);
    }
}
