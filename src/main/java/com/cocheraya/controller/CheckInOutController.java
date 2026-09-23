package com.cocheraya.controller;

import com.cocheraya.dto.CheckInRequest;
import com.cocheraya.dto.CheckInResponse;
import com.cocheraya.dto.CheckOutRequest;
import com.cocheraya.dto.CheckOutResponse;
import com.cocheraya.dto.QRResponse;
import com.cocheraya.service.ICheckInOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/check-in-out")
@RequiredArgsConstructor
public class CheckInOutController {

    private final ICheckInOutService checkInOutService;

    @PostMapping("/{reservationId}/qr")
    public ResponseEntity<QRResponse> generateQR(@PathVariable Long reservationId) {
        return ResponseEntity.ok(checkInOutService.generateQR(reservationId));
    }

    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(checkInOutService.checkIn(request.getCode()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<CheckOutResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ResponseEntity.ok(checkInOutService.checkOut(request.getCode()));
    }
}
