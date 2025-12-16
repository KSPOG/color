package com.example.colorbot;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny domain-specific scripting engine inspired by Blue Eye Macro style commands.
 */
public class ColorScriptEngine {
    private static final Pattern WAIT_PATTERN = Pattern.compile("WAIT\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRESS_PATTERN = Pattern.compile("PRESS\\s+([A-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("TYPE\\s+\"?(.*?)\"?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOVE_PATTERN = Pattern.compile("MOVE\\s+(-?\\d+)\\s+(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_TARGET_PATTERN = Pattern.compile(
            "IF_TARGET_VISIBLE\\s+THEN\\s+(.+?)\\s+(?:ELSE\\s+(.+))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_COLOR_PATTERN = Pattern.compile(
            "IF_COLOR\\s+(-?\\d+)\\s+(-?\\d+)\\s+([#A-Fa-f0-9]{6,7})\\s+THEN\\s+(.+?)\\s+(?:ELSE\\s+(.+))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPTURE_PATTERN = Pattern.compile("CAPTURE_TARGET", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_PATTERN = Pattern.compile("LOG\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICK_PATTERN = Pattern.compile("CLICK", Pattern.CASE_INSENSITIVE);

    private final ColorLibrary library;

    public ColorScriptEngine(ColorLibrary library) {
        this.library = library;
    }

    public List<String> run(String scriptText, Consumer<String> logger) {
        List<String> executed = new ArrayList<>();
        String[] lines = scriptText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            int lineNumber = i + 1;
            try {
                executeLine(line, logger);
                executed.add("Line " + lineNumber + ": " + line);
            } catch (Exception e) {
                String message = "Line " + lineNumber + " failed: " + e.getMessage();
                logger.accept(message);
                break;
            }
        }
        return executed;
    }

    private void executeLine(String line, Consumer<String> logger) {
        Matcher waitMatcher = WAIT_PATTERN.matcher(line);
        if (waitMatcher.matches()) {
            long delay = Long.parseLong(waitMatcher.group(1));
            library.sleepMs(delay);
            logger.accept("Waited " + delay + " ms");
            return;
        }
        Matcher pressMatcher = PRESS_PATTERN.matcher(line);
        if (pressMatcher.matches()) {
            String key = pressMatcher.group(1);
            library.pressKey(key);
            logger.accept("Pressed " + key);
            return;
        }
        Matcher typeMatcher = TYPE_PATTERN.matcher(line);
        if (typeMatcher.matches()) {
            String text = typeMatcher.group(1);
            library.typeText(text);
            logger.accept("Typed '" + text + "'");
            return;
        }
        Matcher moveMatcher = MOVE_PATTERN.matcher(line);
        if (moveMatcher.matches()) {
            int x = Integer.parseInt(moveMatcher.group(1));
            int y = Integer.parseInt(moveMatcher.group(2));
            library.moveMouse(x, y);
            logger.accept("Moved mouse to " + x + "," + y);
            return;
        }
        if (CAPTURE_PATTERN.matcher(line).matches()) {
            ColorSample sample = library.captureCurrentPixel();
            library.setTargetSample(sample);
            logger.accept("Captured target at " + sample.location() + " with color " + sample.toHex());
            return;
        }
        Matcher targetMatcher = IF_TARGET_PATTERN.matcher(line);
        if (targetMatcher.matches()) {
            boolean visible = library.isTargetVisible();
            String action = visible ? targetMatcher.group(1) : targetMatcher.group(2);
            if (action != null) {
                executeLine(action.trim(), logger);
            }
            logger.accept("Target was " + (visible ? "visible" : "missing"));
            return;
        }
        Matcher colorMatcher = IF_COLOR_PATTERN.matcher(line);
        if (colorMatcher.matches()) {
            int x = Integer.parseInt(colorMatcher.group(1));
            int y = Integer.parseInt(colorMatcher.group(2));
            Color color = ColorLibrary.parseColor(colorMatcher.group(3));
            boolean matches = library.isColorAt(new Point(x, y), color);
            String action = matches ? colorMatcher.group(4) : colorMatcher.group(5);
            if (action != null) {
                executeLine(action.trim(), logger);
            }
            logger.accept("Color check at " + x + "," + y + " was " + (matches ? "visible" : "missing"));
            return;
        }
        Matcher logMatcher = LOG_PATTERN.matcher(line);
        if (logMatcher.matches()) {
            logger.accept(logMatcher.group(1));
            return;
        }
        if (CLICK_PATTERN.matcher(line).matches()) {
            library.leftClick();
            logger.accept("Clicked mouse");
            return;
        }
        throw new IllegalArgumentException("Unknown instruction: " + line);
    }
}
