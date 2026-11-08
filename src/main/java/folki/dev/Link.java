package folki.dev;

import java.time.LocalDateTime;


public class Link {
    public String originURL;
    public String shortURL;
    public LocalDateTime creationDateTime;
    public LocalDateTime expirationDateTime;

    public Link(String originURL, String shortURL) {
        this.originURL = originURL;
        this.shortURL = shortURL;
        this.creationDateTime = LocalDateTime.now();
        this.expirationDateTime = LocalDateTime.now().plusDays(1);
    }
}
