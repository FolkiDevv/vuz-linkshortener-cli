package folki.dev.core;

public abstract class Interaction {
    public abstract void say(String message);

    public abstract String read();

    public String askForString(String prompt) {
        say(prompt + ": ");
        return read();
    }

    public String askForString(String prompt, String defaultValue) {
        String input = askForString(prompt + ": ");
        return input.isEmpty() ? defaultValue : input;
    }

    public int askForInt(String prompt) {
        String input = askForString(prompt);
        return input.isEmpty() ? 0 : Integer.parseInt(input);
    }

    public int askForInt(String prompt, int defaultValue) {
        String input = askForString(prompt);
        return input.isEmpty() ? defaultValue : Integer.parseInt(input);
    }

    public boolean askForBoolean(String prompt) {
        String input = askForString(prompt + " (y/N)");
        return input.equalsIgnoreCase("y");
    }

    public boolean askForBoolean(String prompt, boolean defaultValue) {
        prompt += defaultValue ? " (Y/n)" : " (y/N)";
        String input = askForString(prompt);
        return input.isEmpty() ? defaultValue : Boolean.parseBoolean(input);
    }
}
