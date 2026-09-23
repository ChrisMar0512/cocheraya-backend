package com.cocheraya.service;

import com.cocheraya.dto.*;
import com.cocheraya.entity.*;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;
    @Mock
    private FeatureRepository featureRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private ParkingSpaceService parkingSpaceService;

    private User host;
    private User driver;
    private ParkingSpace parkingSpace;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId(2L);
        host.setName("Christian Host");
        host.setEmail("host@cocheraya.com");
        host.setRole(User.Role.HOST);

        driver = new User();
        driver.setId(1L);
        driver.setName("Christian Driver");
        driver.setEmail("driver@cocheraya.com");
        driver.setRole(User.Role.DRIVER);

        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(-12.122, -77.028));
        point.setSRID(4326);

        parkingSpace = new ParkingSpace();
        parkingSpace.setId(10L);
        parkingSpace.setHost(host);
        parkingSpace.setTitle("Estacionamiento Miraflores");
        parkingSpace.setAddress("Av. Larco 123");
        parkingSpace.setPricePerHour(new BigDecimal("10.00"));
        parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        parkingSpace.setLocation(point);

        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthentication(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("shouldCreateParkingSpaceSuccessfully")
    void shouldCreateParkingSpaceSuccessfully() {
        // Arrange
        mockAuthentication(host);
        CreateParkingSpaceRequest request = new CreateParkingSpaceRequest();
        request.setTitle("Estacionamiento Miraflores");
        request.setAddress("Av. Larco 123");
        request.setPricePerHour(new BigDecimal("10.00"));
        request.setLatitude(-77.028);
        request.setLongitude(-12.122);
        request.setFeatureIds(List.of(1L));

        Feature feature = new Feature();
        feature.setId(1L);
        feature.setName("Techado");

        when(featureRepository.findAllById(any())).thenReturn(List.of(feature));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenReturn(parkingSpace);

        // Act
        ParkingSpaceResponse response = parkingSpaceService.createParkingSpace(request, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Estacionamiento Miraflores");
        verify(parkingSpaceRepository).save(any(ParkingSpace.class));
    }

    @Test
    @DisplayName("shouldGetParkingSpaceById")
    void shouldGetParkingSpaceById() {
        // Arrange
        when(parkingSpaceRepository.findById(10L)).thenReturn(Optional.of(parkingSpace));

        // Act
        ParkingSpaceResponse response = parkingSpaceService.getParkingSpaceById(10L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Estacionamiento Miraflores");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundExceptionWhenParkingSpaceNotFound")
    void shouldThrowResourceNotFoundExceptionWhenParkingSpaceNotFound() {
        // Arrange
        when(parkingSpaceRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> parkingSpaceService.getParkingSpaceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cochera no encontrada con id: 99");
    }

    @Test
    @DisplayName("shouldDeleteParkingSpaceSuccessfully")
    void shouldDeleteParkingSpaceSuccessfully() {
        // Arrange
        mockAuthentication(host);
        when(parkingSpaceRepository.findById(10L)).thenReturn(Optional.of(parkingSpace));
        when(reservationRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        parkingSpaceService.deleteParkingSpace(10L);

        // Assert
        verify(parkingSpaceRepository).delete(parkingSpace);
    }

    @Test
    @DisplayName("shouldThrowIllegalStateExceptionWhenDeletingSpaceWithActiveReservations")
    void shouldThrowIllegalStateExceptionWhenDeletingSpaceWithActiveReservations() {
        // Arrange
        mockAuthentication(host);
        when(parkingSpaceRepository.findById(10L)).thenReturn(Optional.of(parkingSpace));

        Reservation activeReservation = new Reservation();
        activeReservation.setParkingSpace(parkingSpace);
        activeReservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        when(reservationRepository.findAll()).thenReturn(List.of(activeReservation));

        // Act & Assert
        assertThatThrownBy(() -> parkingSpaceService.deleteParkingSpace(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede eliminar la cochera porque tiene reservas activas o pendientes");

        verify(parkingSpaceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("shouldThrowAccessDeniedExceptionWhenUpdatingOthersParkingSpace")
    void shouldThrowAccessDeniedExceptionWhenUpdatingOthersParkingSpace() {
        // Arrange
        User anotherHost = new User();
        anotherHost.setId(3L);
        anotherHost.setEmail("another_host@cocheraya.com");
        anotherHost.setName("Another Host");

        mockAuthentication(anotherHost);
        when(parkingSpaceRepository.findById(10L)).thenReturn(Optional.of(parkingSpace));

        UpdateParkingSpaceRequest request = new UpdateParkingSpaceRequest();
        request.setTitle("Nuevo Titulo");

        // Act & Assert
        assertThatThrownBy(() -> parkingSpaceService.updateParkingSpace(10L, request, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tienes permiso para modificar esta cochera");

        verify(parkingSpaceRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldUpdateAvailabilityAndEmitWebSocketEvent")
    void shouldUpdateAvailabilityAndEmitWebSocketEvent() {
        // Arrange
        mockAuthentication(host);
        when(parkingSpaceRepository.findById(10L)).thenReturn(Optional.of(parkingSpace));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenReturn(parkingSpace);

        // Act
        ParkingSpaceResponse response = parkingSpaceService.updateAvailability(10L, ParkingSpaceStatus.OCCUPIED);

        // Assert
        assertThat(response).isNotNull();
        verify(messagingTemplate).convertAndSend(eq("/topic/parking/10/availability"), eq("OCCUPIED"));
        verify(parkingSpaceRepository).save(parkingSpace);
        assertThat(parkingSpace.getStatus()).isEqualTo(ParkingSpaceStatus.OCCUPIED);
    }
}
