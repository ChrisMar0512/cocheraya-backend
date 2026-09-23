package com.cocheraya.controller;

import com.cocheraya.dto.ChatMessageRequest;
import com.cocheraya.dto.ChatMessageResponse;
import com.cocheraya.service.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservations/{reservationId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long reservationId) {
        return ResponseEntity.ok(chatService.getMessages(reservationId));
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long reservationId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        ChatMessageResponse response = chatService.sendMessage(reservationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
