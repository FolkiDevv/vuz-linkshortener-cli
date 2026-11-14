package folki.dev.core.services;

import java.security.SecureRandom;
import java.util.Random;

public final class RandomCodeGenerator implements CodeGenerator {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final int length;
    private final Random random;

    public RandomCodeGenerator(int length) {
        this(length, new SecureRandom());
    }

    public RandomCodeGenerator(int length, Random random) {
        if (length <= 0) {
            throw new IllegalArgumentException("Длина кода должна быть больше 0, получено: " + length);
        }
        this.length = length;
        this.random = random;
    }

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
