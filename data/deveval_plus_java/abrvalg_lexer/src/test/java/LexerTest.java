import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for Lexer.java -- one-to-one mapping from the Python test_lexer.py tests.
 */
public class LexerTest {

    // =======================================================================
    // decode_str tests
    // =======================================================================

    @Test
    void test_decode_str_simple() {
        assertEquals("hello", Lexer.decodeStr("\"hello\""));
    }

    @Test
    void test_decode_str_single_quotes() {
        assertEquals("hello", Lexer.decodeStr("'hello'"));
    }

    @Test
    void test_decode_str_escape_newline() {
        assertEquals("hello\nworld", Lexer.decodeStr("\"hello\\nworld\""));
    }

    @Test
    void test_decode_str_escape_tab() {
        assertEquals("col1\tcol2", Lexer.decodeStr("\"col1\\tcol2\""));
    }

    @Test
    void test_decode_str_escape_return() {
        assertEquals("line\rend", Lexer.decodeStr("\"line\\rend\""));
    }

    @Test
    void test_decode_str_escape_backslash() {
        assertEquals("path\\file", Lexer.decodeStr("\"path\\\\file\""));
    }

    @Test
    void test_decode_str_escape_double_quote() {
        assertEquals("say \"hi\"", Lexer.decodeStr("\"say \\\"hi\\\"\""));
    }

    @Test
    void test_decode_str_empty() {
        assertEquals("", Lexer.decodeStr("\"\""));
    }

    // =======================================================================
    // decode_num tests
    // =======================================================================

    @Test
    void test_decode_num_integer() {
        Number result = Lexer.decodeNum("42");
        assertEquals(42, result.intValue());
        assertTrue(result instanceof Integer);
    }

    @Test
    void test_decode_num_float() {
        Number result = Lexer.decodeNum("3.14");
        assertEquals(3.14, result.doubleValue(), 0.0001);
        assertTrue(result instanceof Double);
    }

    @Test
    void test_decode_num_zero() {
        assertEquals(0, Lexer.decodeNum("0").intValue());
    }

    // =======================================================================
    // Token.__repr__ tests (toString in Java)
    // =======================================================================

    @Test
    void test_Token___repr___basic() {
        Lexer.Token t = new Lexer.Token("NUMBER", 42, 1, 1);
        assertEquals("('NUMBER', 42, 1, 1)", t.toString());
    }

    @Test
    void test_Token___repr___string() {
        Lexer.Token t = new Lexer.Token("STRING", "hello", 2, 5);
        assertEquals("('STRING', 'hello', 2, 5)", t.toString());
    }

    @Test
    void test_Token___repr___none_value() {
        Lexer.Token t = new Lexer.Token("IF", null, 1, 1);
        assertEquals("('IF', None, 1, 1)", t.toString());
    }

    // =======================================================================
    // Lexer.__init__ tests
    // =======================================================================

    @Test
    void test_Lexer___init___source_lines() {
        Lexer lexer = new Lexer();
        assertTrue(lexer.getSourceLines().isEmpty());
    }

    @Test
    void test_Lexer___init___regex() {
        Lexer lexer = new Lexer();
        assertNotNull(lexer.getRegex());
    }

    // =======================================================================
    // Lexer._convert_rules tests
    // =======================================================================

    @Test
    void test_Lexer__convert_rules_single() {
        Lexer fresh = new Lexer();
        String[][] rules = {{"NUMBER", "\\d+"}};
        List<String> result = fresh.convertRules(rules);
        assertEquals(1, result.size());
        // The fragment should contain the pattern for NUMBER
        assertTrue(result.get(0).contains("\\d+"));
    }

    @Test
    void test_Lexer__convert_rules_merged() {
        Lexer fresh = new Lexer();
        String[][] rules = {{"NUMBER", "\\d+\\.\\d+"}, {"NUMBER", "\\d+"}};
        List<String> result = fresh.convertRules(rules);
        assertEquals(1, result.size()); // Same name merged into one group
    }

