import java.util.*;
import java.util.regex.*;
import java.util.function.BiFunction;

/**
 * Interpreter
 * -----------
 *
 * AST-walking interpreter for the abrvalg language.
 * Includes all dependency types: AST nodes, error types, Token, Lexer, TokenStream, Parser.
 */
public class Interpreter {

    // ========================================================================
    // Error types (from abrvalg/errors.py) -- fully implemented helper
    // ========================================================================

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

    public static class ParserError extends AbrvalgSyntaxError {
        public ParserError(String message, Token token) {
            super(message, token.getLine(), token.getColumn());
        }
    }

    // ========================================================================
    // AST node types (from abrvalg/ast.py) -- fully implemented helper
    // ========================================================================

    public interface AstNode {}

    public static class Number implements AstNode {
        private final Object value;
        public Number(Object value) { this.value = value; }
        public Object getValue() { return value; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Number)) return false;
            return Objects.equals(value, ((Number) o).value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
        @Override public String toString() { return "Number(" + value + ")"; }
    }

    public static class StringNode implements AstNode {
        private final String value;
        public StringNode(String value) { this.value = value; }
        public String getValue() { return value; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringNode)) return false;
            return Objects.equals(value, ((StringNode) o).value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
        @Override public String toString() { return "String(" + value + ")"; }
    }

    public static class Identifier implements AstNode {
        private final String value;
        public Identifier(String value) { this.value = value; }
        public String getValue() { return value; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Identifier)) return false;
            return Objects.equals(value, ((Identifier) o).value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
        @Override public String toString() { return "Identifier(" + value + ")"; }
    }

    public static class Assignment implements AstNode {
        private final AstNode left;
        private final AstNode right;
        public Assignment(AstNode left, AstNode right) { this.left = left; this.right = right; }
        public AstNode getLeft() { return left; }
        public AstNode getRight() { return right; }
        @Override public String toString() { return "Assignment(" + left + ", " + right + ")"; }
    }

    public static class BinaryOperator implements AstNode {
        private final String operator;
        private final AstNode left;
        private final AstNode right;
        public BinaryOperator(String operator, AstNode left, AstNode right) {
            this.operator = operator; this.left = left; this.right = right;
        }
        public String getOperator() { return operator; }
        public AstNode getLeft() { return left; }
        public AstNode getRight() { return right; }
        @Override public String toString() { return "BinaryOperator(" + operator + ", " + left + ", " + right + ")"; }
    }

    public static class UnaryOperator implements AstNode {
        private final String operator;
        private final AstNode right;
        public UnaryOperator(String operator, AstNode right) { this.operator = operator; this.right = right; }
        public String getOperator() { return operator; }
        public AstNode getRight() { return right; }
        @Override public String toString() { return "UnaryOperator(" + operator + ", " + right + ")"; }
    }

    public static class Call implements AstNode {
        private final AstNode left;
        private final List<AstNode> arguments;
        public Call(AstNode left, List<AstNode> arguments) { this.left = left; this.arguments = arguments; }
        public AstNode getLeft() { return left; }
        public List<AstNode> getArguments() { return arguments; }
        @Override public String toString() { return "Call(" + left + ", " + arguments + ")"; }
    }

    public static class Function implements AstNode {
        private final String name;
        private final List<String> params;
        private final List<AstNode> body;
        public Function(String name, List<String> params, List<AstNode> body) {
            this.name = name; this.params = params; this.body = body;
        }
        public String getName() { return name; }
        public List<String> getParams() { return params; }
        public List<AstNode> getBody() { return body; }
        @Override public String toString() { return "Function(" + name + ", " + params + ", " + body + ")"; }
    }

    public static class Condition implements AstNode {
        private final AstNode test;
        private final List<AstNode> ifBody;
        private final List<ConditionElif> elifs;
        private final List<AstNode> elseBody;
        public Condition(AstNode test, List<AstNode> ifBody, List<ConditionElif> elifs, List<AstNode> elseBody) {
            this.test = test; this.ifBody = ifBody; this.elifs = elifs; this.elseBody = elseBody;
        }
        public AstNode getTest() { return test; }
        public List<AstNode> getIfBody() { return ifBody; }
        public List<ConditionElif> getElifs() { return elifs; }
        public List<AstNode> getElseBody() { return elseBody; }
        @Override public String toString() { return "Condition(" + test + ")"; }
    }

    public static class ConditionElif implements AstNode {
        private final AstNode test;
        private final List<AstNode> body;
        public ConditionElif(AstNode test, List<AstNode> body) { this.test = test; this.body = body; }
        public AstNode getTest() { return test; }
        public List<AstNode> getBody() { return body; }
        @Override public String toString() { return "ConditionElif(" + test + ")"; }
    }

