package com.example.avatar;

public class IOExceptionBase extends java.io.IOException {
    private final int status;

    public IOExceptionBase(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
