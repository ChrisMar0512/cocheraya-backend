package com.cocheraya.service;

import com.cocheraya.dto.ChatMessageRequest;
import com.cocheraya.dto.ChatMessageResponse;
import com.cocheraya.entity.ChatMessage;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.User;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.exception.UnauthorizedOperationException;
import com.cocheraya.repository.ChatMessageRepository;
import com.cocheraya.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService implements IChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ReservationRepository reservationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long reservationId) {
        User user = getAuthenticatedUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        validateAccess(reservation, user);

        List<ChatMessage> messages = chatMessageRepository.findByReservationIdOrderByCreatedAtAsc(reservationId);
        return messages.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public ChatMessageResponse sendMessage(Long reservationId, ChatMessageRequest request) {
        User user = getAuthenticatedUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        validateAccess(reservation, user);

        ChatMessage message = new ChatMessage();
        message.setReservation(reservation);
        message.setSender(user);
        message.setContent(request.getContent());

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageResponse response = convertToResponse(savedMessage);

        // Broadcast to WebSocket subscribers for this reservation
        messagingTemplate.convertAndSend("/topic/chat/" + reservationId, response);

        return response;
    }

    private void validateAccess(Reservation reservation, User user) {
        boolean isDriver = reservation.getDriver().getId().equals(user.getId());
        boolean isHost = reservation.getParkingSpace().getHost().getId().equals(user.getId());

        if (!isDriver && !isHost) {
            throw new UnauthorizedOperationException("No tienes permiso para ver o participar en este chat");
        }
    }

    private ChatMessageResponse convertToResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setReservationId(message.getReservation().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getName());
        response.setSenderEmail(message.getSender().getEmail());
        response.setContent(message.getContent());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