    // =======================================================================
    // Lexer._compile_rules tests
    // =======================================================================

    @Test
    void test_Lexer__compile_rules_returns_pattern() {
        Lexer fresh = new Lexer();
        String[][] rules = {{"NUMBER", "\\d+"}, {"NAME", "[a-zA-Z_]\\w*"}};
        java.util.regex.Pattern regex = fresh.compileRules(rules);
        assertTrue(regex.matcher("42").lookingAt());
        assertTrue(regex.matcher("hello").lookingAt());
    }

    // =======================================================================
    // Lexer._tokenize_line tests
    // =======================================================================

    @Test
    void test_Lexer__tokenize_line_number() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("42", 1);
        assertEquals(1, tokens.size());
        assertEquals("NUMBER", tokens.get(0).getName());
        assertEquals(42, ((Number) tokens.get(0).getValue()).intValue());
    }

    @Test
    void test_Lexer__tokenize_line_string() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("\"hello\"", 1);
        assertEquals(1, tokens.size());
        assertEquals("STRING", tokens.get(0).getName());
        assertEquals("hello", tokens.get(0).getValue());
    }

    @Test
    void test_Lexer__tokenize_line_operator() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("2 + 3", 1);
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("NUMBER", "OPERATOR", "NUMBER"), names);
    }

    @Test
    void test_Lexer__tokenize_line_keyword() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("if x", 1);
        assertEquals("IF", tokens.get(0).getName());
        assertNull(tokens.get(0).getValue());
    }

    @Test
    void test_Lexer__tokenize_line_skips_whitespace() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("a   b", 1);
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        assertFalse(names.contains("WHITESPACE"));
    }

    @Test
    void test_Lexer__tokenize_line_skips_comment() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("x # comment", 1);
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        assertFalse(names.contains("COMMENT"));
    }

    @Test
    void test_Lexer__tokenize_line_unexpected_char() {
        Lexer lexer = new Lexer();
        assertThrows(Lexer.AbrvalgSyntaxError.class, () -> {
            lexer.tokenizeLine("$", 1);
        });
    }

    @Test
    void test_Lexer__tokenize_line_column() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenizeLine("x + y", 1);
        assertEquals(1, tokens.get(0).getColumn());
        assertEquals(5, tokens.get(2).getColumn());
    }

    // =======================================================================
    // Lexer._count_leading_characters tests
    // =======================================================================

    @Test
    void test_Lexer__count_leading_characters_spaces() {
        Lexer lexer = new Lexer();
        assertEquals(4, lexer.countLeadingCharacters("    hello", ' '));
    }

    @Test
    void test_Lexer__count_leading_characters_tabs() {
        Lexer lexer = new Lexer();
        assertEquals(2, lexer.countLeadingCharacters("\t\thello", '\t'));
    }

    @Test
    void test_Lexer__count_leading_characters_none() {
        Lexer lexer = new Lexer();
        assertEquals(0, lexer.countLeadingCharacters("hello", ' '));
    }

    // =======================================================================
    // Lexer._detect_indent tests
    // =======================================================================

    @Test
    void test_Lexer__detect_indent_spaces() {
        Lexer lexer = new Lexer();
        assertEquals("    ", lexer.detectIndent("    hello"));
    }

    @Test
    void test_Lexer__detect_indent_tab() {
        Lexer lexer = new Lexer();
        assertEquals("\t", lexer.detectIndent("\thello"));
    }

    @Test
    void test_Lexer__detect_indent_no_indent() {
        Lexer lexer = new Lexer();
        assertNull(lexer.detectIndent("hello"));
    }

    // =======================================================================
    // Lexer.tokenize tests
    // =======================================================================

    @Test
    void test_Lexer_tokenize_simple_expression() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("1 + 2");
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("NUMBER", "OPERATOR", "NUMBER", "NEWLINE"), names);
    }

    @Test
    void test_Lexer_tokenize_assignment() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("x = 42");
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("NAME", "ASSIGN", "NUMBER", "NEWLINE"), names);
    }

    @Test
    void test_Lexer_tokenize_indent_dedent() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("if x:\n    y");
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        assertTrue(names.contains("INDENT"));
        assertTrue(names.contains("DEDENT"));
    }

    @Test
    void test_Lexer_tokenize_multiple_indent_levels() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("a\n    b\n        c\n    d\ne");
        List<String> names = tokens.stream().map(Lexer.Token::getName).collect(Collectors.toList());
        long indentCount = names.stream().filter(n -> n.equals("INDENT")).count();
        long dedentCount = names.stream().filter(n -> n.equals("DEDENT")).count();
        assertEquals(indentCount, dedentCount);
    }

    @Test
    void test_Lexer_tokenize_empty_lines_skipped() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("a\n\nb");
        List<Object> values = tokens.stream()
                .filter(t -> t.getName().equals("NAME"))
                .map(Lexer.Token::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b"), values);
    }

    @Test
    void test_Lexer_tokenize_keywords() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("if while for func return");
        List<String> keywordNames = tokens.stream()
                .map(Lexer.Token::getName)
                .filter(n -> !n.equals("NEWLINE"))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("IF", "WHILE", "FOR", "FUNCTION", "RETURN"), keywordNames);
    }

    @Test
    void test_Lexer_tokenize_all_operators() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("+ - * / %");
        List<Object> ops = tokens.stream()
                .filter(t -> t.getName().equals("OPERATOR"))
                .map(Lexer.Token::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("+", "-", "*", "/", "%"), ops);
    }

    @Test
    void test_Lexer_tokenize_comparison_operators() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("< > <= >= == !=");
        List<Object> ops = tokens.stream()
                .filter(t -> t.getName().equals("OPERATOR"))
                .map(Lexer.Token::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("<", ">", "<=", ">=", "==", "!="), ops);
    }

    @Test
    void test_Lexer_tokenize_range_operators() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("1..5");
        List<Object> ops = tokens.stream()
                .filter(t -> t.getName().equals("OPERATOR"))
                .map(Lexer.Token::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(".."), ops);
    }

    @Test
    void test_Lexer_tokenize_range_inclusive() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("1...5");
        List<Object> ops = tokens.stream()
                .filter(t -> t.getName().equals("OPERATOR"))
                .map(Lexer.Token::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("..."), ops);
    }

    @Test
    void test_Lexer_tokenize_boolean_operators() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("a && b || c");
        List<Object> ops = tokens.stream()
                .filter(t -> t.getName().equals("OPERATOR"))
                .map(Lexer.Token::getValue)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("&&", "||"), ops);
    }

    @Test
    void test_Lexer_tokenize_brackets() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("f(x)[0]");
        List<String> names = tokens.stream()
                .map(Lexer.Token::getName)
                .filter(n -> !n.equals("NEWLINE"))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("NAME", "LPAREN", "NAME", "RPAREN", "LBRACK", "NUMBER", "RBRACK"), names);
    }

    @Test
    void test_Lexer_tokenize_dict_braces() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("{\"a\": 1}");
        List<String> names = tokens.stream()
                .map(Lexer.Token::getName)
                .filter(n -> !n.equals("NEWLINE"))
                .collect(Collectors.toList());
        assertTrue(names.contains("LCBRACK"));
        assertTrue(names.contains("RCBRACK"));
    }

    @Test
    void test_Lexer_tokenize_source_lines() {
        Lexer lexer = new Lexer();
        lexer.tokenize("x = 1\ny = 2");
        assertEquals(2, lexer.getSourceLines().size());
    }

    @Test
    void test_Lexer_tokenize_float() {
        Lexer lexer = new Lexer();
        List<Lexer.Token> tokens = lexer.tokenize("3.14");
        Lexer.Token num = tokens.stream()
                .filter(t -> t.getName().equals("NUMBER"))
                .findFirst().orElseThrow();
        assertEquals(3.14, ((Number) num.getValue()).doubleValue(), 0.0001);
        assertTrue(num.getValue() instanceof Double);
    }

    // =======================================================================
    // TokenStream.__init__ tests
    // =======================================================================

    @Test
    void test_TokenStream___init___pos() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 1, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        assertEquals(0, stream.getPos());
        assertSame(tokens, stream.getTokens());
    }

    // =======================================================================
    // TokenStream.consume_expected tests
    // =======================================================================

    @Test
    void test_TokenStream_consume_expected_success() {
        List<Lexer.Token> tokens = Arrays.asList(
                new Lexer.Token("NUMBER", 42, 1, 1),
                new Lexer.Token("OPERATOR", "+", 1, 3));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        Lexer.Token result = stream.consumeExpected("NUMBER");
        assertEquals(42, ((Number) result.getValue()).intValue());
    }

    @Test
    void test_TokenStream_consume_expected_multiple() {
        List<Lexer.Token> tokens = Arrays.asList(
                new Lexer.Token("LPAREN", "(", 1, 1),
                new Lexer.Token("NUMBER", 1, 1, 2),
                new Lexer.Token("RPAREN", ")", 1, 3));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        Lexer.Token result = stream.consumeExpected("LPAREN", "NUMBER");
        assertEquals("NUMBER", result.getName());
    }

    @Test
    void test_TokenStream_consume_expected_failure() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NAME", "x", 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        assertThrows(Lexer.AbrvalgSyntaxError.class, () -> {
            stream.consumeExpected("NUMBER");
        });
    }

    // =======================================================================
    // TokenStream.consume tests
    // =======================================================================

    @Test
    void test_TokenStream_consume_advances() {
        List<Lexer.Token> tokens = Arrays.asList(
                new Lexer.Token("NUMBER", 1, 1, 1),
                new Lexer.Token("NUMBER", 2, 1, 3));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        Lexer.Token first = stream.consume();
        assertEquals(1, ((Number) first.getValue()).intValue());
        Lexer.Token second = stream.consume();
        assertEquals(2, ((Number) second.getValue()).intValue());
    }

    @Test
    void test_TokenStream_consume_returns_current() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NAME", "x", 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        Lexer.Token result = stream.consume();
        assertEquals("NAME", result.getName());
        assertEquals("x", result.getValue());
    }

    // =======================================================================
    // TokenStream.current tests
    // =======================================================================

    @Test
    void test_TokenStream_current_returns_token() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        assertEquals(42, ((Number) stream.current().getValue()).intValue());
    }

    @Test
    void test_TokenStream_current_does_not_advance() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        stream.current();
        stream.current();
        assertEquals(0, stream.getPos());
    }

    @Test
    void test_TokenStream_current_at_end() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        stream.consume();
        assertThrows(Lexer.AbrvalgSyntaxError.class, () -> {
            stream.current();
        });
    }

    // =======================================================================
    // TokenStream.expect_end tests
    // =======================================================================

    @Test
    void test_TokenStream_expect_end_at_end() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        stream.consume();
        assertDoesNotThrow(() -> stream.expectEnd());
    }

    @Test
    void test_TokenStream_expect_end_not_at_end() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        assertThrows(Lexer.AbrvalgSyntaxError.class, () -> {
            stream.expectEnd();
        });
    }

    // =======================================================================
    // TokenStream.is_end tests
    // =======================================================================

    @Test
    void test_TokenStream_is_end_false() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        assertFalse(stream.isEnd());
    }

    @Test
    void test_TokenStream_is_end_true() {
        List<Lexer.Token> tokens = Arrays.asList(new Lexer.Token("NUMBER", 42, 1, 1));
        Lexer.TokenStream stream = new Lexer.TokenStream(tokens);
        stream.consume();
        assertTrue(stream.isEnd());
    }

    @Test
    void test_TokenStream_is_end_empty() {
        Lexer.TokenStream stream = new Lexer.TokenStream(Arrays.asList());
        assertTrue(stream.isEnd());
    }
}
