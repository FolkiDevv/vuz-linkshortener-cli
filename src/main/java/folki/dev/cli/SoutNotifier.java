package folki.dev.cli;

import folki.dev.core.Interaction;
import folki.dev.infra.Notifier;

public final class SoutNotifier implements Notifier {
    private final Interaction cliInteraction;

    public SoutNotifier(Interaction cliInteraction) {
        this.cliInteraction = cliInteraction;
    }

    @Override
    public void notify(String message) {
        cliInteraction.print(message);
    }
}