    public static class Match implements AstNode {
        private final AstNode test;
        private final List<MatchPattern> patterns;
        private final List<AstNode> elseBody;
        public Match(AstNode test, List<MatchPattern> patterns, List<AstNode> elseBody) {
            this.test = test; this.patterns = patterns; this.elseBody = elseBody;
        }
        public AstNode getTest() { return test; }
        public List<MatchPattern> getPatterns() { return patterns; }
        public List<AstNode> getElseBody() { return elseBody; }
        @Override public String toString() { return "Match(" + test + ")"; }
    }

    public static class MatchPattern implements AstNode {
        private final AstNode pattern;
        private final List<AstNode> body;
        public MatchPattern(AstNode pattern, List<AstNode> body) { this.pattern = pattern; this.body = body; }
        public AstNode getPattern() { return pattern; }
        public List<AstNode> getBody() { return body; }
        @Override public String toString() { return "MatchPattern(" + pattern + ")"; }
    }

    public static class WhileLoop implements AstNode {
        private final AstNode test;
        private final List<AstNode> body;
        public WhileLoop(AstNode test, List<AstNode> body) { this.test = test; this.body = body; }
        public AstNode getTest() { return test; }
        public List<AstNode> getBody() { return body; }
        @Override public String toString() { return "WhileLoop(" + test + ")"; }
    }

    public static class ForLoop implements AstNode {
        private final String varName;
        private final AstNode collection;
        private final List<AstNode> body;
        public ForLoop(String varName, AstNode collection, List<AstNode> body) {
            this.varName = varName; this.collection = collection; this.body = body;
        }
        public String getVarName() { return varName; }
        public AstNode getCollection() { return collection; }
        public List<AstNode> getBody() { return body; }
        @Override public String toString() { return "ForLoop(" + varName + ")"; }
    }

    public static class Break implements AstNode {
        @Override public String toString() { return "Break()"; }
    }

    public static class Continue implements AstNode {
        @Override public String toString() { return "Continue()"; }
    }

    public static class Return implements AstNode {
        private final AstNode value;
        public Return(AstNode value) { this.value = value; }
        public AstNode getValue() { return value; }
        @Override public String toString() { return "Return(" + value + ")"; }
    }

    public static class Array implements AstNode {
        private final List<AstNode> items;
        public Array(List<AstNode> items) { this.items = items; }
        public List<AstNode> getItems() { return items; }
        @Override public String toString() { return "Array(" + items + ")"; }
    }

    public static class Dictionary implements AstNode {
        private final List<Map.Entry<AstNode, AstNode>> items;
        public Dictionary(List<Map.Entry<AstNode, AstNode>> items) { this.items = items; }
        public List<Map.Entry<AstNode, AstNode>> getItems() { return items; }
        @Override public String toString() { return "Dictionary(" + items.size() + " items)"; }
    }

    public static class SubscriptOperator implements AstNode {
        private final AstNode left;
        private final AstNode key;
        public SubscriptOperator(AstNode left, AstNode key) { this.left = left; this.key = key; }
        public AstNode getLeft() { return left; }
        public AstNode getKey() { return key; }
        @Override public String toString() { return "SubscriptOperator(" + left + ", " + key + ")"; }
    }

    public static class Program implements AstNode {
        private final List<AstNode> body;
        public Program(List<AstNode> body) { this.body = body; }
        public List<AstNode> getBody() { return body; }
        @Override public String toString() { return "Program(" + body + ")"; }
    }

    // ========================================================================
    // Token (from lexer.py) -- fully implemented helper
    // ========================================================================

    public static class Token {
        private final String name;
        private final Object value;
        private final int line;
        private final int column;

        public Token(String name, Object value, int line, int column) {
            this.name = name; this.value = value; this.line = line; this.column = column;
        }

        public String getName() { return name; }
        public Object getValue() { return value; }
        public int getLine() { return line; }
        public int getColumn() { return column; }

        @Override
        public String toString() {
            String valStr;
            if (value == null) valStr = "None";
            else if (value instanceof String) valStr = "'" + value + "'";
            else valStr = value.toString();
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
        public int hashCode() { return Objects.hash(name, value, line, column); }
    }

    // ========================================================================
    // Lexer (from lexer.py) -- fully implemented helper
    // ========================================================================

    public static class Lexer {

        static final String[][] RULES = {
            {"COMMENT",    "#.*"},
            {"STRING",     "\"(\\\\\"|[^\"])*\""},
            {"STRING",     "'(\\\\'|[^'])*'"},
            {"NUMBER",     "\\d+\\.\\d+"},
            {"NUMBER",     "\\d+"},
            {"NAME",       "[a-zA-Z_]\\w*"},
            {"WHITESPACE", "[ \\t]+"},
            {"NEWLINE",    "\\n+"},
            {"OPERATOR",   "[\\+\\*\\-/%]"},
            {"OPERATOR",   "<=|>=|==|!=|<|>"},
            {"OPERATOR",   "\\|\\||&&"},
            {"OPERATOR",   "\\.\\.\\.|\\.\\."},
            {"OPERATOR",   "!"},
            {"ASSIGN",     "="},
            {"LPAREN",     "\\("},
            {"RPAREN",     "\\)"},
            {"LBRACK",     "\\["},
            {"RBRACK",     "\\]"},
            {"LCBRACK",    "\\{"},
            {"RCBRACK",    "\\}"},
            {"COLON",      ":"},
            {"COMMA",      ","},
        };

