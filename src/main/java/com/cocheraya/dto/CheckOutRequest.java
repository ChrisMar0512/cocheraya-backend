package com.cocheraya.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckOutRequest {

    
    @NotBlank(message = "El código QR es obligatorio")
    private String code;
}
