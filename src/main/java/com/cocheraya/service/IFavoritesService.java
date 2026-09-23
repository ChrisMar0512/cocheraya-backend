package com.cocheraya.service;

import com.cocheraya.dto.ParkingSpaceResponse;
import java.util.List;

public interface IFavoritesService {
    void addFavorite(Long parkingSpaceId);
    void removeFavorite(Long parkingSpaceId);
    List<ParkingSpaceResponse> getFavorites();
}
