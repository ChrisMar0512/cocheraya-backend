package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("ParkingSpaceRepository Tests")
class ParkingSpaceRepositoryTest {

    @Autowired
    private ParkingSpaceRepository parkingSpaceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User host;
    private GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        host = new User();
        host.setEmail("host@cocheraya.com");
        host.setPassword("password123");
        host.setName("Host User");
        host.setPhone("987654321");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);
        entityManager.persistAndFlush(host);
    }

    private Point createPoint(double longitude, double latitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    @Test
    @DisplayName("should find nearby spaces when within radius")
    void shouldFindNearbySpacesWhenWithinRadius() {
        // Miraflores, Lima coordinates (central point approx: -12.1221, -77.0298)
        ParkingSpace nearSpace = new ParkingSpace();
        nearSpace.setHost(host);
        nearSpace.setTitle("Near Space");
        nearSpace.setAddress("Calle Larco, Miraflores");
        nearSpace.setPricePerHour(new BigDecimal("5.00"));
        nearSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        nearSpace.setLocation(createPoint(-77.0298, -12.1221)); // 0m away
        entityManager.persistAndFlush(nearSpace);

        // Call repository method: buscar cocheras en radio de 1000m
        List<ParkingSpace> result = parkingSpaceRepository.findNearby(-12.1221, -77.0298, 1000.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Near Space");
    }

    @Test
    @DisplayName("should not find nearby spaces when outside radius")
    void shouldNotFindNearbySpacesWhenOutsideRadius() {
        // Space in Barranco (approx 3km away from Miraflores point)
        ParkingSpace farSpace = new ParkingSpace();
        farSpace.setHost(host);
        farSpace.setTitle("Far Space");
        farSpace.setAddress("Barranco");
        farSpace.setPricePerHour(new BigDecimal("5.00"));
        farSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        farSpace.setLocation(createPoint(-77.0220, -12.1480)); 
        entityManager.persistAndFlush(farSpace);

        // Find with 1km radius (should be empty)
        List<ParkingSpace> result = parkingSpaceRepository.findNearby(-12.1221, -77.0298, 1000.0);
        assertThat(result).isEmpty();

        // Find with 5km radius (should find it)
        List<ParkingSpace> result5k = parkingSpaceRepository.findNearby(-12.1221, -77.0298, 5000.0);
        assertThat(result5k).hasSize(1);
        assertThat(result5k.get(0).getTitle()).isEqualTo("Far Space");
    }

    @Test
    @DisplayName("should only find available spaces")
    void shouldOnlyFindAvailableSpaces() {
        ParkingSpace reservedSpace = new ParkingSpace();
        reservedSpace.setHost(host);
        reservedSpace.setTitle("Reserved Space");
        reservedSpace.setAddress("Larco");
        reservedSpace.setPricePerHour(new BigDecimal("5.00"));
        reservedSpace.setStatus(ParkingSpaceStatus.RESERVED);
        reservedSpace.setLocation(createPoint(-77.0298, -12.1221)); 
        entityManager.persistAndFlush(reservedSpace);

        List<ParkingSpace> result = parkingSpaceRepository.findNearby(-12.1221, -77.0298, 1000.0);
        assertThat(result).isEmpty();
    }
}
