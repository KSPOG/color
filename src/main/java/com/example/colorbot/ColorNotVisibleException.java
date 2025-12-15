package com.example.colorbot;

public class ColorNotVisibleException extends IllegalStateException {
    public ColorNotVisibleException(String message) {
        super(message);
    }
}
