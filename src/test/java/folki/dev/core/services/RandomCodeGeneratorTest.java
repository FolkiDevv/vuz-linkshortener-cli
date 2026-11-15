package folki.dev.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomCodeGeneratorTest {
    private static final int SAMPLES = 1_000;

    @Test
    void generatesCodeOfConfiguredLengthFromBase62Alphabet() {
        RandomCodeGenerator generator = new RandomCodeGenerator(8);

        for (int i = 0; i < SAMPLES; i++) {
            String code = generator.generate();
            assertEquals(8, code.length());
            assertTrue(code.matches("[0-9A-Za-z]+"), "неожиданные символы в коде " + code);
        }
    }

    @Test
    void codesAreRandom() {
        RandomCodeGenerator generator = new RandomCodeGenerator(6);
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < SAMPLES; i++) {
            codes.add(generator.generate());
        }
        assertEquals(SAMPLES, codes.size());
    }

    @Test
    void sameSeedGivesSameCodes() {
        RandomCodeGenerator first = new RandomCodeGenerator(6, new Random(42));
        RandomCodeGenerator second = new RandomCodeGenerator(6, new Random(42));

        assertEquals(first.generate(), second.generate());
    }

    @Test
    void rejectsNonPositiveLength() {
        assertThrows(IllegalArgumentException.class, () -> new RandomCodeGenerator(0));
        assertThrows(IllegalArgumentException.class, () -> new RandomCodeGenerator(-1));
    }
}
