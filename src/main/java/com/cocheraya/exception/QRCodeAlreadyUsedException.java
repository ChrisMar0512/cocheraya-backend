package com.cocheraya.exception;

public class QRCodeAlreadyUsedException extends RuntimeException {
    public QRCodeAlreadyUsedException(String message) {
        super(message);
    }
}
