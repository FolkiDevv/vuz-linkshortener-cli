package folki.dev.cli;

import folki.dev.core.Interaction;
import java.util.Scanner;

public class CliInteraction extends Interaction {
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void say(String message) {
        System.out.println(message);
    }

    @Override
    public String read() {
        return scanner.nextLine();
    }
}
