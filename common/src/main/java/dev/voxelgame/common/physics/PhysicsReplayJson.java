package dev.voxelgame.common.physics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PhysicsReplayJson {
    private PhysicsReplayJson() {
    }

    static Map<String, Object> parseObject(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parse();
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("Physics replay JSON line must be an object");
        }
        LinkedHashMap<String, Object> object = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            object.put((String) entry.getKey(), entry.getValue());
        }
        return object;
    }

    static void appendString(StringBuilder builder, String value) {
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int padding = hex.length(); padding < 4; padding++) {
                            builder.append('0');
                        }
                        builder.append(hex);
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON");
            }
            char c = text.charAt(index);
            return switch (c) {
                case '{' -> parseObjectValue();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected JSON token: " + c);
                }
            };
        }

        private Map<String, Object> parseObjectValue() {
            expect('{');
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw error("Expected object key");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            ArrayList<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c != '\\') {
                    if (c < 0x20) {
                        throw error("Unescaped control character in string");
                    }
                    builder.append(c);
                    continue;
                }
                if (index >= text.length()) {
                    throw error("Unterminated escape sequence");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicodeEscape());
                    default -> throw error("Unsupported escape sequence: \\" + escaped);
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("Incomplete unicode escape");
            }
            int value = 0;
            for (int offset = 0; offset < 4; offset++) {
                char c = text.charAt(index++);
                int digit = Character.digit(c, 16);
                if (digit < 0) {
                    throw error("Invalid unicode escape");
                }
                value = value * 16 + digit;
            }
            return (char) value;
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Invalid JSON literal");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (index >= text.length()) {
                throw error("Invalid JSON number");
            }
            if (peek('0')) {
                index++;
            } else if (Character.isDigit(text.charAt(index))) {
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            } else {
                throw error("Invalid JSON number");
            }
            boolean floating = false;
            if (peek('.')) {
                floating = true;
                index++;
                if (index >= text.length() || !Character.isDigit(text.charAt(index))) {
                    throw error("Invalid JSON fraction");
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                floating = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                if (index >= text.length() || !Character.isDigit(text.charAt(index))) {
                    throw error("Invalid JSON exponent");
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            String raw = text.substring(start, index);
            try {
                if (floating) {
                    return Double.parseDouble(raw);
                }
                return Long.parseLong(raw);
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON number: " + raw);
            }
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char c = text.charAt(index);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    return;
                }
                index++;
            }
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + index);
        }
    }
}
