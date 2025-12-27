package com.example.colorbot;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

import java.util.Objects;

import java.awt.Robot;
import java.awt.event.InputEvent;


import java.util.Optional;

/**
 * Core automation library backing the Color Bot UI and scripting system.
 */
public class ColorLibrary {
    private final Robot robot;
    private volatile ColorSample targetSample;


    public ColorLibrary(Robot robot) {
        this.robot = Objects.requireNonNull(robot, "robot");
        this.robot.setAutoWaitForIdle(true);
    }


    public ColorLibrary() {
        try {
            this.robot = new Robot();
            this.robot.setAutoWaitForIdle(true);
        } catch (AWTException e) {
            throw new IllegalStateException("Unable to create Robot", e);
        }
    }

    public synchronized void setTargetSample(ColorSample sample) {
        this.targetSample = sample;
    }

    public Optional<ColorSample> getTargetSample() {
        return Optional.ofNullable(targetSample);
    }

    public ColorSample captureCurrentPixel() {
        Point pointer = MouseInfo.getPointerInfo().getLocation();
        Color color = robot.getPixelColor(pointer.x, pointer.y);
        return new ColorSample(pointer, color);
    }

    public boolean isTargetVisible() {
        return getTargetSample()
                .map(this::isColorAt)
                .orElse(false);
    }

    public boolean isColorAt(ColorSample sample) {
        Color current = robot.getPixelColor(sample.location().x, sample.location().y);
        return colorsMatch(sample.color(), current);
    }

    public boolean isColorAt(Point location, Color color) {
        Color current = robot.getPixelColor(location.x, location.y);
        return colorsMatch(color, current);
    }

    public void pressKey(String keyName) {
        int keyCode = KeyName.toKeyCode(keyName);
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }


    public void holdKey(String keyName) {
        int keyCode = KeyName.toKeyCode(keyName);
        robot.keyPress(keyCode);
    }

    public void releaseKey(String keyName) {
        int keyCode = KeyName.toKeyCode(keyName);
        robot.keyRelease(keyCode);
    }

    public void typeText(String text) {
        if (text == null) {
            return;
        }
        for (char c : text.toCharArray()) {
            int keycode = KeyEventMapper.mapChar(c);
            robot.keyPress(keycode);
            robot.keyRelease(keycode);
        }
    }

    public void moveMouse(int x, int y) {
        robot.mouseMove(x, y);
    }

    public void leftClick() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static Color parseColor(String hex) {
        String trimmed = hex.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.length() != 6) {
            throw new IllegalArgumentException("Color must be a 6 digit hex value");
        }
        int r = Integer.parseInt(trimmed.substring(0, 2), 16);
        int g = Integer.parseInt(trimmed.substring(2, 4), 16);
        int b = Integer.parseInt(trimmed.substring(4, 6), 16);
        return new Color(r, g, b);
    }

    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }


    public BufferedImage captureScreenshot() {
        Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        return robot.createScreenCapture(screen);
    }

    private boolean colorsMatch(Color expected, Color actual) {
        return expected.equals(actual);
    }
}
