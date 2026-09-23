package com.cocheraya.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {
    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Size(max = 1000, message = "El mensaje no puede exceder los 1000 caracteres")
    private String content;
}
