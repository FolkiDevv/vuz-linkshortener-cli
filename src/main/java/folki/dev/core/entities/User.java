package folki.dev.core.entities;

import java.util.UUID;

public final class User {
    public final UUID id;

    public User() {
        this.id = UUID.randomUUID();
    }

    public User(UUID id) {
        this.id = id;
    }

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
        return "User{" + "id=" + id + '}';
    }
}
