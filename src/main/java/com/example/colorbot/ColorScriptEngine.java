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

    private static final Pattern HOLD_PATTERN = Pattern.compile("HOLD\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_PATTERN = Pattern.compile("RELEASE\\s+(.+)", Pattern.CASE_INSENSITIVE);


    private static final Pattern HOLD_PATTERN = Pattern.compile("HOLD\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_PATTERN = Pattern.compile("RELEASE\\s+(.+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern TYPE_PATTERN = Pattern.compile("TYPE\\s+\"?(.*?)\"?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOVE_PATTERN = Pattern.compile("MOVE\\s+(-?\\d+)\\s+(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_TARGET_PATTERN = Pattern.compile(
            "IF_TARGET_VISIBLE\\s+THEN\\s+(.+?)\\s+(?:ELSE\\s+(.+))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_COLOR_PATTERN = Pattern.compile(

            "IF_COLOR\\s+(-?\\d+)\\s+(-?\\d+)\\s+(\\d{1,3})\\s+(\\d{1,3})\\s+(\\d{1,3})\\s+THEN\\s+(.+?)\\s+(?:ELSE\\s+(.+))?",

            "IF_COLOR\\s+(-?\\d+)\\s+(-?\\d+)\\s+([#A-Fa-f0-9]{6,7})\\s+THEN\\s+(.+?)\\s+(?:ELSE\\s+(.+))?",

            Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPTURE_PATTERN = Pattern.compile("CAPTURE_TARGET", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_PATTERN = Pattern.compile("LOG\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICK_PATTERN = Pattern.compile("CLICK", Pattern.CASE_INSENSITIVE);

    private static final Pattern LOOP_PATTERN = Pattern.compile("LOOP\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern END_LOOP_PATTERN = Pattern.compile("END_LOOP", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLUE_EYE_PAUSE_PATTERN = Pattern.compile("MACRO\\.PAUSE\\('?(\\d+)'?\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLUE_EYE_PRESS_PATTERN = Pattern.compile("KEYBOARD\\.PRESS\\s+KEYS?\\('?(.*?)'?\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLUE_EYE_HOLD_PATTERN = Pattern.compile("KEYBOARD\\.HOLD\\s+KEYS?\\('?(.*?)'?\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLUE_EYE_RELEASE_PATTERN = Pattern.compile("KEYBOARD\\.RELEASE\\s+KEYS?\\('?(.*?)'?\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLUE_EYE_IF_COLOR_PATTERN = Pattern.compile(

            "IF\\s+COLOR\\.AT\\s+COORDINATE\\s+IS\\s+(NOT\\s+)?\\(RGB\\s+'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\)\\s*BEGIN",

            "IF\\s+COLOR\\.AT\\s+COORDINATE\\s+IS\\s+(NOT\\s+)?\(RGB\\s+'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\\s*,?\\s*'?(\\d+)'?\)\\s*BEGIN",

            Pattern.CASE_INSENSITIVE);
    private static final Pattern BLUE_EYE_LOOP_PATTERN = Pattern.compile("MACRO\\.LOOP\\('?(\\d+)'?\\)\\s*BEGIN", Pattern.CASE_INSENSITIVE);
    private static final Pattern END_PATTERN = Pattern.compile("END", Pattern.CASE_INSENSITIVE);


    private final ColorLibrary library;

    public ColorScriptEngine(ColorLibrary library) {
        this.library = library;
    }

    public List<String> run(String scriptText, Consumer<String> logger) {
        List<String> executed = new ArrayList<>();
        String[] lines = scriptText.split("\\R");

        runLines(lines, 0, lines.length, logger, executed);
        return executed;
    }

    private void runLines(String[] lines, int start, int end, Consumer<String> logger, List<String> executed) {
        for (int i = start; i < end; i++) {

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            int lineNumber = i + 1;
            try {


                Matcher blueEyeLoop = BLUE_EYE_LOOP_PATTERN.matcher(line);
                if (blueEyeLoop.matches()) {
                    int count = Integer.parseInt(blueEyeLoop.group(1));
                    int blockEnd = findBlockEnd(lines, i + 1, end);
                    if (blockEnd == -1) {
                        throw new IllegalArgumentException("MACRO.LOOP missing END after line " + lineNumber);
                    }
                    for (int iteration = 0; iteration < count; iteration++) {
                        runLines(lines, i + 1, blockEnd, logger, executed);
                    }
                    i = blockEnd;
                    continue;
                }
                Matcher blueEyeIf = BLUE_EYE_IF_COLOR_PATTERN.matcher(line);
                if (blueEyeIf.matches()) {
                    boolean negate = blueEyeIf.group(1) != null && !blueEyeIf.group(1).isBlank();
                    int r = Integer.parseInt(blueEyeIf.group(2));
                    int g = Integer.parseInt(blueEyeIf.group(3));
                    int b = Integer.parseInt(blueEyeIf.group(4));
                    int x = Integer.parseInt(blueEyeIf.group(5));
                    int y = Integer.parseInt(blueEyeIf.group(6));
                    int blockEnd = findBlockEnd(lines, i + 1, end);
                    if (blockEnd == -1) {
                        throw new IllegalArgumentException("IF COLOR block missing END after line " + lineNumber);
                    }
                    boolean matches = library.isColorAt(new Point(x, y), new Color(r, g, b));
                    if (negate) {
                        matches = !matches;
                    }
                    if (matches) {
                        runLines(lines, i + 1, blockEnd, logger, executed);
                    }
                    i = blockEnd;
                    logger.accept("Color check at " + x + "," + y + " was " + (matches ? "visible" : "missing"));
                    continue;
                }
                Matcher loopMatcher = LOOP_PATTERN.matcher(line);
                if (loopMatcher.matches()) {
                    int count = Integer.parseInt(loopMatcher.group(1));
                    int loopEnd = findLoopEnd(lines, i + 1, end);
                    if (loopEnd == -1) {
                        throw new IllegalArgumentException("LOOP at line " + lineNumber + " missing END_LOOP");
                    }
                    for (int iteration = 0; iteration < count; iteration++) {
                        runLines(lines, i + 1, loopEnd, logger, executed);
                    }
                    i = loopEnd; // skip to the END_LOOP marker
                    continue;
                }
                if (END_LOOP_PATTERN.matcher(line).matches()) {
                    throw new IllegalArgumentException("END_LOOP without matching LOOP at line " + lineNumber);
                }
                if (END_PATTERN.matcher(line).matches()) {
                    throw new IllegalArgumentException("END without matching BEGIN at line " + lineNumber);
                }

                executeLine(line, logger);
                executed.add("Line " + lineNumber + ": " + line);
            } catch (Exception e) {
                String message = "Line " + lineNumber + " failed: " + e.getMessage();
                logger.accept(message);
                break;
            }
        }

    }

    private int findLoopEnd(String[] lines, int start, int end) {
        for (int i = start; i < end; i++) {
            if (END_LOOP_PATTERN.matcher(lines[i].trim()).matches()) {
                return i;
            }
        }
        return -1;
    }

    private int findBlockEnd(String[] lines, int start, int end) {
        int depth = 0;
        for (int i = start; i < end; i++) {
            String trimmed = lines[i].trim();
            if (BLUE_EYE_IF_COLOR_PATTERN.matcher(trimmed).matches() || BLUE_EYE_LOOP_PATTERN.matcher(trimmed).matches()) {
                depth++;
            }
            if (END_PATTERN.matcher(trimmed).matches()) {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;

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

        Matcher holdMatcher = HOLD_PATTERN.matcher(line);
        if (holdMatcher.matches()) {
            String key = holdMatcher.group(1).trim();
            library.holdKey(key);
            logger.accept("Held " + key);
            return;
        }
        Matcher releaseMatcher = RELEASE_PATTERN.matcher(line);
        if (releaseMatcher.matches()) {
            String key = releaseMatcher.group(1).trim();
            library.releaseKey(key);
            logger.accept("Released " + key);
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

            int r = Integer.parseInt(colorMatcher.group(3));
            int g = Integer.parseInt(colorMatcher.group(4));
            int b = Integer.parseInt(colorMatcher.group(5));
            Color color = new Color(r, g, b);
            boolean matches = library.isColorAt(new Point(x, y), color);
            String action = matches ? colorMatcher.group(6) : colorMatcher.group(7);

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

        Matcher pauseMatcher = BLUE_EYE_PAUSE_PATTERN.matcher(line);
        if (pauseMatcher.matches()) {
            long delay = Long.parseLong(pauseMatcher.group(1));
            library.sleepMs(delay);
            logger.accept("Waited " + delay + " ms");
            return;
        }
        Matcher blueEyePress = BLUE_EYE_PRESS_PATTERN.matcher(line);
        if (blueEyePress.matches()) {
            String key = blueEyePress.group(1);
            library.pressKey(key);
            logger.accept("Pressed " + key);
            return;
        }
        Matcher blueEyeHold = BLUE_EYE_HOLD_PATTERN.matcher(line);
        if (blueEyeHold.matches()) {
            String key = blueEyeHold.group(1);
            library.holdKey(key);
            logger.accept("Held " + key);
            return;
        }
        Matcher blueEyeRelease = BLUE_EYE_RELEASE_PATTERN.matcher(line);
        if (blueEyeRelease.matches()) {
            String key = blueEyeRelease.group(1);
            library.releaseKey(key);
            logger.accept("Released " + key);
            return;
        }
        throw new IllegalArgumentException("Unknown instruction: " + line);
    }
}
