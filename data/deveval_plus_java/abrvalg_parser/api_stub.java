import java.util.*;
import java.util.regex.*;

/**
 * API Stub for AstParser.
 *
 * Dependency types (AST nodes, errors, Token, Lexer, TokenStream) are fully
 * implemented. Parser functions are stubs (throw UnsupportedOperationException).
 */
public class AstParser {

    // ========================================================================
    // Error types (from abrvalg/errors.py) -- IMPLEMENTED
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
    // AST node types (from abrvalg/ast.py) -- IMPLEMENTED
    // ========================================================================

    public interface AstNode {}

    public static class Number implements AstNode {
        private final Object value;
        public Number(Object value) { this.value = value; }
        public Object getValue() { return value; }
    }

    public static class StringNode implements AstNode {
        private final String value;
        public StringNode(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    public static class Identifier implements AstNode {
        private final String value;
        public Identifier(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    public static class Assignment implements AstNode {
        private final AstNode left;
        private final AstNode right;
        public Assignment(AstNode left, AstNode right) { this.left = left; this.right = right; }
        public AstNode getLeft() { return left; }
        public AstNode getRight() { return right; }
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
    }

    public static class UnaryOperator implements AstNode {
        private final String operator;
        private final AstNode right;
        public UnaryOperator(String operator, AstNode right) { this.operator = operator; this.right = right; }
        public String getOperator() { return operator; }
        public AstNode getRight() { return right; }
    }

    public static class Call implements AstNode {
        private final AstNode left;
        private final List<AstNode> arguments;
        public Call(AstNode left, List<AstNode> arguments) { this.left = left; this.arguments = arguments; }
        public AstNode getLeft() { return left; }
        public List<AstNode> getArguments() { return arguments; }
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
    }

    public static class ConditionElif implements AstNode {
        private final AstNode test;
        private final List<AstNode> body;
        public ConditionElif(AstNode test, List<AstNode> body) { this.test = test; this.body = body; }
        public AstNode getTest() { return test; }
        public List<AstNode> getBody() { return body; }
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
    }

    public static class MatchPattern implements AstNode {
        private final AstNode pattern;
        private final List<AstNode> body;
        public MatchPattern(AstNode pattern, List<AstNode> body) { this.pattern = pattern; this.body = body; }
        public AstNode getPattern() { return pattern; }
        public List<AstNode> getBody() { return body; }
    }

    public static class WhileLoop implements AstNode {
        private final AstNode test;
        private final List<AstNode> body;
        public WhileLoop(AstNode test, List<AstNode> body) { this.test = test; this.body = body; }
        public AstNode getTest() { return test; }
        public List<AstNode> getBody() { return body; }
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
    }

    public static class Break implements AstNode {}

    public static class Continue implements AstNode {}

    public static class Return implements AstNode {
        private final AstNode value;
        public Return(AstNode value) { this.value = value; }
        public AstNode getValue() { return value; }
    }

    public static class Array implements AstNode {
        private final List<AstNode> items;
        public Array(List<AstNode> items) { this.items = items; }
        public List<AstNode> getItems() { return items; }
    }

    public static class Dictionary implements AstNode {
        private final List<Map.Entry<AstNode, AstNode>> items;
        public Dictionary(List<Map.Entry<AstNode, AstNode>> items) { this.items = items; }
        public List<Map.Entry<AstNode, AstNode>> getItems() { return items; }
    }

    public static class SubscriptOperator implements AstNode {
        private final AstNode left;
        private final AstNode key;
        public SubscriptOperator(AstNode left, AstNode key) { this.left = left; this.key = key; }
        public AstNode getLeft() { return left; }
        public AstNode getKey() { return key; }
    }

    public static class Program implements AstNode {
        private final List<AstNode> body;
        public Program(List<AstNode> body) { this.body = body; }
        public List<AstNode> getBody() { return body; }
    }

    // ========================================================================
    // Token (from lexer.py) -- IMPLEMENTED
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
    }

    // ========================================================================
    // Lexer (from lexer.py) -- IMPLEMENTED
    // ========================================================================

    public static class Lexer {
        public Lexer() { throw new UnsupportedOperationException("Dependency: see abrvalg_lexer"); }
        public List<Token> tokenize(String s) { throw new UnsupportedOperationException(); }
        public List<String> getSourceLines() { throw new UnsupportedOperationException(); }
    }

    // ========================================================================
    // TokenStream (from lexer.py) -- IMPLEMENTED
    // ========================================================================

    public static class TokenStream {
        private final List<Token> tokens;
        private int pos;

        public TokenStream(List<Token> tokens) { this.tokens = tokens; this.pos = 0; }

        public Token consumeExpected(String... expectedNames) { throw new UnsupportedOperationException("Dependency: see abrvalg_lexer"); }
        public Token consume() { throw new UnsupportedOperationException("Dependency: see abrvalg_lexer"); }
        public Token current() { throw new UnsupportedOperationException("Dependency: see abrvalg_lexer"); }
        public void expectEnd() { throw new UnsupportedOperationException("Dependency: see abrvalg_lexer"); }
        public boolean isEnd() { throw new UnsupportedOperationException("Dependency: see abrvalg_lexer"); }
    }

    // ========================================================================
    // enter_scope -- STUB
    // ========================================================================

    public static class ScopeGuard implements AutoCloseable {
        public ScopeGuard(Parser parser, String name) {
            throw new UnsupportedOperationException("stub");
        }
        @Override public void close() { throw new UnsupportedOperationException("stub"); }
    }

    public static ScopeGuard enterScope(Parser parser, String name) {
        throw new UnsupportedOperationException("stub");
    }

    // ========================================================================
    // Subparser interfaces -- IMPLEMENTED
    // ========================================================================

    public interface PrefixSubparser {
        AstNode parse(Parser parser, TokenStream tokens);
    }

    public interface InfixSubparser {
        AstNode parse(Parser parser, TokenStream tokens, AstNode left);
        int getPrecedence(Token token);
    }

    public static class SubparserLookup {
        public static <T> T getSubparser(Token token, Map<String, java.util.function.Supplier<T>> subparsers, java.util.function.Supplier<T> defaultSupplier) {
            throw new UnsupportedOperationException("stub");
        }
        public static <T> T getSubparser(Token token, Map<String, java.util.function.Supplier<T>> subparsers) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Prefix Subparsers -- STUBS
    // ========================================================================

    public static class NumberExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class StringExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class NameExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class UnaryOperatorExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class GroupExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class ArrayExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class DictionaryExpression implements PrefixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Infix Subparsers -- STUBS
    // ========================================================================

    public static class BinaryOperatorExpression implements InfixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens, AstNode left) {
            throw new UnsupportedOperationException("stub");
        }
        @Override public int getPrecedence(Token token) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class CallExpression implements InfixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens, AstNode left) {
            throw new UnsupportedOperationException("stub");
        }
        @Override public int getPrecedence(Token token) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class SubscriptOperatorExpression implements InfixSubparser {
        @Override public AstNode parse(Parser parser, TokenStream tokens, AstNode left) {
            throw new UnsupportedOperationException("stub");
        }
        @Override public int getPrecedence(Token token) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Expression -- STUB
    // ========================================================================

    public static class Expression {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
        public AstNode parse(Parser parser, TokenStream tokens, int precedence) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // ListOfExpressions -- STUB
    // ========================================================================

    public static class ListOfExpressions {
        public List<AstNode> parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Block -- STUB
    // ========================================================================

    public static class Block {
        public List<AstNode> parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Statement Subparsers -- STUBS
    // ========================================================================

    public static class FunctionStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class ConditionalStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class MatchStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class WhileLoopStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class ForLoopStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class ReturnStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class BreakStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class ContinueStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class AssignmentStatement {
        public AstNode parse(Parser parser, TokenStream tokens, AstNode left) {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static class ExpressionStatement {
        public AstNode parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Statements -- STUB
    // ========================================================================

    public static class Statements {
        public List<AstNode> parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // ProgramParser -- STUB
    // ========================================================================

    public static class ProgramParser {
        public Program parse(Parser parser, TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }

    // ========================================================================
    // Parser -- STUB
    // ========================================================================

    public static class Parser {
        private List<String> scope;

        public Parser() { this.scope = null; }
        public List<String> getScope() { return scope; }
        public void setScope(List<String> scope) { this.scope = scope; }

        public Program parse(TokenStream tokens) {
            throw new UnsupportedOperationException("stub");
        }
    }
}
