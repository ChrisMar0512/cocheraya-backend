package com.cocheraya.repository;

import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad ParkingSpace.
 *
 * Incluye una consulta nativa con PostGIS para búsqueda espacial por radio:
 * - ST_DWithin con tipo ::geography convierte el radio a metros reales sobre
 *   la superficie esférica de la Tierra (más preciso que la distancia euclidiana).
 * - ST_SetSRID + ST_MakePoint construyen el punto de referencia con SRID 4326 (WGS84).
 */
@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    
    @Query(
        value = """
            SELECT * FROM parking_space
            WHERE status = 'AVAILABLE'
              AND ST_DWithin(
                    CAST(location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                    :radiusMeters
                  )
            """,
        nativeQuery = true
    )
    List<ParkingSpace> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters
    );

    
    List<ParkingSpace> findByHostId(Long hostId);

    
    long countByHostId(Long hostId);

    List<ParkingSpace> findByFavoritedByContaining(User user);

    boolean existsByTitle(String title);
}