        private static final Map<String, String> KEYWORDS = new LinkedHashMap<>();
        static {
            KEYWORDS.put("func",     "FUNCTION");
            KEYWORDS.put("return",   "RETURN");
            KEYWORDS.put("else",     "ELSE");
            KEYWORDS.put("elif",     "ELIF");
            KEYWORDS.put("if",       "IF");
            KEYWORDS.put("while",    "WHILE");
            KEYWORDS.put("break",    "BREAK");
            KEYWORDS.put("continue", "CONTINUE");
            KEYWORDS.put("for",      "FOR");
            KEYWORDS.put("in",       "IN");
            KEYWORDS.put("match",    "MATCH");
            KEYWORDS.put("when",     "WHEN");
        }

        private static final Set<String> IGNORE_TOKENS = new HashSet<>(Arrays.asList("WHITESPACE", "COMMENT"));

        private static final Map<Character, Character> ESCAPE_CHARS = new HashMap<>();
        static {
            ESCAPE_CHARS.put('r', '\r');
            ESCAPE_CHARS.put('n', '\n');
            ESCAPE_CHARS.put('t', '\t');
            ESCAPE_CHARS.put('\\', '\\');
            ESCAPE_CHARS.put('"', '"');
            ESCAPE_CHARS.put('\'', '\'');
        }

        private final List<String> sourceLines;
        private final Pattern regex;
        private final List<GroupMapping> groupMappings;

        private static class GroupMapping {
            final String name;
            final int groupIndex;
            GroupMapping(String name, int groupIndex) { this.name = name; this.groupIndex = groupIndex; }
        }

        public Lexer() {
            this.sourceLines = new ArrayList<>();
            this.groupMappings = new ArrayList<>();
            this.regex = compileRules(RULES);
        }

        public List<String> getSourceLines() { return sourceLines; }

        public static String decodeStr(String s) {
            String inner = s.substring(1, s.length() - 1);
            Pattern pat = Pattern.compile("\\\\([rnt\\\\'\"])");
            Matcher matcher = pat.matcher(inner);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                char ch = matcher.group(1).charAt(0);
                Character replacement = ESCAPE_CHARS.get(ch);
                if (replacement == null) throw new RuntimeException("Unknown escape character " + ch);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacement)));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        public static java.lang.Number decodeNum(String s) {
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { return Double.parseDouble(s); }
        }

