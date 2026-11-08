package folki.dev.core;

public interface Interaction {
    void say(String message);

    String read();

    String askForString(String prompt);

    String askForString(String prompt, String defaultValue);

    int askForInt(String prompt);

    int askForInt(String prompt, int defaultValue);

    boolean askForBoolean(String prompt);

    boolean askForBoolean(String prompt, boolean defaultValue);
}
