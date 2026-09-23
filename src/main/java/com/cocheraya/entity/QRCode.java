package com.cocheraya.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_code")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QRCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    
    @Column(unique = true, nullable = false)
    private String code;

    
    @Column(name = "used_for_checkin", nullable = false)
    private Boolean usedForCheckin = false;

    
    @Column(name = "used_for_checkout", nullable = false)
    private Boolean usedForCheckout = false;

    /**
     * Momento de expiración del QR.
     * Se calcula como LocalDateTime.now().plusMinutes(30) al crear.
     * Si el DRIVER no escanea antes de esta hora, debe solicitar un nuevo QR.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
