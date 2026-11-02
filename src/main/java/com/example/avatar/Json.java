package com.example.avatar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free JSON reader for API response envelopes. */
final class Json {
    private final String text;
    private int pos;

    private Json(String text) { this.text = text; }

    static Map<?, ?> object(String text) {
        Json reader = new Json(text);
        Object value = reader.read();
        reader.space();
        if (reader.pos != text.length() || !(value instanceof Map)) throw reader.invalid();
        return (Map<?, ?>) value;
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("Invalid JSON at offset " + pos);
    }

    private void space() {
        while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++;
    }

    private boolean take(char c) {
        space();
        if (pos < text.length() && text.charAt(pos) == c) { pos++; return true; }
        return false;
    }

    private void require(char c) { if (!take(c)) throw invalid(); }

    private Object read() {
        space();
        if (pos >= text.length()) throw invalid();
        char c = text.charAt(pos);
        if (c == '"') return string();
        if (take('{')) {
            Map<String, Object> result = new LinkedHashMap<>();
            if (take('}')) return result;
            do {
                space();
                String key = string();
                require(':');
                result.put(key, read());
            } while (take(','));
            require('}');
            return result;
        }
        if (take('[')) {
            ArrayList<Object> result = new ArrayList<>();
            if (take(']')) return result;
            do { result.add(read()); } while (take(','));
            require(']');
            return result;
        }
        for (String literal : new String[]{"true", "false", "null"}) {
            if (text.startsWith(literal, pos)) {
                pos += literal.length();
                return literal.equals("null") ? null : Boolean.valueOf(literal);
            }
        }
        int start = pos;
        while (pos < text.length() && "-+0123456789.eE".indexOf(text.charAt(pos)) >= 0) pos++;
        String number = text.substring(start, pos);
        if (!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) throw invalid();
        return new java.math.BigDecimal(number);
    }

    private String string() {
        if (pos >= text.length() || text.charAt(pos++) != '"') throw invalid();
        StringBuilder result = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') return result.toString();
            if (c < 0x20) throw invalid();
            if (c == '\\') {
                if (pos >= text.length()) throw invalid();
                c = text.charAt(pos++);
                switch (c) {
                    case '"': case '\\': case '/': break;
                    case 'b': c = '\b'; break;
                    case 'f': c = '\f'; break;
                    case 'n': c = '\n'; break;
                    case 'r': c = '\r'; break;
                    case 't': c = '\t'; break;
                    case 'u':
                        if (pos + 4 > text.length()) throw invalid();
                        try { c = (char) Integer.parseInt(text.substring(pos, pos + 4), 16); }
                        catch (NumberFormatException e) { throw invalid(); }
                        pos += 4;
                        break;
                    default: throw invalid();
                }
            }
            result.append(c);
        }
        throw invalid();
    }
}
