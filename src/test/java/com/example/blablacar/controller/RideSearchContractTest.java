package com.example.blablacar.controller;

import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.model.location.LocationType;
import com.example.blablacar.service.ride.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RideSearchContractTest {
    private RideService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RideService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RideController(service)).build();
    }

    private String payload(final String fromType, final String toType, final LocalDate date,
                           final String filters) {
        return """
                {"fromId":3516,"fromType":"%s","toId":18949,"toType":"%s",
                 "date":"%s","seats":1%s}
                """.formatted(fromType, toType, date, filters);
    }

    private void search(final String body, final int expectedStatus) throws Exception {
        mvc.perform(post("/rides/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void acceptsFrontendAdministrativeUnitPayload() throws Exception {
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", LocalDate.now().plusDays(1), ""), 200);
        ArgumentCaptor<RideSearchRequestDTO> request = ArgumentCaptor.forClass(RideSearchRequestDTO.class);
        verify(service).searchRides(request.capture());
        assertEquals(LocationType.ADMIN_UNIT, request.getValue().fromType());
        assertEquals(LocationType.ADMIN_UNIT, request.getValue().toType());
    }

    @Test
    void acceptsStreetAndMixedEndpoints() throws Exception {
        search(payload("STREET", "ADMIN_UNIT", LocalDate.now().plusDays(1), ""), 200);
        search(payload("ADMIN_UNIT", "STREET", LocalDate.now().plusDays(1), ""), 200);
        search(payload("STREET", "STREET", LocalDate.now().plusDays(1), ""), 200);
    }

    @Test
    void rejectsSettlementCategoryAsEndpointType() throws Exception {
        search(payload("CITY", "ADMIN_UNIT", LocalDate.now().plusDays(1), ""), 400);
    }

    @Test
    void acceptsTodayAndRejectsYesterday() throws Exception {
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", LocalDate.now(), ""), 200);
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", LocalDate.now().minusDays(1), ""), 400);
    }

    @Test
    void validatesOptionalFilters() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", date,
                ",\"maxDistanceStart\":0,\"maxDistanceEnd\":0,\"maxPrice\":0"), 200);
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", date, ",\"maxDistanceStart\":-1"), 400);
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", date, ",\"maxDistanceEnd\":-1"), 400);
        search(payload("ADMIN_UNIT", "ADMIN_UNIT", date, ",\"timeWindow\":\"UNKNOWN\""), 400);
        for (String window : new String[] {"BEFORE_8", "8_12", "12_18", "AFTER_18"}) {
            search(payload("ADMIN_UNIT", "ADMIN_UNIT", date, ",\"timeWindow\":\"" + window + "\""), 200);
        }
    }
}
