package com.example.avatar;

public final class InfraiException extends IOExceptionBase {
    public InfraiException(String message, int status) {
        super(message, status);
    }
}
