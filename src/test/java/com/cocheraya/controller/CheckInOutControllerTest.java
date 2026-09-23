package com.cocheraya.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cocheraya.dto.CheckInRequest;
import com.cocheraya.dto.CheckInResponse;
import com.cocheraya.dto.CheckOutRequest;
import com.cocheraya.dto.CheckOutResponse;
import com.cocheraya.dto.QRResponse;
import com.cocheraya.security.JwtAuthFilter;
import com.cocheraya.security.JwtService;
import com.cocheraya.service.ICheckInOutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckInOutController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckInOutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ICheckInOutService checkInOutService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldGenerateQRSuccessfully() throws Exception {
        QRResponse response = new QRResponse();
        response.setCode("test-qr-code");

        when(checkInOutService.generateQR(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in-out/1/qr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("test-qr-code"));
    }

    @Test
    void shouldCheckInSuccessfully() throws Exception {
        CheckInRequest request = new CheckInRequest();
        request.setCode("test-code");

        CheckInResponse response = new CheckInResponse();
        response.setMessage("Check-in exitoso");

        when(checkInOutService.checkIn(eq("test-code"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in-out/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Check-in exitoso"));
    }

    @Test
    void shouldCheckOutSuccessfully() throws Exception {
        CheckOutRequest request = new CheckOutRequest();
        request.setCode("test-code");

        CheckOutResponse response = new CheckOutResponse();
        response.setDurationMinutes(60L);

        when(checkInOutService.checkOut(eq("test-code"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in-out/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(60));
    }
}
