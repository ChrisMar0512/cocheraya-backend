package com.cocheraya.service;

import com.cocheraya.dto.*;
import com.cocheraya.dto.HostDashboardResponse.ReservationSummary;
import com.cocheraya.entity.*;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio central para la gestión de cocheras (ParkingSpace) en CocheraYa.
 *
 * Responsabilidades:
 * - CRUD de cocheras con control de acceso por host
 * - Integración con Cloudinary para gestión de fotos
 * - Búsqueda espacial por radio usando PostGIS via ParkingSpaceRepository
 * - Gestión de favoritos de conductores (ahora delegado a FavoritesService)
 * - Dashboard de métricas para el host
 * - Emisión de eventos WebSocket al cambiar disponibilidad
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSpaceService implements IParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ReviewRepository reviewRepository;
    private final CloudinaryService cloudinaryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    

    /**
     * Crea una nueva cochera para el host autenticado.
     * Si se provee foto, la sube a Cloudinary y guarda URL + publicId.
     * Construye el Point geográfico con SRID 4326 (WGS84/GPS).
     *
     * @param request datos de la cochera a crear
     * @param photo   foto opcional de la cochera
     * @return respuesta con los datos de la cochera creada
     */
    public ParkingSpaceResponse createParkingSpace(
            CreateParkingSpaceRequest request,
            MultipartFile photo
    ) {
        User host = getAuthenticatedUser();

        ParkingSpace space = new ParkingSpace();
        space.setHost(host);
        space.setTitle(request.getTitle());
        space.setDescription(request.getDescription());
        space.setAddress(request.getAddress());
        space.setPricePerHour(request.getPricePerHour());

        // Construir el punto geográfico WGS84 — longitud=X, latitud=Y
        space.setLocation(buildPoint(request.getLongitude(), request.getLatitude()));

        
        if (photo != null && !photo.isEmpty()) {
            String[] uploadResult = cloudinaryService.uploadImageWithPublicId(photo);
            space.setPhotoUrl(uploadResult[0]);
            space.setCloudinaryPublicId(uploadResult[1]);
        }

        
        if (request.getFeatureIds() != null && !request.getFeatureIds().isEmpty()) {
            Set<Feature> features = new HashSet<>(
                    featureRepository.findAllById(request.getFeatureIds())
            );
            space.setFeatures(features);
        }

        ParkingSpace saved = parkingSpaceRepository.save(space);
        log.info("Cochera creada: id={}, host={}", saved.getId(), host.getEmail());
        return mapToResponse(saved);
    }

    
    public ParkingSpaceResponse updateParkingSpace(
            Long id,
            UpdateParkingSpaceRequest request,
            MultipartFile photo
    ) {
        ParkingSpace space = findSpaceAndVerifyOwnership(id);

        
        if (request.getTitle() != null)        space.setTitle(request.getTitle());
        if (request.getDescription() != null)  space.setDescription(request.getDescription());
        if (request.getAddress() != null)      space.setAddress(request.getAddress());
        if (request.getPricePerHour() != null) space.setPricePerHour(request.getPricePerHour());

        
        if (request.getLatitude() != null && request.getLongitude() != null) {
            space.setLocation(buildPoint(request.getLongitude(), request.getLatitude()));
        }

        
        if (request.getFeatureIds() != null) {
            Set<Feature> features = new HashSet<>(
                    featureRepository.findAllById(request.getFeatureIds())
            );
            space.setFeatures(features);
        }

        
        if (photo != null && !photo.isEmpty()) {
            
            if (space.getCloudinaryPublicId() != null) {
                cloudinaryService.deleteImage(space.getCloudinaryPublicId());
                log.info("Foto anterior eliminada de Cloudinary: {}", space.getCloudinaryPublicId());
            }
            
            String[] uploadResult = cloudinaryService.uploadImageWithPublicId(photo);
            space.setPhotoUrl(uploadResult[0]);
            space.setCloudinaryPublicId(uploadResult[1]);
        }

        ParkingSpace updated = parkingSpaceRepository.save(space);
        log.info("Cochera actualizada: id={}", updated.getId());
        return mapToResponse(updated);
    }

    /**
     * Cambia el estado de disponibilidad de una cochera.
     * Solo el host dueño puede cambiar el estado.
     * Emite un evento WebSocket al topic /topic/parking/{id}/availability
     * para que los clientes conectados sean notificados en tiempo real.
     *
     * @param id        ID de la cochera
     * @param newStatus nuevo estado (AVAILABLE, RESERVED, OCCUPIED)
     * @return respuesta actualizada
     */
    public ParkingSpaceResponse updateAvailability(Long id, ParkingSpaceStatus newStatus) {
        ParkingSpace space = findSpaceAndVerifyOwnership(id);
        ParkingSpaceStatus oldStatus = space.getStatus();

        space.setStatus(newStatus);
        ParkingSpace updated = parkingSpaceRepository.save(space);

        // Emitir evento WebSocket para notificar clientes en tiempo real
        messagingTemplate.convertAndSend(
                "/topic/parking/" + id + "/availability",
                newStatus.name()
        );

        log.info("Disponibilidad de cochera {} cambiada: {} -> {}", id, oldStatus, newStatus);
        return mapToResponse(updated);
    }

    

    
    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> searchNearby(double lat, double lng, double radiusMeters) {
        log.info("Búsqueda de cocheras: lat={}, lng={}, radio={}m", lat, lng, radiusMeters);
        return parkingSpaceRepository.findNearby(lat, lng, radiusMeters)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    

    
    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> getMyParkingSpaces() {
        User host = getAuthenticatedUser();
        return parkingSpaceRepository.findByHostId(host.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    

    
    @Transactional(readOnly = true)
    public HostDashboardResponse getHostDashboard(Long hostId) {
        HostDashboardResponse dashboard = new HostDashboardResponse();

        
        dashboard.setTotalSpaces(parkingSpaceRepository.countByHostId(hostId));

        
        dashboard.setTotalReservationsCompleted(
                reservationRepository.countFinishedByHostId(hostId)
        );

        
        BigDecimal earnings = walletTransactionRepository.sumEarningsByHostId(hostId);
        dashboard.setTotalEarnings(earnings != null ? earnings : BigDecimal.ZERO);

        
        dashboard.setAverageRating(reviewRepository.averageRatingByHostId(hostId));

        
        List<ReservationSummary> recentReservations = reservationRepository
                .findRecentByHostId(hostId, 5)
                .stream()
                .map(this::mapToReservationSummary)
                .collect(Collectors.toList());
        dashboard.setRecentReservations(recentReservations);

        log.info("Dashboard generado para host id={}", hostId);
        return dashboard;
    }

    
    @Transactional(readOnly = true)
    public ParkingSpaceResponse getParkingSpaceById(Long id) {
        ParkingSpace space = findSpaceById(id);
        return mapToResponse(space);
    }

    
    public void deleteParkingSpace(Long id) {
        ParkingSpace space = findSpaceAndVerifyOwnership(id);

        
        long activeReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getParkingSpace().getId().equals(id) && 
                        (r.getStatus() == Reservation.ReservationStatus.PENDING || 
                         r.getStatus() == Reservation.ReservationStatus.ACTIVE))
                .count();

        if (activeReservations > 0) {
            throw new IllegalStateException("No se puede eliminar la cochera porque tiene reservas activas o pendientes.");
        }

        
        space.getFavoritedBy().clear();
        parkingSpaceRepository.save(space);

        
        if (space.getCloudinaryPublicId() != null) {
            cloudinaryService.deleteImage(space.getCloudinaryPublicId());
            log.info("Foto de cochera eliminada de Cloudinary: {}", space.getCloudinaryPublicId());
        }

        parkingSpaceRepository.delete(space);
        log.info("Cochera de baja id={}", id);
    }

    

    
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado: " + email));
    }

    
    private ParkingSpace findSpaceById(Long id) {
        return parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cochera no encontrada con id: " + id));
    }

    
    private ParkingSpace findSpaceAndVerifyOwnership(Long id) {
        ParkingSpace space = findSpaceById(id);
        User currentUser = getAuthenticatedUser();

        if (!space.getHost().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "No tienes permiso para modificar esta cochera"
            );
        }
        return space;
    }

    /**
     * Construye un Point de JTS con SRID 4326 (WGS84).
     * Nota: en WGS84, X = longitud, Y = latitud.
     *
     * @param longitude longitud GPS (eje X)
     * @param latitude  latitud GPS (eje Y)
     * @return Point con SRID 4326
     */
    private Point buildPoint(double longitude, double latitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        // SRID 4326 = WGS84, el sistema de referencia estándar de GPS
        point.setSRID(4326);
        return point;
    }

    
    private ParkingSpaceResponse mapToResponse(ParkingSpace space) {
        ParkingSpaceResponse response = modelMapper.map(space, ParkingSpaceResponse.class);
        response.setHostName(space.getHost().getName());
        response.setFavoritesCount(space.getFavoritedBy().size());

        
        if (space.getLocation() != null) {
            response.setLatitude(space.getLocation().getY());   
            response.setLongitude(space.getLocation().getX());  
        }

        
        List<String> featureNames = space.getFeatures()
                .stream()
                .map(Feature::getName)
                .collect(Collectors.toList());
        response.setFeatures(featureNames);

        return response;
    }

    
    private ReservationSummary mapToReservationSummary(Reservation reservation) {
        ReservationSummary summary = new ReservationSummary();
        summary.setReservationId(reservation.getId());
        summary.setDriverName(reservation.getDriver().getName());
        summary.setParkingSpaceTitle(reservation.getParkingSpace().getTitle());
        summary.setStatus(reservation.getStatus().name());
        summary.setCreatedAt(reservation.getCreatedAt());
        summary.setEndTime(reservation.getEndTime());
        summary.setPricePerHour(reservation.getParkingSpace().getPricePerHour());
        return summary;
    }

}
