package com.example.colorbot;

import java.awt.Color;
import java.awt.Point;

/**
 * Immutable container for a single screen coordinate and its expected color.
 */
public record ColorSample(Point location, Color color) {
    public ColorSample {
        if (location == null) {
            throw new IllegalArgumentException("location cannot be null");
        }
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }
    }

    public String toHex() {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
