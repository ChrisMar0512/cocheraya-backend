package com.cocheraya.service;

import com.cocheraya.dto.CheckInResponse;
import com.cocheraya.dto.CheckOutResponse;
import com.cocheraya.dto.QRResponse;

public interface ICheckInOutService {
    QRResponse generateQR(Long reservationId);
    CheckInResponse checkIn(String code);
    CheckOutResponse checkOut(String code);
}
