package com.example.colorbot;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

public class ColorBot {
    private final Robot robot;
    private ColorBotConfig config;

    public ColorBot(ColorBotConfig config) {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new IllegalStateException("Unable to create automation robot", e);
        }
        this.config = config.copy();
    }

    public synchronized ColorBotConfig getConfig() {
        return config.copy();
    }

    public synchronized void updateConfig(ColorBotConfig updated) {
        this.config = updated.copy();
    }

    public synchronized CaptureResult captureUnderCursor() {
        Point cursor = MouseInfo.getPointerInfo().getLocation();
        Color color = robot.getPixelColor(cursor.x, cursor.y);
        config.setTargetPoint(cursor);
        config.setTargetColor(color);
        return new CaptureResult(cursor, color, describeColor(color));
    }

    public synchronized boolean isColorVisible() {
        if (config.getTargetPoint() == null || config.getTargetColor() == null) {
            return false;
        }
        Color sampled = robot.getPixelColor(config.getTargetPoint().x, config.getTargetPoint().y);
        return sampled.equals(config.getTargetColor());
    }

    public synchronized void ensureColorVisible() {
        if (!isColorVisible()) {
            throw new ColorNotVisibleException("Configured color code is not visible at the target coordinates.");
        }
    }

    public synchronized VisibilityResult performVisibilityActions() {
        boolean visible = isColorVisible();
        if (visible) {
            pressKey(config.getVisibleKeyCode());
        } else {
            pressKey(config.getNotVisibleKeyCode());
            if (config.isFailWhenMissing()) {
                throw new ColorNotVisibleException("Configured color code is not visible at the target coordinates.");
            }
        }
        return new VisibilityResult(visible, config.getTargetPoint(), config.getTargetColor());
    }

    private void pressKey(int keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        robot.delay(50);
    }

    private String describeColor(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    public record CaptureResult(Point point, Color color, String hex) { }

    public record VisibilityResult(boolean visible, Point point, Color color) { }
}
