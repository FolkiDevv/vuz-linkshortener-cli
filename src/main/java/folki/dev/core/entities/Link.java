package folki.dev.core.entities;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public final class Link {
    public final User owner;
    public final String originURL;
    public final String shortURL;
    public LocalDateTime creationDateTime;
    public LocalDateTime expirationDateTime;
    public volatile int maxClicks;
    public final AtomicInteger clickCount;

    public Link(
            User owner,
            String originURL,
            String shortURL,
            LocalDateTime creation,
            LocalDateTime expiration,
            int maxClicks,
            int clickCount) {
        this.owner = owner;
        this.originURL = originURL;
        this.shortURL = shortURL;
        this.creationDateTime = creation;
        this.expirationDateTime = expiration;
        this.maxClicks = maxClicks;
        this.clickCount = new AtomicInteger(clickCount);
    }

    public boolean tryRegisterClick() {
        int previous = clickCount.getAndUpdate(count -> count < maxClicks ? count + 1 : count);
        return previous < maxClicks;
    }

    public boolean isActive() {
        return clickCount.get() < maxClicks;
    }

    @Override
    public int hashCode() {
        return originURL.hashCode() + shortURL.hashCode() + owner.hashCode();
    }

    @Override
    public String toString() {
        return "Link{" + "owner="
                + owner + ", originURL='"
                + originURL + '\'' + ", shortURL='"
                + shortURL + '\'' + ", creationDateTime="
                + creationDateTime + ", expirationDateTime="
                + expirationDateTime + ", clicks="
                + clickCount.get() + "/" + maxClicks + '}';
    }
}
