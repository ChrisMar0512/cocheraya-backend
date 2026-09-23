package com.cocheraya.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cocheraya.dto.CreateReviewRequest;
import com.cocheraya.dto.ReviewResponse;
import com.cocheraya.security.JwtAuthFilter;
import com.cocheraya.security.JwtService;
import com.cocheraya.service.IReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IReviewService reviewService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldCreateReviewSuccessfully() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setReservationId(1L);
        request.setRating(5);
        request.setComment("Excelente");

        ReviewResponse response = new ReviewResponse();
        response.setId(10L);
        response.setRating(5);
        response.setComment("Excelente");

        when(reviewService.createReview(any(CreateReviewRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.rating").value(5));
    }
}
