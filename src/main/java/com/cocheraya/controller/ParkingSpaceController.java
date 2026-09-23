package com.cocheraya.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cocheraya.dto.*;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.User;
import com.cocheraya.service.IParkingSpaceService;
import com.cocheraya.service.IFavoritesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/parking-spaces")
@RequiredArgsConstructor
public class ParkingSpaceController {

    private final IParkingSpaceService parkingSpaceService;
    private final IFavoritesService favoritesService;
    private final ObjectMapper objectMapper;

    

    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ParkingSpaceResponse> createParkingSpace(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        CreateParkingSpaceRequest request = objectMapper.readValue(
                dataJson, CreateParkingSpaceRequest.class);
        ParkingSpaceResponse response = parkingSpaceService.createParkingSpace(request, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ParkingSpaceResponse> updateParkingSpace(
            @PathVariable Long id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        UpdateParkingSpaceRequest request = objectMapper.readValue(
                dataJson, UpdateParkingSpaceRequest.class);
        ParkingSpaceResponse response = parkingSpaceService.updateParkingSpace(id, request, photo);
        return ResponseEntity.ok(response);
    }

    /**
     * Cambia el estado de disponibilidad de una cochera.
     * Solo el HOST dueño puede cambiar el estado.
     * Emite evento WebSocket al cambiar.
     *
     * @param id      ID de la cochera
     * @param request objeto con el nuevo status
     * @return 200 OK con los datos actualizados
     */
    @PutMapping("/{id}/change-availability")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<ParkingSpaceResponse> updateAvailability(
            @PathVariable Long id,
            @RequestBody UpdateAvailabilityRequest request) {
        ParkingSpaceResponse response = parkingSpaceService.updateAvailability(
                id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/mine")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<List<ParkingSpaceResponse>> getMyParkingSpaces() {
        return ResponseEntity.ok(parkingSpaceService.getMyParkingSpaces());
    }

    
    @GetMapping("/host-dashboard")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<HostDashboardResponse> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User host = (User) auth.getPrincipal();
        return ResponseEntity.ok(parkingSpaceService.getHostDashboard(host.getId()));
    }

    

    
    @GetMapping("/search-nearby")
    public ResponseEntity<List<ParkingSpaceResponse>> searchNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000.0") double radius) {
        return ResponseEntity.ok(parkingSpaceService.searchNearby(lat, lng, radius));
    }

    

    
    @PostMapping("/{id}/favorites")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> addFavorite(@PathVariable Long id) {
        favoritesService.addFavorite(id);
        return ResponseEntity.ok().build();
    }

    
    @DeleteMapping("/{id}/favorites")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long id) {
        favoritesService.removeFavorite(id);
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping("/favorites")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<ParkingSpaceResponse>> getFavorites() {
        return ResponseEntity.ok(favoritesService.getFavorites());
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceResponse> getParkingSpace(@PathVariable Long id) {
        return ResponseEntity.ok(parkingSpaceService.getParkingSpaceById(id));
    }

    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> deleteParkingSpace(@PathVariable Long id) {
        parkingSpaceService.deleteParkingSpace(id);
        return ResponseEntity.noContent().build();
    }
}
