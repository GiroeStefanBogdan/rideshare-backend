package com.example.blablacar.service.user;

import com.example.blablacar.exception.user.InvalidAgeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrationServiceTest {

    private final RegistrationService service = new RegistrationService(null, null);

    @Test
    void validateAgeShouldThrowWhenNull() {
        assertThrows(InvalidAgeException.class, () -> service.validateAge(null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 17})
    void validateAgeShouldThrowWhenUnder18(final int age) {
        LocalDate birthday = LocalDate.now().minusYears(age).minusDays(1);
        assertThrows(InvalidAgeException.class, () -> service.validateAge(birthday));
    }

    @ParameterizedTest
    @ValueSource(ints = {18, 25, 99})
    void validateAgeShouldPassWhen18OrOlder(final int age) {
        LocalDate birthday = LocalDate.now().minusYears(age);
        assertDoesNotThrow(() -> service.validateAge(birthday));
    }
}
