package com.cocheraya.controller;

import com.cocheraya.dto.CreateReservationRequest;
import com.cocheraya.dto.ReservationResponse;
import com.cocheraya.service.IReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final IReservationService reservationService;

    

    /**
     * Crea una nueva reserva para el DRIVER autenticado.
     *
     * La cochera se bloquea con PESSIMISTIC_WRITE en el service para
     * evitar que dos drivers simultáneos reserven la misma cochera (race condition).
     *
     * @param request contiene el parkingSpaceId a reservar
     * @return 201 CREATED con los datos de la reserva creada
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationResponse response = reservationService.createReservation(
                request.getParkingSpaceId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    

    
    @GetMapping("/mine")
    public ResponseEntity<List<ReservationResponse>> getMyHistory() {
        return ResponseEntity.ok(reservationService.getMyReservations());
    }

    

    
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    

    /**
     * Cancela una reserva PENDING del DRIVER autenticado.
     * Libera la cochera y emite evento WebSocket.
     *
     * Solo se pueden cancelar reservas en estado PENDING.
     * El DRIVER debe ser el dueño de la reserva.
     *
     * @param id ID de la reserva a cancelar
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    

    
    @GetMapping("/by-parking-space/{parkingSpaceId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByParkingSpace(
            @PathVariable Long parkingSpaceId
    ) {
        return ResponseEntity.ok(
                reservationService.getReservationsByParkingSpace(parkingSpaceId)
        );
    }
}
