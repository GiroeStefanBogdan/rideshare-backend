package com.example.blablacar.controller;

import com.example.blablacar.dto.ride.MyRidesResponseDTO;
import com.example.blablacar.exception.GlobalExceptionHandler;
import com.example.blablacar.exception.ride.BookingCancellationExpiredException;
import com.example.blablacar.exception.ride.BookingNotFoundException;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserPrincipal;
import com.example.blablacar.service.ride.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyRidesContractTest {
    private final RideService service = mock(RideService.class);
    private final User user = new User();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new RideController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(final MethodParameter parameter) {
                        return parameter.getParameterType() == UserPrincipal.class;
                    }

                    @Override
                    public Object resolveArgument(final MethodParameter parameter,
                                                  final ModelAndViewContainer container,
                                                  final NativeWebRequest request,
                                                  final WebDataBinderFactory factory) {
                        return new UserPrincipal(user);
                    }
                }).build();
    }

    @Test
    void listsAuthenticatedUsersFourBuckets() throws Exception {
        when(service.getMyRides(user)).thenReturn(new MyRidesResponseDTO(
                List.of(), List.of(), List.of(), List.of()));
        mvc.perform(get("/rides/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingBookings").isEmpty())
                .andExpect(jsonPath("$.pastBookings").isEmpty())
                .andExpect(jsonPath("$.upcomingHostedRides").isEmpty())
                .andExpect(jsonPath("$.pastHostedRides").isEmpty());
        verify(service).getMyRides(user);
    }

    @Test
    void cancelsBookingAndMapsFailures() throws Exception {
        mvc.perform(delete("/rides/me/bookings/31")).andExpect(status().isNoContent());
        verify(service).cancelBooking(user, 31L);
        doThrow(new BookingNotFoundException()).when(service).cancelBooking(user, 32L);
        mvc.perform(delete("/rides/me/bookings/32")).andExpect(status().isNotFound());
        doThrow(new BookingCancellationExpiredException()).when(service).cancelBooking(user, 33L);
        mvc.perform(delete("/rides/me/bookings/33")).andExpect(status().isConflict());
    }

    @Test
    void rejectsMissingStopTimesAndNullStopsBeforeService() throws Exception {
        for (String stops : List.of("null,null", """
                {"id":1,"type":"ADMIN_UNIT","stopOrder":1,"cumulativePricePerSeat":0},
                {"id":2,"type":"ADMIN_UNIT","stopOrder":2,"cumulativePricePerSeat":10}
                """)) {
            mvc.perform(post("/rides").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"seatsTotal\":3,\"rideStops\":[" + stops + "]}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
