package com.example.blablacar.dto.ride;

import com.example.blablacar.model.location.LocationType;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RideSearchRequestDTOTest {

    private final Validator validator;

    RideSearchRequestDTOTest() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    private RideSearchRequestDTO buildRequest(final Integer maxPrice) {
        return new RideSearchRequestDTO(
                1L, LocationType.ADMIN_UNIT, 2L, LocationType.ADMIN_UNIT,
                LocalDate.now().plusDays(1), 2,
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

        assertTrue(violations.isEmpty(), "Zero maxPrice must be valid");
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
