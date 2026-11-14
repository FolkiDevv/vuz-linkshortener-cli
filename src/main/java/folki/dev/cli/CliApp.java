package folki.dev.cli;

import folki.dev.core.Interaction;

public class CliApp {
    private Interaction intr = new CliInteraction();

    public void mainCycle() {
        while (true) {
            switch (this.intr.askForInt("Выберите действие")) {
                case 1:
                    intr.say("1");
                    break;
                case 2:
                    intr.say("2");
                default:
                    intr.say("Неизвестная команда");
            }
        }
    }
}
