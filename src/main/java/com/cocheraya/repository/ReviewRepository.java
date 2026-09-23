package com.cocheraya.repository;

import com.cocheraya.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    
    List<Review> findByParkingSpaceIdOrderByCreatedAtDesc(Long parkingSpaceId);

    
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    
    boolean existsByReservationIdAndReviewerId(Long reservationId, Long reviewerId);

    
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.parkingSpace.id = :parkingSpaceId
        """)
    Double averageRatingByParkingSpaceId(@Param("parkingSpaceId") Long parkingSpaceId);

    
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.parkingSpace.host.id = :hostId
        """)
    Double averageRatingByHostId(@Param("hostId") Long hostId);

    
    long countByParkingSpaceId(Long parkingSpaceId);
}
