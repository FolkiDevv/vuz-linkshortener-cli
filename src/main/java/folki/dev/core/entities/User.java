package folki.dev.core.entities;

import java.util.UUID;

public class User {
    public UUID id;

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
