package com.cocheraya.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cocheraya.dto.ParkingSpaceResponse;
import com.cocheraya.security.JwtAuthFilter;
import com.cocheraya.security.JwtService;
import com.cocheraya.service.IParkingSpaceService;
import com.cocheraya.service.IFavoritesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSpaceController.class)
@AutoConfigureMockMvc(addFilters = false) // Deshabilitar seguridad para pruebas del controller
class ParkingSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IParkingSpaceService parkingSpaceService;

    @MockBean
    private IFavoritesService favoritesService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("shouldGetMyParkingSpacesSuccessfully")
    void shouldGetMyParkingSpaces() throws Exception {
        ParkingSpaceResponse response = new ParkingSpaceResponse();
        response.setId(10L);
        response.setTitle("Cochera Test");
        response.setPricePerHour(new BigDecimal("12.50"));

        when(parkingSpaceService.getMyParkingSpaces()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/parking-spaces/mine")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].title").value("Cochera Test"))
                .andExpect(jsonPath("$[0].pricePerHour").value(12.50));
    }

    @Test
    @DisplayName("shouldGetParkingSpaceDetailSuccessfully")
    void shouldGetParkingSpaceDetailSuccessfully() throws Exception {
        ParkingSpaceResponse response = new ParkingSpaceResponse();
        response.setId(10L);
        response.setTitle("Cochera Test");
        response.setPricePerHour(new BigDecimal("12.50"));

        when(parkingSpaceService.getParkingSpaceById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/parking-spaces/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Cochera Test"));
    }

    @Test
    @DisplayName("shouldDeleteParkingSpaceSuccessfully")
    void shouldDeleteParkingSpaceSuccessfully() throws Exception {
        doNothing().when(parkingSpaceService).deleteParkingSpace(10L);

        mockMvc.perform(delete("/api/v1/parking-spaces/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(parkingSpaceService).deleteParkingSpace(10L);
    }

    @Test
    @DisplayName("shouldSearchNearbySpacesSuccessfully")
    void shouldSearchNearbySpacesSuccessfully() throws Exception {
        ParkingSpaceResponse response = new ParkingSpaceResponse();
        response.setId(10L);
        response.setTitle("Cochera Test Cerca");
        response.setLatitude(-12.1);
        response.setLongitude(-77.0);

        when(parkingSpaceService.searchNearby(-12.1, -77.0, 1000.0)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/parking-spaces/search-nearby")
                        .param("lat", "-12.1")
                        .param("lng", "-77.0")
                        .param("radius", "1000.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cochera Test Cerca"));
    }
}
