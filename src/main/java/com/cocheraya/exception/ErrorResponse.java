package com.cocheraya.exception;

import com.cocheraya.dto.ErrorResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse extends ErrorResponseDTO {

    public ErrorResponse(int status, String error, String message, String path) {
        super(status, error, message, path);
    }
}