package com.cocheraya.service;

import com.cocheraya.dto.ReservationResponse;
import java.util.List;

public interface IReservationService {
    ReservationResponse createReservation(Long parkingSpaceId);
    List<ReservationResponse> getMyReservations();
    ReservationResponse getReservationById(Long id);
    ReservationResponse cancelReservation(Long id);
    List<ReservationResponse> getReservationsByParkingSpace(Long parkingSpaceId);
}
