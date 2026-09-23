package com.cocheraya.dto;

import lombok.Data;

@Data
public class QRResponse {

    
    private String code;

    
    private String qrImageBase64;

    
    private Long reservationId;
}
