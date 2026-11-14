package folki.dev.core;

public abstract class Interaction {
    public abstract void print(String message);

    public void printError(String prompt) {
        print("ОШИБКА: " + prompt);
    }

    public abstract String read();

    public String askForString(String prompt) {
        print(prompt + ": ");
        return read();
    }

    public String askForString(String prompt, String defaultValue) {
        String input = askForString(prompt + ": ");
        return input.isEmpty() ? defaultValue : input;
    }

    public int askForInt(String prompt) {
        while (true) {
            try {
                String input = askForString(prompt);
                return input.isEmpty() ? 0 : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                printError("Введенное значение не является числом!");
            }
        }
    }

    public int askForInt(String prompt, int defaultValue) {
        while (true) {
            try {
                String input = askForString(prompt);
                return input.isEmpty() ? defaultValue : Integer.parseInt(input);
            } catch (NumberFormatException e) {
                printError("Введенное значение не является числом!");
            }
        }
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
