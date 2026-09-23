package com.cocheraya.service;

import com.cocheraya.dto.ParkingSpaceResponse;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.User;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.repository.ParkingSpaceRepository;
import com.cocheraya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoritesService implements IFavoritesService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public void addFavorite(Long parkingSpaceId) {
        User user = getAuthenticatedUser();
        ParkingSpace space = parkingSpaceRepository.findById(parkingSpaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cochera no encontrada"));
        space.getFavoritedBy().add(user);
        parkingSpaceRepository.save(space);
    }

    @Override
    public void removeFavorite(Long parkingSpaceId) {
        User user = getAuthenticatedUser();
        ParkingSpace space = parkingSpaceRepository.findById(parkingSpaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cochera no encontrada"));
        space.getFavoritedBy().remove(user);
        parkingSpaceRepository.save(space);
    }

    @Override
    public List<ParkingSpaceResponse> getFavorites() {
        User user = getAuthenticatedUser();
        List<ParkingSpace> spaces = parkingSpaceRepository.findByFavoritedByContaining(user);
        return spaces.stream()
                .map(space -> {
                    ParkingSpaceResponse response = modelMapper.map(space, ParkingSpaceResponse.class);
                    response.setFavoritesCount(space.getFavoritedBy().size());
                    return response;
                })
                .toList();
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
