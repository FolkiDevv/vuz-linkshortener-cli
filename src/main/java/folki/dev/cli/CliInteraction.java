package folki.dev.cli;

import folki.dev.core.Interaction;
import java.util.Scanner;

public final class CliInteraction extends Interaction {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void print(String message) {
        System.out.println(message);
    }

    @Override
    public void printPrompt(String prompt) {
        System.out.print(prompt);
        System.out.flush();
    }

    @Override
    public String read() {
        return scanner.nextLine();
    }
}
