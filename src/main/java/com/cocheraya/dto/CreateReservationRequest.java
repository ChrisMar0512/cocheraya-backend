package com.cocheraya.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReservationRequest {

    
    @NotNull(message = "El parkingSpaceId es obligatorio")
    private Long parkingSpaceId;
}
