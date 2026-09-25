package com.cocheraya.service;

import com.cocheraya.dto.ParkingUpdateEvent;
import com.cocheraya.dto.ReservationResponse;
import com.cocheraya.dto.ReservationResponse.DriverInfo;
import com.cocheraya.dto.ReservationResponse.ParkingSpaceInfo;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.User;
import com.cocheraya.repository.ParkingSpaceRepository;
import com.cocheraya.repository.ReservationRepository;
import com.cocheraya.repository.UserRepository;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.exception.SpaceNotAvailableException;
import com.cocheraya.exception.InvalidOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de reservas de CocheraYa.
 *
 * Gestiona el ciclo de vida completo de una reserva:
 *   PENDING → ACTIVE → FINISHED  (flujo exitoso)
 *   PENDING → EXPIRED            (tiempo agotado o cancelación)
 *
 * CONCURRENCIA:
 *   createReservation usa PESSIMISTIC_WRITE lock a nivel de base de datos
 *   para evitar race conditions al reservar la misma cochera simultáneamente.
 *
 * WEBSOCKET:
 *   Se emiten eventos a /topic/parking-updates cada vez que una cochera
 *   cambia de estado, para que los clientes actualicen su mapa en tiempo real.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService implements IReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    /** Tiempo de expiración de una reserva PENDING (en minutos) */
    private static final int EXPIRATION_MINUTES = 15;
    private static final String PARKING_UPDATES_TOPIC = "/topic/parking-updates";

    

    /**
     * Crea una nueva reserva para el DRIVER autenticado.
     *
     * SEGURIDAD ANTE CONCURRENCIA (PESSIMISTIC_WRITE):
     *
     * Sin el bloqueo pesimista, dos requests HTTP simultáneos del mismo o diferentes
     * drivers podrían ejecutar esta secuencia en paralelo:
     *
     *   Thread A: lee ParkingSpace → status = AVAILABLE ✓ → continúa
     *   Thread B: lee ParkingSpace → status = AVAILABLE ✓ → continúa  (¡ANTES de que A persista!)
     *   Thread A: cambia status a RESERVED → guarda → crea Reservation 1
     *   Thread B: cambia status a RESERVED → guarda → crea Reservation 2  ← ¡DUPLICADO!
     *
     * Resultado: dos reservas activas para la misma cochera. Esto es un race condition
     * clásico en sistemas concurrentes. PESSIMISTIC_WRITE hace que PostgreSQL ejecute
     * un SELECT ... FOR UPDATE, bloqueando el registro de ParkingSpace a nivel de fila
     * en la BD. El Thread B queda esperando hasta que Thread A haga commit o rollback,
     * momento en el cual Thread B re-lee el registro y ahora ve status = RESERVED,
     * lanzando IllegalStateException correctamente.
     *
     * @param parkingSpaceId ID de la cochera a reservar
     * @return respuesta con los datos de la reserva creada
     * @throws IllegalStateException si la cochera no está disponible
     */
    @Transactional
    public ReservationResponse createReservation(Long parkingSpaceId) {
        User driver = getAuthenticatedUser();

        // PESSIMISTIC_WRITE → SELECT ... FOR UPDATE en PostgreSQL
        // Bloquea el registro a nivel de fila hasta que esta transacción termine.
        
        ParkingSpace space = entityManager.find(
                ParkingSpace.class, parkingSpaceId, LockModeType.PESSIMISTIC_WRITE
        );

        if (space == null) {
            throw new ResourceNotFoundException("Cochera no encontrada con id: " + parkingSpaceId);
        }

        
        
        if (space.getStatus() != ParkingSpaceStatus.AVAILABLE) {
            throw new SpaceNotAvailableException("La cochera no está disponible en este momento");
        }

        
        space.setStatus(ParkingSpaceStatus.RESERVED);
        entityManager.merge(space);

        // Crear la reserva con expiración en 15 minutos
        Reservation reservation = new Reservation();
        reservation.setDriver(driver);
        reservation.setParkingSpace(space);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));

        Reservation saved = reservationRepository.save(reservation);

        // Notificar clientes WebSocket del cambio de disponibilidad
        emitParkingUpdate(space.getId(), ParkingSpaceStatus.RESERVED.name());

        log.info("Reserva creada: id={}, driver={}, cochera={}, expira={}",
                saved.getId(), driver.getEmail(), space.getTitle(), saved.getExpiresAt());

        return mapToResponse(saved);
    }

    

    
    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        User driver = getAuthenticatedUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con id: " + reservationId
                ));

        
        if (!reservation.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException(
                    "No tienes permiso para cancelar esta reserva"
            );
        }

        
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException(
                    "No se puede cancelar una reserva con estado: " + reservation.getStatus() +
                    ". Solo se pueden cancelar reservas PENDING."
            );
        }

        
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);

        
        ParkingSpace space = reservation.getParkingSpace();
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        parkingSpaceRepository.save(space);

        // Notificar clientes WebSocket
        emitParkingUpdate(space.getId(), ParkingSpaceStatus.AVAILABLE.name());

        log.info("Reserva cancelada: id={}, driver={}, cochera liberada={}",
                reservationId, driver.getEmail(), space.getTitle());

        return mapToResponse(reservation);
    }

    

    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations() {
        User driver = getAuthenticatedUser();
        return reservationRepository.findByDriverIdOrderByCreatedAtDesc(driver.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reserva no encontrada con id: " + reservationId
                ));
        return mapToResponse(reservation);
    }

    
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByParkingSpace(Long parkingSpaceId) {
        return reservationRepository.findByParkingSpaceId(parkingSpaceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    

    
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario autenticado no encontrado: " + email
                ));
    }

    /**
     * Emite un evento WebSocket al topic /topic/parking-updates
     * para que los clientes refresquen el estado de la cochera en el mapa.
     *
     * @param parkingSpaceId ID de la cochera
     * @param newStatus      nuevo estado ("AVAILABLE", "RESERVED", "OCCUPIED")
     */
    private void emitParkingUpdate(Long parkingSpaceId, String newStatus) {
        ParkingUpdateEvent event = new ParkingUpdateEvent(parkingSpaceId, newStatus);
        messagingTemplate.convertAndSend(PARKING_UPDATES_TOPIC, event);
    }

    
    private ReservationResponse mapToResponse(Reservation reservation) {
        ReservationResponse response = modelMapper.map(reservation, ReservationResponse.class);

        
        ParkingSpace space = reservation.getParkingSpace();
        ParkingSpaceInfo spaceInfo = modelMapper.map(space, ParkingSpaceInfo.class);
        response.setParkingSpace(spaceInfo);

        
        User driver = reservation.getDriver();
        DriverInfo driverInfo = modelMapper.map(driver, DriverInfo.class);
        response.setDriver(driverInfo);

        return response;
    }
}
