package com.example.colorbot;

import java.awt.event.KeyEvent;

/**
 * Lightweight mapper from characters to key codes.
 */
public final class KeyEventMapper {
    private KeyEventMapper() {
    }

    public static int mapChar(char c) {
        if (Character.isLetterOrDigit(c)) {
            return KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(c));
        }
        return switch (c) {
            case ' ' -> KeyEvent.VK_SPACE;
            case ',' -> KeyEvent.VK_COMMA;
            case '.' -> KeyEvent.VK_PERIOD;
            case '-' -> KeyEvent.VK_MINUS;
            case '_' -> KeyEvent.VK_UNDERSCORE;
            case '\\' -> KeyEvent.VK_BACK_SLASH;
            case '/' -> KeyEvent.VK_SLASH;
            case ';' -> KeyEvent.VK_SEMICOLON;
            case ':' -> KeyEvent.VK_COLON;
            case '\'' -> KeyEvent.VK_QUOTE;
            case '"' -> KeyEvent.VK_QUOTEDBL;
            case '[' -> KeyEvent.VK_OPEN_BRACKET;
            case ']' -> KeyEvent.VK_CLOSE_BRACKET;
            default -> KeyEvent.getExtendedKeyCodeForChar(c);
        };
    }
}
