package com.example.colorbot;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class ColorBotConfig {
    private int captureKeyCode = KeyEvent.VK_F8;
    private int visibleKeyCode = KeyEvent.VK_F9;
    private int notVisibleKeyCode = KeyEvent.VK_F10;
    private boolean failWhenMissing = true;
    private Point targetPoint;
    private Color targetColor;

    public ColorBotConfig copy() {
        ColorBotConfig config = new ColorBotConfig();
        config.captureKeyCode = this.captureKeyCode;
        config.visibleKeyCode = this.visibleKeyCode;
        config.notVisibleKeyCode = this.notVisibleKeyCode;
        config.failWhenMissing = this.failWhenMissing;
        config.targetPoint = this.targetPoint == null ? null : new Point(this.targetPoint);
        config.targetColor = this.targetColor;
        return config;
    }

    public int getCaptureKeyCode() {
        return captureKeyCode;
    }

    public void setCaptureKeyCode(int captureKeyCode) {
        this.captureKeyCode = captureKeyCode;
    }

    public int getVisibleKeyCode() {
        return visibleKeyCode;
    }

    public void setVisibleKeyCode(int visibleKeyCode) {
        this.visibleKeyCode = visibleKeyCode;
    }

    public int getNotVisibleKeyCode() {
        return notVisibleKeyCode;
    }

    public void setNotVisibleKeyCode(int notVisibleKeyCode) {
        this.notVisibleKeyCode = notVisibleKeyCode;
    }

    public boolean isFailWhenMissing() {
        return failWhenMissing;
    }

    public void setFailWhenMissing(boolean failWhenMissing) {
        this.failWhenMissing = failWhenMissing;
    }

    public Point getTargetPoint() {
        return targetPoint;
    }

    public void setTargetPoint(Point targetPoint) {
        this.targetPoint = targetPoint;
    }

    public Color getTargetColor() {
        return targetColor;
    }

    public void setTargetColor(Color targetColor) {
        this.targetColor = targetColor;
    }

    public String getTargetColorHex() {
        if (targetColor == null) {
            return "";
        }
        return String.format("#%02X%02X%02X", targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue());
    }

    public String describeTargetPoint() {
        if (targetPoint == null) {
            return "";
        }
        return targetPoint.x + ", " + targetPoint.y;
    }

    public String describeKey(int keyCode) {
        return KeyEvent.getKeyText(keyCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColorBotConfig)) {
            return false;
        }
        ColorBotConfig that = (ColorBotConfig) o;
        return captureKeyCode == that.captureKeyCode
                && visibleKeyCode == that.visibleKeyCode
                && notVisibleKeyCode == that.notVisibleKeyCode
                && failWhenMissing == that.failWhenMissing
                && Objects.equals(targetPoint, that.targetPoint)
                && Objects.equals(targetColor, that.targetColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(captureKeyCode, visibleKeyCode, notVisibleKeyCode, failWhenMissing, targetPoint, targetColor);
    }
}
