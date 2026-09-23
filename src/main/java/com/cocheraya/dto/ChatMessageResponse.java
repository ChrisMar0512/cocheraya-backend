package com.cocheraya.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long reservationId;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private String content;
    private LocalDateTime createdAt;
}
