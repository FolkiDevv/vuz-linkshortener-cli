package folki.dev.core.entities;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {
    public UUID id = UUID.randomUUID();

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return id.equals(((User) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                '}';
    }
}
