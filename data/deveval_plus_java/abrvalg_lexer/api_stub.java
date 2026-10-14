import java.util.*;
import java.util.regex.*;

/**
 * API stub for Lexer module.
 * Error types and Token are fully implemented; lexer functions are stubs.
 */
public class Lexer {

    // ---- Error types (fully implemented) ----

    public static class AbrvalgSyntaxError extends RuntimeException {
        private final String msg;
        private final int line;
        private final int column;

        public AbrvalgSyntaxError(String message, int line, int column) {
            super(message);
            this.msg = message;
            this.line = line;
            this.column = column;
        }

        public String getMsg() { return msg; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
    }

    public static class LexerError extends AbrvalgSyntaxError {
        public LexerError(String message, int line, int column) {
            super(message, line, column);
        }
    }

    // ---- Token (fully implemented) ----

    public static class Token {
        private final String name;
        private final Object value;
        private final int line;
        private final int column;

        public Token(String name, Object value, int line, int column) {
            this.name = name;
            this.value = value;
            this.line = line;
            this.column = column;
        }

        public String getName() { return name; }
        public Object getValue() { return value; }
        public int getLine() { return line; }
        public int getColumn() { return column; }

        @Override
        public String toString() {
            String valStr;
            if (value == null) {
                valStr = "None";
            } else if (value instanceof String) {
                valStr = "'" + value + "'";
            } else {
                valStr = value.toString();
            }
            return "('" + name + "', " + valStr + ", " + line + ", " + column + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Token)) return false;
            Token t = (Token) o;
            return line == t.line && column == t.column
                    && Objects.equals(name, t.name)
                    && Objects.equals(value, t.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, line, column);
        }
    }

    // ---- Stub: decode_str ----

    public static String decodeStr(String s) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Stub: decode_num ----

    public static Number decodeNum(String s) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Instance state ----

    private final List<String> sourceLines = new ArrayList<>();
    private final Pattern regex = null;

    public Lexer() {
    }

    public List<String> getSourceLines() { return sourceLines; }
    public Pattern getRegex() { return regex; }

    // ---- Stub: _convert_rules ----

    public List<String> convertRules(String[][] rules) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Stub: _compile_rules ----

    public Pattern compileRules(String[][] rules) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Stub: _tokenize_line ----

    public List<Token> tokenizeLine(String line, int lineNum) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Stub: _count_leading_characters ----

    public int countLeadingCharacters(String line, char ch) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Stub: _detect_indent ----

    public String detectIndent(String line) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- Stub: tokenize ----

    public List<Token> tokenize(String s) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // ---- TokenStream (fully implemented, depends on Token and LexerError) ----

    public static class TokenStream {
        private final List<Token> tokens;
        private int pos;

        public TokenStream(List<Token> tokens) {
            this.tokens = tokens;
            this.pos = 0;
        }

        public List<Token> getTokens() { return tokens; }
        public int getPos() { return pos; }

        public Token consumeExpected(String... expectedNames) {
            Token token = null;
            for (String expected : expectedNames) {
                token = consume();
                if (!token.getName().equals(expected)) {
                    throw new LexerError(
                        "Expected " + expected + ", got " + token.getName(),
                        token.getLine(), token.getColumn());
                }
            }
            return token;
        }

        public Token consume() {
            Token token = current();
            pos++;
            return token;
        }

        public Token current() {
            try {
                return tokens.get(pos);
            } catch (IndexOutOfBoundsException e) {
                Token last = tokens.get(tokens.size() - 1);
                throw new LexerError("Unexpected end of input", last.getLine(), last.getColumn());
            }
        }

        public void expectEnd() {
            if (!isEnd()) {
                Token token = current();
                throw new LexerError("End expected", token.getLine(), token.getColumn());
            }
        }

        public boolean isEnd() {
            return pos == tokens.size();
        }
    }

    // ---- Stub: report_syntax_error ----

    public static void reportSyntaxError(Lexer lexer, AbrvalgSyntaxError error) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
