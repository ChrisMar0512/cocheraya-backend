package com.cocheraya.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReservationExpiredEvent extends ApplicationEvent {

    private final Long reservationId;
    private final Long parkingSpaceId;

    
    public ReservationExpiredEvent(Object source, Long reservationId, Long parkingSpaceId) {
        super(source);
        this.reservationId = reservationId;
        this.parkingSpaceId = parkingSpaceId;
    }
}
