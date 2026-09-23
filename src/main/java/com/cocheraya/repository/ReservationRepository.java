package com.cocheraya.repository;

import com.cocheraya.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    

    
    List<Reservation> findByDriverId(Long driverId);

    
    List<Reservation> findByParkingSpaceId(Long parkingSpaceId);

    
    List<Reservation> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    

    /**
     * Busca reservas PENDING cuyo tiempo de expiración ya pasó.
     *
     * El scheduler ejecuta esta query cada 60 segundos para detectar reservas
     * donde el DRIVER no hizo check-in dentro de los 15 minutos permitidos.
     * Las reservas encontradas serán marcadas como EXPIRED y la cochera liberada.
     *
     * @param now momento actual para comparar contra expiresAt
     * @return lista de reservas pendientes que ya expiraron
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = com.cocheraya.entity.Reservation$ReservationStatus.PENDING
          AND r.expiresAt < :now
        """)
    List<Reservation> findExpiredPendingReservations(@Param("now") LocalDateTime now);

    
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = com.cocheraya.entity.Reservation$ReservationStatus.PENDING
          AND r.expiresAt BETWEEN :now AND :fiveMinutesLater
        """)
    List<Reservation> findPendingReservationsExpiringIn5Minutes(
            @Param("now") LocalDateTime now,
            @Param("fiveMinutesLater") LocalDateTime fiveMinutesLater
    );

    

    
    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.parkingSpace.host.id = :hostId
          AND r.status = com.cocheraya.entity.Reservation$ReservationStatus.FINISHED
        """)
    long countFinishedByHostId(@Param("hostId") Long hostId);

    
    @Query(
        value = """
            SELECT r.* FROM reservation r
            JOIN parking_space ps ON r.parking_space_id = ps.id
            WHERE ps.host_id = :hostId
            ORDER BY r.created_at DESC
            LIMIT :lim
            """,
        nativeQuery = true
    )
    List<Reservation> findRecentByHostId(
            @Param("hostId") Long hostId,
            @Param("lim") int lim
    );
}
