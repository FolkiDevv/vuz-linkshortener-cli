package folki.dev.infra.browser;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public final class DesktopBrowser implements Browser {
    private static final Logger LOG = Logger.getLogger(DesktopBrowser.class.getName());

    @Override
    public boolean open(URI uri) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            LOG.info("Desktop API недоступен, ссылка " + uri + " не открыта в браузере");
            return false;
        }
        try {
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LOG.warning("Не удалось открыть " + uri + " в браузере: " + e);
            return false;
        }
    }
}
