package com.cocheraya.repository;

import com.cocheraya.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
