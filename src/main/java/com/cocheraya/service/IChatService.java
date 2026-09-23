package com.cocheraya.service;

import com.cocheraya.dto.ChatMessageRequest;
import com.cocheraya.dto.ChatMessageResponse;
import java.util.List;

public interface IChatService {
    List<ChatMessageResponse> getMessages(Long reservationId);
    ChatMessageResponse sendMessage(Long reservationId, ChatMessageRequest request);
}
