package folki.dev.core.entities;

import folki.dev.infra.config.Config;

import java.io.Serializable;
import java.time.LocalDateTime;


public class Link implements Serializable {
    public User owner;
    public String originURL;
    public String shortURL;
    public LocalDateTime creationDateTime;
    public LocalDateTime expirationDateTime;

    public Link(User owner, String originURL, String shortURL, Config config) {
        this.owner = owner;
        this.originURL = originURL;
        this.shortURL = shortURL;
        this.creationDateTime = LocalDateTime.now();
        this.expirationDateTime = LocalDateTime.now().plusSeconds(config.getInt("link.ttl.seconds", 86400));
    }

    @Override
    public int hashCode() {
        return originURL.hashCode() + shortURL.hashCode() + owner.hashCode();
    }

    @Override
    public String toString() {
        return "Link{" +
                "owner=" + owner +
                ", originURL='" + originURL + '\'' +
                ", shortURL='" + shortURL + '\'' +
                ", creationDateTime=" + creationDateTime +
                ", expirationDateTime=" + expirationDateTime +
                '}';
    }
}
