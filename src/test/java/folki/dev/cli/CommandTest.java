package folki.dev.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import folki.dev.core.exceptions.ValidationError;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandTest {
    @Test
    void parsesNameArgumentsAndOptions() {
        Command command = Command.parse("  EDIT  aB3x9Z --limit 20 --ttl 2h ");

        assertEquals("edit", command.name());
        assertEquals(List.of("aB3x9Z"), command.args());
        assertEquals(20, command.intOption("limit"));
        assertEquals(Duration.ofHours(2), command.durationOption("ttl"));
    }

    @Test
    void missingOptionsAreNull() {
        Command command = Command.parse("create https://example.com");

        assertNull(command.intOption("limit"));
        assertNull(command.durationOption("ttl"));
    }

    @Test
    void parsesDurationSuffixes() {
        assertEquals(Duration.ofSeconds(90), Command.parse("x --ttl 90").durationOption("ttl"));
        assertEquals(Duration.ofSeconds(15), Command.parse("x --ttl 15s").durationOption("ttl"));
        assertEquals(Duration.ofMinutes(30), Command.parse("x --ttl 30m").durationOption("ttl"));
        assertEquals(Duration.ofDays(7), Command.parse("x --ttl 7d").durationOption("ttl"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x --limit abc", "x --limit -1", "x --limit 99999999999", "x --limit 1.5"})
    void rejectsBadLimit(String line) {
        Command command = Command.parse(line);
        assertThrows(ValidationError.class, () -> command.intOption("limit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x --ttl 1w", "x --ttl -5", "x --ttl 99999999999999999999d"})
    void rejectsBadTtl(String line) {
        Command command = Command.parse(line);
        assertThrows(ValidationError.class, () -> command.durationOption("ttl"));
    }

    @Test
    void optionWithoutValueIsAnError() {
        assertThrows(ValidationError.class, () -> Command.parse("create https://example.com --limit"));
        assertThrows(ValidationError.class, () -> Command.parse("create https://example.com --limit --ttl 5"));
    }

    @Test
    void unknownOptionAndMissingArgumentAreReported() {
        Command command = Command.parse("create --limt 5");

        assertThrows(ValidationError.class, () -> command.allowOnly(Set.of("limit", "ttl"), "usage"));
        assertThrows(ValidationError.class, () -> command.requireArg(0, "usage"));
    }
}