        private List<String> convertRules(String[][] rules) {
            LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
            for (String[] rule : rules) {
                grouped.computeIfAbsent(rule[0], k -> new ArrayList<>()).add(rule[1]);
            }
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                String name = entry.getKey();
                List<String> patterns = entry.getValue();
                StringBuilder joined = new StringBuilder();
                for (int i = 0; i < patterns.size(); i++) {
                    if (i > 0) joined.append("|");
                    joined.append("(").append(patterns.get(i)).append(")");
                }
                String fragment = "(" + joined + ")";
                result.add(fragment);
                groupMappings.add(new GroupMapping(name, 0));
            }
            return result;
        }

        private Pattern compileRules(String[][] rules) {
            groupMappings.clear();
            List<String> fragments = convertRules(rules);
            String combined = String.join("|", fragments);
            int groupIndex = 1;
            for (int i = 0; i < fragments.size(); i++) {
                groupMappings.set(i, new GroupMapping(groupMappings.get(i).name, groupIndex));
                groupIndex += countCapturingGroups(fragments.get(i));
            }
            return Pattern.compile(combined);
        }

        private int countCapturingGroups(String pattern) {
            int count = 0;
            boolean escaped = false;
            boolean inCharClass = false;
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (escaped) { escaped = false; continue; }
                if (c == '\\') { escaped = true; continue; }
                if (c == '[') { inCharClass = true; continue; }
                if (c == ']') { inCharClass = false; continue; }
                if (inCharClass) continue;
                if (c == '(' && (i + 1 >= pattern.length() || pattern.charAt(i + 1) != '?')) {
                    count++;
                }
            }
            return count;
        }

        private List<Token> tokenizeLine(String line, int lineNum) {
            List<Token> tokens = new ArrayList<>();
            int pos = 0;
            while (pos < line.length()) {
                Matcher matcher = regex.matcher(line);
                matcher.region(pos, line.length());
                if (matcher.lookingAt()) {
                    String matchedName = null;
                    String matchedValue = null;
                    for (GroupMapping gm : groupMappings) {
                        String groupVal = matcher.group(gm.groupIndex);
                        if (groupVal != null) {
                            matchedName = gm.name;
                            matchedValue = groupVal;
                            break;
                        }
                    }
                    if (matchedName != null) {
                        pos = matcher.end();
                        if (!IGNORE_TOKENS.contains(matchedName)) {
                            Object value = matchedValue;
                            if (matchedName.equals("STRING")) {
                                value = decodeStr(matchedValue);
                            } else if (matchedName.equals("NUMBER")) {
                                value = decodeNum(matchedValue);
                            } else if (matchedName.equals("NAME") && KEYWORDS.containsKey(matchedValue)) {
                                matchedName = KEYWORDS.get(matchedValue);
                                value = null;
                            }
                            tokens.add(new Token(matchedName, value, lineNum, matcher.start() + 1));
                        }
                    } else {
                        throw new LexerError("Unexpected character " + line.charAt(pos), lineNum, pos + 1);
                    }
                } else {
                    throw new LexerError("Unexpected character " + line.charAt(pos), lineNum, pos + 1);
                }
            }
            return tokens;
        }

        private int countLeadingCharacters(String line, char ch) {
            int count = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) != ch) break;
                count++;
            }
            return count;
        }

        private String detectIndent(String line) {
            if (line.length() > 0 && (line.charAt(0) == ' ' || line.charAt(0) == '\t')) {
                char indentChar = line.charAt(0);
                int count = countLeadingCharacters(line, indentChar);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count; i++) sb.append(indentChar);
                return sb.toString();
            }
            return null;
        }

        private int countOccurrences(String line, String symbol) {
            int count = 0;
            int len = symbol.length();
            while (count * len + len <= line.length()
                    && line.substring(count * len, count * len + len).equals(symbol)) {
                count++;
            }
            return count;
        }

        private String rtrim(String s) {
            int end = s.length();
            while (end > 0 && (s.charAt(end - 1) == ' ' || s.charAt(end - 1) == '\t'
                    || s.charAt(end - 1) == '\r')) {
                end--;
            }
            return s.substring(0, end);
        }

        public List<Token> tokenize(String s) {
            String indentSymbol = null;
            List<Token> tokens = new ArrayList<>();
            int lastIndentLevel = 0;
            int lineNum = 0;

            String[] lines = s.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                lineNum = i + 1;
                String line = rtrim(lines[i]);

                if (line.isEmpty()) {
                    sourceLines.add("");
                    continue;
                }

                if (indentSymbol == null) {
                    indentSymbol = detectIndent(line);
                }

                int indentLevel = 0;
                if (indentSymbol != null) {
                    indentLevel = countOccurrences(line, indentSymbol);
                    line = line.substring(indentLevel * indentSymbol.length());
                }

                sourceLines.add(line);

                List<Token> lineTokens = tokenizeLine(line, lineNum);
                if (!lineTokens.isEmpty()) {
                    if (indentLevel != lastIndentLevel) {
                        if (indentLevel > lastIndentLevel) {
                            for (int j = 0; j < indentLevel - lastIndentLevel; j++) {
                                tokens.add(new Token("INDENT", null, lineNum, 0));
                            }
                        } else {
                            for (int j = 0; j < lastIndentLevel - indentLevel; j++) {
                                tokens.add(new Token("DEDENT", null, lineNum, 0));
                            }
                        }
                        lastIndentLevel = indentLevel;
                    }
                    tokens.addAll(lineTokens);
                    tokens.add(new Token("NEWLINE", null, lineNum, line.length() + 1));
                }
            }

            if (lastIndentLevel > 0) {
                for (int j = 0; j < lastIndentLevel; j++) {
                    tokens.add(new Token("DEDENT", null, lineNum, 0));
                }
            }

            return tokens;
        }
    }

    // ========================================================================
    // TokenStream (from lexer.py) -- fully implemented helper
    // ========================================================================

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

    // ========================================================================
    // Parser (from parser.py) -- fully implemented helper
    // ========================================================================

    public static class Parser {
        List<String> scope;

        public Parser() {
            this.scope = null;
        }

        public Program parse(TokenStream tokens) {
            this.scope = new ArrayList<>();
            List<AstNode> statements = parseStatements(tokens);
            tokens.expectEnd();
            return new Program(statements);
        }

        static final Map<String, Integer> PRECEDENCE = new LinkedHashMap<>();
        static {
            PRECEDENCE.put("call", 10);
            PRECEDENCE.put("subscript", 10);
            PRECEDENCE.put("unary", 9);
            PRECEDENCE.put("*", 7);
            PRECEDENCE.put("/", 7);
            PRECEDENCE.put("%", 7);
            PRECEDENCE.put("+", 6);
            PRECEDENCE.put("-", 6);
            PRECEDENCE.put(">", 5);
            PRECEDENCE.put(">=", 5);
            PRECEDENCE.put("<", 5);
            PRECEDENCE.put("<=", 5);
            PRECEDENCE.put("==", 4);
            PRECEDENCE.put("!=", 4);
            PRECEDENCE.put("&&", 3);
            PRECEDENCE.put("||", 2);
            PRECEDENCE.put("..", 1);
            PRECEDENCE.put("...", 1);
        }

        public AstNode parseExpression(TokenStream tokens, int precedence) {
            AstNode left = parsePrefixExpression(tokens);
            if (left == null) return null;
            while (precedence < getNextPrecedence(tokens)) {
                AstNode op = parseInfixExpression(tokens, left);
                if (op != null) { left = op; } else { break; }
            }
            return left;
        }

        public AstNode parseExpression(TokenStream tokens) {
            return parseExpression(tokens, 0);
        }

        private AstNode parsePrefixExpression(TokenStream tokens) {
            if (tokens.isEnd()) return null;
            Token token = tokens.current();
            switch (token.getName()) {
                case "NUMBER": return parseNumberExpression(tokens);
                case "STRING": return parseStringExpression(tokens);
                case "NAME": return parseNameExpression(tokens);
                case "LPAREN": return parseGroupExpression(tokens);
                case "LBRACK": return parseArrayExpression(tokens);
                case "LCBRACK": return parseDictionaryExpression(tokens);
                case "OPERATOR": return parseUnaryOperatorExpression(tokens);
                default: return null;
            }
        }

        private AstNode parseInfixExpression(TokenStream tokens, AstNode left) {
            if (tokens.isEnd()) return null;
            Token token = tokens.current();
            switch (token.getName()) {
                case "OPERATOR": return parseBinaryOperatorExpression(tokens, left);
                case "LPAREN": return parseCallExpression(tokens, left);
                case "LBRACK": return parseSubscriptExpression(tokens, left);
                default: return null;
            }
        }

        private int getInfixPrecedence(Token token) {
            switch (token.getName()) {
                case "OPERATOR":
                    Integer p = PRECEDENCE.get(token.getValue().toString());
                    return p != null ? p : 0;
                case "LPAREN": return PRECEDENCE.get("call");
                case "LBRACK": return PRECEDENCE.get("subscript");
                default: return 0;
            }
        }

        private int getNextPrecedence(TokenStream tokens) {
            if (!tokens.isEnd()) {
                Token token = tokens.current();
                String name = token.getName();
                if (name.equals("OPERATOR") || name.equals("LPAREN") || name.equals("LBRACK")) {
                    return getInfixPrecedence(token);
                }
            }
            return 0;
        }

        private AstNode parseNumberExpression(TokenStream tokens) {
            Token token = tokens.consumeExpected("NUMBER");
            return new Number(token.getValue());
        }

        private AstNode parseStringExpression(TokenStream tokens) {
            Token token = tokens.consumeExpected("STRING");
            return new StringNode((String) token.getValue());
        }

        private AstNode parseNameExpression(TokenStream tokens) {
            Token token = tokens.consumeExpected("NAME");
            return new Identifier((String) token.getValue());
        }

        private AstNode parseGroupExpression(TokenStream tokens) {
            tokens.consumeExpected("LPAREN");
            AstNode expr = parseExpression(tokens);
            tokens.consumeExpected("RPAREN");
            return expr;
        }

        private AstNode parseArrayExpression(TokenStream tokens) {
            tokens.consumeExpected("LBRACK");
            List<AstNode> items = parseListOfExpressions(tokens);
            tokens.consumeExpected("RBRACK");
            return new Array(items);
        }

        private AstNode parseDictionaryExpression(TokenStream tokens) {
            tokens.consumeExpected("LCBRACK");
            List<Map.Entry<AstNode, AstNode>> items = new ArrayList<>();
            while (!tokens.isEnd()) {
                AstNode key = parseExpression(tokens);
                if (key != null) {
                    tokens.consumeExpected("COLON");
                    AstNode value = parseExpression(tokens);
                    if (value == null) throw new ParserError("Dictionary value expected", tokens.consume());
                    items.add(new AbstractMap.SimpleEntry<>(key, value));
                } else { break; }
                if (tokens.current().getName().equals("COMMA")) { tokens.consumeExpected("COMMA"); } else { break; }
            }
            tokens.consumeExpected("RCBRACK");
            return new Dictionary(items);
        }

        private AstNode parseUnaryOperatorExpression(TokenStream tokens) {
            Token token = tokens.consumeExpected("OPERATOR");
            String op = (String) token.getValue();
            if (!op.equals("-") && !op.equals("!")) {
                throw new ParserError("Unary operator " + op + " is not supported", token);
            }
            AstNode right = parseExpression(tokens, PRECEDENCE.get("unary"));
            if (right == null) throw new ParserError("Expected expression", tokens.consume());
            return new UnaryOperator(op, right);
        }

        private AstNode parseBinaryOperatorExpression(TokenStream tokens, AstNode left) {
            Token token = tokens.consumeExpected("OPERATOR");
            String op = (String) token.getValue();
            int prec = PRECEDENCE.getOrDefault(op, 0);
            AstNode right = parseExpression(tokens, prec);
            if (right == null) throw new ParserError("Expected expression", tokens.consume());
            return new BinaryOperator(op, left, right);
        }

        private AstNode parseCallExpression(TokenStream tokens, AstNode left) {
            tokens.consumeExpected("LPAREN");
            List<AstNode> arguments = parseListOfExpressions(tokens);
            tokens.consumeExpected("RPAREN");
            return new Call(left, arguments);
        }

        private AstNode parseSubscriptExpression(TokenStream tokens, AstNode left) {
            tokens.consumeExpected("LBRACK");
            AstNode key = parseExpression(tokens);
            if (key == null) throw new ParserError("Subscript operator key is required", tokens.current());
            tokens.consumeExpected("RBRACK");
            return new SubscriptOperator(left, key);
        }

        private List<AstNode> parseListOfExpressions(TokenStream tokens) {
            List<AstNode> items = new ArrayList<>();
            while (!tokens.isEnd()) {
                AstNode exp = parseExpression(tokens);
                if (exp != null) { items.add(exp); } else { break; }
                if (tokens.current().getName().equals("COMMA")) { tokens.consumeExpected("COMMA"); } else { break; }
            }
            return items;
        }

        private List<AstNode> parseBlock(TokenStream tokens) {
            tokens.consumeExpected("NEWLINE", "INDENT");
            List<AstNode> statements = parseStatements(tokens);
            tokens.consumeExpected("DEDENT");
            return statements;
        }

        public List<AstNode> parseStatements(TokenStream tokens) {
            List<AstNode> statements = new ArrayList<>();
            while (!tokens.isEnd()) {
                AstNode statement = parseStatement(tokens);
                if (statement != null) { statements.add(statement); } else { break; }
            }
            return statements;
        }

        private AstNode parseStatement(TokenStream tokens) {
            if (tokens.isEnd()) return null;
            Token token = tokens.current();
            switch (token.getName()) {
                case "FUNCTION": return parseFunctionStatement(tokens);
                case "IF": return parseConditionalStatement(tokens);
                case "MATCH": return parseMatchStatement(tokens);
                case "WHILE": return parseWhileLoopStatement(tokens);
                case "FOR": return parseForLoopStatement(tokens);
                case "RETURN": return parseReturnStatement(tokens);
                case "BREAK": return parseBreakStatement(tokens);
                case "CONTINUE": return parseContinueStatement(tokens);
                default: return parseExpressionStatement(tokens);
            }
        }

        private AstNode parseFunctionStatement(TokenStream tokens) {
            tokens.consumeExpected("FUNCTION");
            Token idToken = tokens.consumeExpected("NAME");
            tokens.consumeExpected("LPAREN");
            List<String> params = parseFunctionParams(tokens);
            tokens.consumeExpected("RPAREN");
            tokens.consumeExpected("COLON");
            scope.add("function");
            List<AstNode> block = parseBlock(tokens);
            scope.remove(scope.size() - 1);
            if (block == null) throw new ParserError("Expected function body", tokens.current());
            return new Function((String) idToken.getValue(), params, block);
        }

        private List<String> parseFunctionParams(TokenStream tokens) {
            List<String> params = new ArrayList<>();
            if (tokens.current().getName().equals("NAME")) {
                while (!tokens.isEnd()) {
                    Token idToken = tokens.consumeExpected("NAME");
                    params.add((String) idToken.getValue());
                    if (tokens.current().getName().equals("COMMA")) { tokens.consumeExpected("COMMA"); } else { break; }
                }
            }
            return params;
        }

        private AstNode parseConditionalStatement(TokenStream tokens) {
            tokens.consumeExpected("IF");
            AstNode test = parseExpression(tokens);
            if (test == null) throw new ParserError("Expected `if` condition", tokens.current());
            tokens.consumeExpected("COLON");
            List<AstNode> ifBlock = parseBlock(tokens);
            if (ifBlock == null) throw new ParserError("Expected if body", tokens.current());
            List<ConditionElif> elifConditions = new ArrayList<>();
            while (!tokens.isEnd() && tokens.current().getName().equals("ELIF")) {
                tokens.consumeExpected("ELIF");
                AstNode elifTest = parseExpression(tokens);
                if (elifTest == null) throw new ParserError("Expected `elif` condition", tokens.current());
                tokens.consumeExpected("COLON");
                List<AstNode> elifBlock = parseBlock(tokens);
                if (elifBlock == null) throw new ParserError("Expected `elif` body", tokens.current());
                elifConditions.add(new ConditionElif(elifTest, elifBlock));
            }
            List<AstNode> elseBlock = null;
            if (!tokens.isEnd() && tokens.current().getName().equals("ELSE")) {
                tokens.consumeExpected("ELSE", "COLON");
                elseBlock = parseBlock(tokens);
                if (elseBlock == null) throw new ParserError("Expected `else` body", tokens.current());
            }
            return new Condition(test, ifBlock, elifConditions, elseBlock);
        }

        private AstNode parseMatchStatement(TokenStream tokens) {
            tokens.consumeExpected("MATCH");
            AstNode test = parseExpression(tokens);
            tokens.consumeExpected("COLON", "NEWLINE", "INDENT");
            List<MatchPattern> patterns = new ArrayList<>();
            while (!tokens.isEnd() && tokens.current().getName().equals("WHEN")) {
                tokens.consumeExpected("WHEN");
                AstNode pattern = parseExpression(tokens);
                if (pattern == null) throw new ParserError("Pattern expression expected", tokens.current());
                tokens.consumeExpected("COLON");
                List<AstNode> block = parseBlock(tokens);
                patterns.add(new MatchPattern(pattern, block));
            }
            if (patterns.isEmpty()) throw new ParserError("One or more `when` pattern excepted", tokens.current());
            List<AstNode> elseBlock = null;
            if (!tokens.isEnd() && tokens.current().getName().equals("ELSE")) {
                tokens.consumeExpected("ELSE", "COLON");
                elseBlock = parseBlock(tokens);
                if (elseBlock == null) throw new ParserError("Expected `else` body", tokens.current());
            }
            tokens.consumeExpected("DEDENT");
            return new Match(test, patterns, elseBlock);
        }

        private AstNode parseWhileLoopStatement(TokenStream tokens) {
            tokens.consumeExpected("WHILE");
            AstNode test = parseExpression(tokens);
            if (test == null) throw new ParserError("While condition expected", tokens.current());
            tokens.consumeExpected("COLON");
            scope.add("loop");
            List<AstNode> block = parseBlock(tokens);
            scope.remove(scope.size() - 1);
            if (block == null) throw new ParserError("Expected loop body", tokens.current());
            return new WhileLoop(test, block);
        }

        private AstNode parseForLoopStatement(TokenStream tokens) {
            tokens.consumeExpected("FOR");
            Token idToken = tokens.consumeExpected("NAME");
            tokens.consumeExpected("IN");
            AstNode collection = parseExpression(tokens);
            tokens.consumeExpected("COLON");
            scope.add("loop");
            List<AstNode> block = parseBlock(tokens);
            scope.remove(scope.size() - 1);
            if (block == null) throw new ParserError("Expected loop body", tokens.current());
            return new ForLoop((String) idToken.getValue(), collection, block);
        }

        private AstNode parseReturnStatement(TokenStream tokens) {
            if (scope == null || !scope.contains("function"))
                throw new ParserError("Return outside of function", tokens.current());
            tokens.consumeExpected("RETURN");
            AstNode value = parseExpression(tokens);
            tokens.consumeExpected("NEWLINE");
            return new Return(value);
        }

        private AstNode parseBreakStatement(TokenStream tokens) {
            if (scope == null || scope.isEmpty() || !scope.get(scope.size() - 1).equals("loop"))
                throw new ParserError("Break outside of loop", tokens.current());
            tokens.consumeExpected("BREAK", "NEWLINE");
            return new Break();
        }

        private AstNode parseContinueStatement(TokenStream tokens) {
            if (scope == null || scope.isEmpty() || !scope.get(scope.size() - 1).equals("loop"))
                throw new ParserError("Continue outside of loop", tokens.current());
            tokens.consumeExpected("CONTINUE", "NEWLINE");
            return new Continue();
        }

        private AstNode parseExpressionStatement(TokenStream tokens) {
            AstNode exp = parseExpression(tokens);
            if (exp != null) {
                if (!tokens.isEnd() && tokens.current().getName().equals("ASSIGN")) {
                    tokens.consumeExpected("ASSIGN");
                    AstNode right = parseExpression(tokens);
                    tokens.consumeExpected("NEWLINE");
                    return new Assignment(exp, right);
                } else {
                    tokens.consumeExpected("NEWLINE");
                    return exp;
                }
            }
            return null;
        }
    }

    // ========================================================================
    // reportSyntaxError -- fully implemented helper
    // ========================================================================

    public static void reportSyntaxError(Lexer lexer, AbrvalgSyntaxError error) {
        int line = error.getLine();
        int column = error.getColumn();
        String sourceLine = lexer.getSourceLines().get(line - 1);
        System.out.println("Syntax error: " + error.getMsg() + " at line " + line + ", column " + column);
        StringBuilder arrow = new StringBuilder();
        for (int i = 0; i < column - 1; i++) arrow.append(' ');
        System.out.println(sourceLine + "\n" + arrow + "^");
    }

    // ========================================================================
    // BuiltinFunction -- fully implemented helper
    // ========================================================================

    public static class BuiltinFunction {
        private final List<String> params;
        private final BiFunction<Map<String, Object>, Environment, Object> body;

        public BuiltinFunction(List<String> params, BiFunction<Map<String, Object>, Environment, Object> body) {
            this.params = params;
            this.body = body;
        }

        public List<String> getParams() { return params; }
        public BiFunction<Map<String, Object>, Environment, Object> getBody() { return body; }
    }

    // ========================================================================
    // Control flow exceptions -- fully implemented helper
    // ========================================================================

    public static class BreakException extends RuntimeException {
        public BreakException() { super(); }
    }

    public static class ContinueException extends RuntimeException {
        public ContinueException() { super(); }
    }

    public static class ReturnException extends RuntimeException {
        private final Object value;
        public ReturnException(Object value) {
            super();
            this.value = value;
        }
        public Object getReturnValue() { return value; }
    }

    // ========================================================================
    // Custom exception types -- fully implemented helper
    // ========================================================================

    public static class TypeError extends RuntimeException {
        public TypeError(String message) { super(message); }
    }

    public static class NameError extends RuntimeException {
        public NameError(String message) { super(message); }
    }

    // ========================================================================
    // Environment -- STUB (interpreter entity)
    // ========================================================================

    public static class Environment {
        private Environment _parent;
        private final Map<String, Object> _values;

        public Environment() {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public Environment(Environment parent) {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public Environment(Environment parent, Map<String, Object> args) {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public void fromDict(Map<String, Object> args) {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public void set(String key, Object val) {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public Object get(String key) {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public Map<String, Object> asdict() {
            // TODO: implement
            throw new UnsupportedOperationException("Not implemented");
        }

        public Environment getParent() { return _parent; }
        public Map<String, Object> getValues() { return _values; }

        @Override
        public String toString() {
            return "Environment(" + _values + ")";
        }
    }

    // ========================================================================
    // Evaluator map -- STUB (interpreter entity)
    // ========================================================================

    public static final Map<Class<? extends AstNode>, BiFunction<AstNode, Environment, Object>> evaluators = new LinkedHashMap<>();
    // TODO: populate evaluators map with entries for all 16 AST node types

    // ========================================================================
    // eval functions -- STUBS (interpreter entities)
    // ========================================================================

    public static Object eval_binary_operator(BinaryOperator node, Environment env) {
        // TODO: implement — handle +, -, *, /, %, >, >=, <, <=, ==, !=, &&, ||, .., ...
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_unary_operator(UnaryOperator node, Environment env) {
        // TODO: implement — handle - (negate) and ! (not)
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void eval_assignment(Assignment node, Environment env) {
        // TODO: implement — if left is SubscriptOperator, delegate to eval_setitem;
        //       otherwise store eval_expression(right) in env under left.getValue()
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_condition(Condition node, Environment env) {
        // TODO: implement — evaluate if/elif/else chain
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_match(Match node, Environment env) {
        // TODO: implement — evaluate test, compare with each pattern via ==, run matching body
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void eval_while_loop(WhileLoop node, Environment env) {
        // TODO: implement — loop while test is truthy, handle Break/Continue exceptions
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void eval_for_loop(ForLoop node, Environment env) {
        // TODO: implement — iterate collection, set var, eval body, handle Break/Continue
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void eval_function_declaration(Function node, Environment env) {
        // TODO: implement — store function node in env under node.getName()
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_call(Call node, Environment env) {
        // TODO: implement — look up function, check arg count, create scope, eval body
        //       handle BuiltinFunction vs user Function, catch ReturnException
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_identifier(Identifier node, Environment env) {
        // TODO: implement — look up name in env, throw NameError if null
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_getitem(SubscriptOperator node, Environment env) {
        // TODO: implement — subscript access for List and Map
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void eval_setitem(Assignment node, Environment env) {
        // TODO: implement — subscript assignment for List and Map
        throw new UnsupportedOperationException("Not implemented");
    }

    public static List<Object> eval_array(Array node, Environment env) {
        // TODO: implement — evaluate each item in node.getItems()
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Map<Object, Object> eval_dict(Dictionary node, Environment env) {
        // TODO: implement — evaluate each key-value pair in node.getItems()
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_return(Return node, Environment env) {
        // TODO: implement — evaluate value expression if not null, else return null
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_node(Object node, Environment env) {
        // TODO: implement — dispatch to evaluator by node class
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_expression(AstNode node, Environment env) {
        // TODO: implement — delegate to eval_node
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_statement(AstNode node, Environment env) {
        // TODO: implement — delegate to eval_node
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object eval_statements(List<AstNode> statements, Environment env) {
        // TODO: implement — iterate statements, handle Break/Continue/Return
        throw new UnsupportedOperationException("Not implemented");
    }

    // ========================================================================
    // Builtins -- STUB (interpreter entity)
    // ========================================================================

    public static void add_builtins(Environment env) {
        // TODO: implement — register print, len, slice, str, int as BuiltinFunction
        throw new UnsupportedOperationException("Not implemented");
    }

    // ========================================================================
    // create_global_env / evaluate_env / evaluate -- STUBS (interpreter entities)
    // ========================================================================

    public static Environment create_global_env() {
        // TODO: implement — create Environment, add builtins, return
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object evaluate_env(String s, Environment env) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object evaluate_env(String s, Environment env, boolean verbose) {
        // TODO: implement — tokenize, parse, eval_statements
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object evaluate(String s) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    public static Object evaluate(String s, boolean verbose) {
        // TODO: implement — create global env, call evaluate_env
        throw new UnsupportedOperationException("Not implemented");
    }
}
