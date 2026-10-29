package dev.coolclient.core.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer. Core ships its own because 1.8.9 bundles Gson 2.2.4 while
 * modern versions bundle Gson 2.11, and their APIs differ.
 *
 * Objects map to {@code LinkedHashMap<String,Object>}, arrays to {@code ArrayList<Object>},
 * numbers to {@code Double}, plus String, Boolean and null.
 */
public final class Json {
    private Json() {}

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object value = p.readValue();
        p.skipWs();
        if (p.pos != p.src.length()) throw p.error("Trailing data");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        if (!(o instanceof Map)) throw new IllegalArgumentException("JSON root is not an object");
        return (Map<String, Object>) o;
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, -1, 0);
        return sb.toString();
    }

    public static String pretty(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 2, 0);
        return sb.toString();
    }

    // ---------------------------------------------------------------- typed getters

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Map<String, Object> m, String key) {
        Object o = m == null ? null : m.get(key);
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Map<String, Object> m, String key) {
        Object o = m == null ? null : m.get(key);
        return o instanceof List ? (List<Object>) o : null;
    }

    public static String str(Map<String, Object> m, String key, String def) {
        Object o = m == null ? null : m.get(key);
        return o instanceof String ? (String) o : def;
    }

    public static double num(Map<String, Object> m, String key, double def) {
        Object o = m == null ? null : m.get(key);
        return o instanceof Number ? ((Number) o).doubleValue() : def;
    }

    public static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object o = m == null ? null : m.get(key);
        return o instanceof Boolean ? (Boolean) o : def;
    }

    // ---------------------------------------------------------------- writer

    private static void write(StringBuilder sb, Object v, int indent, int depth) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            quote(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("0");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append('{');
            Iterator<? extends Map.Entry<?, ?>> it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                newline(sb, indent, depth + 1);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(indent >= 0 ? ": " : ":");
                write(sb, e.getValue(), indent, depth + 1);
                if (it.hasNext()) sb.append(',');
            }
            newline(sb, indent, depth);
            sb.append('}');
        } else if (v instanceof Iterable) {
            Iterator<?> it = ((Iterable<?>) v).iterator();
            if (!it.hasNext()) { sb.append("[]"); return; }
            sb.append('[');
            while (it.hasNext()) {
                newline(sb, indent, depth + 1);
                write(sb, it.next(), indent, depth + 1);
                if (it.hasNext()) sb.append(',');
            }
            newline(sb, indent, depth);
            sb.append(']');
        } else if (v instanceof int[]) {
            List<Object> l = new ArrayList<Object>();
            for (int i : (int[]) v) l.add(i);
            write(sb, l, indent, depth);
        } else {
            quote(sb, v.toString());
        }
    }

    private static void newline(StringBuilder sb, int indent, int depth) {
        if (indent < 0) return;
        sb.append('\n');
        for (int i = 0; i < indent * depth; i++) sb.append(' ');
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- parser

    private static final class Parser {
        final String src;
        int pos;

        Parser(String src) { this.src = src; }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException(msg + " at offset " + pos);
        }

        void skipWs() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') pos++;
                else break;
            }
        }

        Object readValue() {
            if (pos >= src.length()) throw error("Unexpected end");
            char c = src.charAt(pos);
            switch (c) {
                case '{': return readObject();
                case '[': return readArray();
                case '"': return readString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default: return readNumber();
            }
        }

        void expect(String word) {
            if (!src.startsWith(word, pos)) throw error("Expected " + word);
            pos += word.length();
        }

        Map<String, Object> readObject() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            pos++; // {
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                if (peek() != '"') throw error("Expected key");
                String key = readString();
                skipWs();
                if (peek() != ':') throw error("Expected ':'");
                pos++;
                skipWs();
                m.put(key, readValue());
                skipWs();
                char c = peek();
                pos++;
                if (c == '}') return m;
                if (c != ',') throw error("Expected ',' or '}'");
            }
        }

        List<Object> readArray() {
            List<Object> l = new ArrayList<Object>();
            pos++; // [
            skipWs();
            if (peek() == ']') { pos++; return l; }
            while (true) {
                skipWs();
                l.add(readValue());
                skipWs();
                char c = peek();
                pos++;
                if (c == ']') return l;
                if (c != ',') throw error("Expected ',' or ']'");
            }
        }

        char peek() {
            if (pos >= src.length()) throw error("Unexpected end");
            return src.charAt(pos);
        }

        String readString() {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= src.length()) throw error("Unterminated string");
                char c = src.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = src.charAt(pos++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: throw error("Bad escape");
                }
            }
        }

        Double readNumber() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            if (start == pos) throw error("Unexpected character '" + src.charAt(pos) + "'");
            return Double.valueOf(src.substring(start, pos));
        }
    }
}
