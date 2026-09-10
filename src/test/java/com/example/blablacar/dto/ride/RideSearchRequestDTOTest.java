package com.example.blablacar.dto.ride;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideSearchRequestDTOTest {

    private final Validator validator;

    RideSearchRequestDTOTest() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    private RideSearchRequestDTO buildRequest(final Integer maxPrice) {
        return new RideSearchRequestDTO(
                1L, "ADMIN_UNIT", 2L, "ADMIN_UNIT",
                LocalDate.of(2026, 5, 1), 2,
                null, null, maxPrice, null, null, null);
    }

    @Test
    void maxPriceMayBeAbsent() {
        final Set<ConstraintViolation<RideSearchRequestDTO>> violations =
                validator.validate(buildRequest(null));

        assertTrue(violations.isEmpty(), "Absent maxPrice must be valid");
    }

    @Test
    void maxPriceMayBeZero() {
        final Set<ConstraintViolation<RideSearchRequestDTO>> violations =
                validator.validate(buildRequest(0));

        assertFalse(violations.isEmpty()
                        && violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("maxPrice")),
                "Zero maxPrice must be valid");
    }

    @Test
    void maxPriceMayBePositive() {
        final Set<ConstraintViolation<RideSearchRequestDTO>> violations =
                validator.validate(buildRequest(50));

        assertTrue(violations.isEmpty(), "Positive maxPrice must be valid");
    }

    @Test
    void negativeMaxPriceIsInvalid() {
        final Set<ConstraintViolation<RideSearchRequestDTO>> violations =
                validator.validate(buildRequest(-1));

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("maxPrice")),
                "Negative maxPrice must be rejected");
    }
}
