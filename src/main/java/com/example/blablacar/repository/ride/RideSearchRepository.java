package com.example.blablacar.repository.ride;

import com.example.blablacar.dto.ride.RideSearchRequestDTO;
import com.example.blablacar.dto.ride.RideSearchResultDTO;

import java.util.List;

public interface RideSearchRepository {

    List<RideSearchResultDTO> searchRides(RideSearchRequestDTO request,
                                          double fromLat, double fromLon,
                                          double toLat, double toLon);
}
