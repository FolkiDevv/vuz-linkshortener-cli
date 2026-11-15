package folki.dev.infra.browser;

import java.net.URI;

public interface Browser {
    boolean open(URI uri);
}
