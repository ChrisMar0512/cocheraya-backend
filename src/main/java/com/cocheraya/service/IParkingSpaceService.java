package com.cocheraya.service;

import com.cocheraya.dto.CreateParkingSpaceRequest;
import com.cocheraya.dto.HostDashboardResponse;
import com.cocheraya.dto.ParkingSpaceResponse;
import com.cocheraya.dto.UpdateParkingSpaceRequest;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IParkingSpaceService {
    ParkingSpaceResponse createParkingSpace(CreateParkingSpaceRequest request, MultipartFile photo) throws Exception;
    ParkingSpaceResponse updateParkingSpace(Long id, UpdateParkingSpaceRequest request, MultipartFile photo) throws Exception;
    ParkingSpaceResponse updateAvailability(Long id, ParkingSpaceStatus newStatus);
    List<ParkingSpaceResponse> searchNearby(double lat, double lng, double radiusMeters);
    List<ParkingSpaceResponse> getMyParkingSpaces();
    HostDashboardResponse getHostDashboard(Long hostId);
    ParkingSpaceResponse getParkingSpaceById(Long id);
    void deleteParkingSpace(Long id);
}
