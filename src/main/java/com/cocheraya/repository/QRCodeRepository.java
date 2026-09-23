package com.cocheraya.repository;

import com.cocheraya.entity.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {

    
    Optional<QRCode> findByCode(String code);

    
    Optional<QRCode> findByReservationId(Long reservationId);
}
