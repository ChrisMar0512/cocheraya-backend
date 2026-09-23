package com.cocheraya.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckInRequest {

    
    @NotBlank(message = "El código QR es obligatorio")
    private String code;
}
