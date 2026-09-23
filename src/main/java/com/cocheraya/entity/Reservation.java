package com.cocheraya.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "reservation",
    indexes = {
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_expires_at", columnList = "expires_at")
    }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    
    @Column(name = "start_time")
    private LocalDateTime startTime;

    
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Momento en que la reserva fue creada.
     * Asignado automáticamente por Hibernate con @CreationTimestamp.
     */
    @CreationTimestamp
    @Column(name = "reserved_at", updatable = false)
    private LocalDateTime reservedAt;

    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    

    public enum ReservationStatus {
        
        PENDING,
        
        ACTIVE,
        
        FINISHED,
        
        EXPIRED
    }
}
