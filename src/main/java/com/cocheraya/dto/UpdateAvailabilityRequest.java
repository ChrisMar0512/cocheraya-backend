package com.cocheraya.dto;

import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import lombok.Data;

@Data
public class UpdateAvailabilityRequest {

    
    private ParkingSpaceStatus status;
}
