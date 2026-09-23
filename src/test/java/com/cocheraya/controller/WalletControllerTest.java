package com.cocheraya.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cocheraya.dto.TopUpRequest;
import com.cocheraya.dto.WalletResponse;
import com.cocheraya.exception.InvalidOperationException;
import com.cocheraya.security.JwtAuthFilter;
import com.cocheraya.security.JwtService;
import com.cocheraya.service.IWalletService;
import com.cocheraya.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IWalletService walletService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("driver@test.com");
        mockUser.setRole(User.Role.DRIVER);

        Authentication auth = new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldReturnBalanceSuccessfully() throws Exception {
        WalletResponse response = new WalletResponse();
        response.setBalance(new BigDecimal("100.00"));

        when(walletService.getBalance(any(Long.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void shouldTopUpSuccessfully() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(new BigDecimal("50.00"));

        WalletResponse response = new WalletResponse();
        response.setBalance(new BigDecimal("150.00"));

        when(walletService.topUp(any(Long.class), any(BigDecimal.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/wallet/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }
}
