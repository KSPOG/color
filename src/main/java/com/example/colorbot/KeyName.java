package com.example.colorbot;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for converting human readable key names to {@link KeyEvent} key codes.
 */
public final class KeyName {
    private static final Map<String, Integer> KEY_MAP = new HashMap<>();

    static {
        KEY_MAP.put("ENTER", KeyEvent.VK_ENTER);
        KEY_MAP.put("SPACE", KeyEvent.VK_SPACE);
        KEY_MAP.put("TAB", KeyEvent.VK_TAB);
        KEY_MAP.put("ESC", KeyEvent.VK_ESCAPE);
        KEY_MAP.put("ESCAPE", KeyEvent.VK_ESCAPE);
        KEY_MAP.put("SHIFT", KeyEvent.VK_SHIFT);
        KEY_MAP.put("CTRL", KeyEvent.VK_CONTROL);
        KEY_MAP.put("CONTROL", KeyEvent.VK_CONTROL);
        KEY_MAP.put("ALT", KeyEvent.VK_ALT);
        KEY_MAP.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        KEY_MAP.put("DELETE", KeyEvent.VK_DELETE);
        KEY_MAP.put("HOME", KeyEvent.VK_HOME);
        KEY_MAP.put("END", KeyEvent.VK_END);
        KEY_MAP.put("PAGEUP", KeyEvent.VK_PAGE_UP);
        KEY_MAP.put("PAGEDOWN", KeyEvent.VK_PAGE_DOWN);
        KEY_MAP.put("UP", KeyEvent.VK_UP);
        KEY_MAP.put("DOWN", KeyEvent.VK_DOWN);
        KEY_MAP.put("LEFT", KeyEvent.VK_LEFT);
        KEY_MAP.put("RIGHT", KeyEvent.VK_RIGHT);
        KEY_MAP.put("INSERT", KeyEvent.VK_INSERT);
        KEY_MAP.put("CAPSLOCK", KeyEvent.VK_CAPS_LOCK);
        KEY_MAP.put("PAUSE", KeyEvent.VK_PAUSE);
        KEY_MAP.put("PRINTSCREEN", KeyEvent.VK_PRINTSCREEN);
    }

    private KeyName() {
    }

    public static int toKeyCode(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("Key name cannot be blank");
        }
        String normalized = keyName.trim().toUpperCase(Locale.ROOT);
        Integer mapped = KEY_MAP.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        if (normalized.startsWith("F")) {
            try {
                int number = Integer.parseInt(normalized.substring(1));
                if (number >= 1 && number <= 24) {
                    return KeyEvent.VK_F1 + number - 1;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        if (normalized.length() == 1) {
            return KeyEvent.getExtendedKeyCodeForChar(normalized.charAt(0));
        }
        throw new IllegalArgumentException("Unsupported key name: " + keyName);
    }
}
