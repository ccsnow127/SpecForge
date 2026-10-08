import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests for AstParser -- mirrors every test in test_parser.py.
 */
public class AstParserTest {

    // ---- Helpers ----

    private AstParser.Program parse(String source) {
        AstParser.Lexer lexer = new AstParser.Lexer();
        List<AstParser.Token> tokens = lexer.tokenize(source);
        AstParser.TokenStream stream = new AstParser.TokenStream(tokens);
        return new AstParser.Parser().parse(stream);
    }

    private AstParser.AstNode parseExpr(String source) {
        AstParser.Program prog = parse(source);
        assertEquals(1, prog.getBody().size());
        return prog.getBody().get(0);
    }

    // === ParserError.__init__ tests ===

    @Test
    void test_ParserError___init___attributes() {
        AstParser.Token tok = new AstParser.Token("NAME", "x", 3, 7);
        AstParser.ParserError err = new AstParser.ParserError("bad token", tok);
        assertEquals(3, err.getLine());
        assertEquals(7, err.getColumn());
        assertTrue(err.getMessage().contains("bad token"));
    }

    @Test
    void test_ParserError___init___is_syntax_error() {
        AstParser.Token tok = new AstParser.Token("NAME", "x", 1, 1);
        AstParser.ParserError err = new AstParser.ParserError("msg", tok);
        assertInstanceOf(AstParser.AbrvalgSyntaxError.class, err);
    }

    // === enter_scope tests ===

    @Test
    void test_enter_scope_appends_and_pops() {
        AstParser.Parser p = new AstParser.Parser();
        p.setScope(new java.util.ArrayList<>());
        try (AstParser.ScopeGuard sg = AstParser.enterScope(p, "function")) {
            assertEquals(List.of("function"), p.getScope());
        }
        assertEquals(List.of(), p.getScope());
    }

    @Test
    void test_enter_scope_nested() {
        AstParser.Parser p = new AstParser.Parser();
        p.setScope(new java.util.ArrayList<>());
        try (AstParser.ScopeGuard sg1 = AstParser.enterScope(p, "function")) {
            try (AstParser.ScopeGuard sg2 = AstParser.enterScope(p, "loop")) {
                assertEquals(List.of("function", "loop"), p.getScope());
            }
            assertEquals(List.of("function"), p.getScope());
        }
        assertEquals(List.of(), p.getScope());
    }

    // === Subparser.get_subparser tests ===

    @Test
    void test_Subparser_get_subparser_found() {
        AstParser.Token tok = new AstParser.Token("NUMBER", 42, 1, 1);
        java.util.Map<String, java.util.function.Supplier<AstParser.PrefixSubparser>> map = new java.util.HashMap<>();
        map.put("NUMBER", AstParser.NumberExpression::new);
        AstParser.PrefixSubparser result = AstParser.SubparserLookup.getSubparser(tok, map);
        assertInstanceOf(AstParser.NumberExpression.class, result);
    }

    @Test
    void test_Subparser_get_subparser_not_found() {
        AstParser.Token tok = new AstParser.Token("NAME", "x", 1, 1);
        java.util.Map<String, java.util.function.Supplier<AstParser.PrefixSubparser>> map = new java.util.HashMap<>();
        map.put("NUMBER", AstParser.NumberExpression::new);
        AstParser.PrefixSubparser result = AstParser.SubparserLookup.getSubparser(tok, map);
        assertNull(result);
    }

    @Test
    void test_Subparser_get_subparser_default() {
        AstParser.Token tok = new AstParser.Token("NAME", "x", 1, 1);
        java.util.Map<String, java.util.function.Supplier<AstParser.PrefixSubparser>> map = new java.util.HashMap<>();
        AstParser.PrefixSubparser result = AstParser.SubparserLookup.getSubparser(tok, map, AstParser.NameExpression::new);
        assertInstanceOf(AstParser.NameExpression.class, result);
    }

    // === NumberExpression.parse tests ===

    @Test
    void test_NumberExpression_parse_integer() {
        AstParser.AstNode node = parseExpr("42");
        assertInstanceOf(AstParser.Number.class, node);
        assertEquals(42, ((AstParser.Number) node).getValue());
    }

    @Test
    void test_NumberExpression_parse_float() {
        AstParser.AstNode node = parseExpr("3.14");
        assertInstanceOf(AstParser.Number.class, node);
        assertEquals(3.14, ((AstParser.Number) node).getValue());
    }

    // === StringExpression.parse tests ===

    @Test
    void test_StringExpression_parse_double_quotes() {
        AstParser.AstNode node = parseExpr("\"hello\"");
        assertInstanceOf(AstParser.StringNode.class, node);
        assertEquals("hello", ((AstParser.StringNode) node).getValue());
    }

    @Test
    void test_StringExpression_parse_single_quotes() {
        AstParser.AstNode node = parseExpr("'world'");
        assertInstanceOf(AstParser.StringNode.class, node);
        assertEquals("world", ((AstParser.StringNode) node).getValue());
    }

    // === NameExpression.parse tests ===

    @Test
    void test_NameExpression_parse_identifier() {
        AstParser.AstNode node = parseExpr("foo");
        assertInstanceOf(AstParser.Identifier.class, node);
        assertEquals("foo", ((AstParser.Identifier) node).getValue());
    }

    // === UnaryOperatorExpression.parse tests ===

    @Test
    void test_UnaryOperatorExpression_parse_neg() {
        AstParser.AstNode node = parseExpr("-5");
        assertInstanceOf(AstParser.UnaryOperator.class, node);
        AstParser.UnaryOperator un = (AstParser.UnaryOperator) node;
        assertEquals("-", un.getOperator());
        assertInstanceOf(AstParser.Number.class, un.getRight());
    }

    @Test
    void test_UnaryOperatorExpression_parse_not() {
        AstParser.AstNode node = parseExpr("!x");
        assertInstanceOf(AstParser.UnaryOperator.class, node);
        assertEquals("!", ((AstParser.UnaryOperator) node).getOperator());
    }

    // === GroupExpression.parse tests ===

    @Test
    void test_GroupExpression_parse_parens() {
        AstParser.AstNode node = parseExpr("(1 + 2)");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        assertEquals("+", ((AstParser.BinaryOperator) node).getOperator());
    }

    @Test
    void test_GroupExpression_parse_nested() {
        AstParser.AstNode node = parseExpr("((42))");
        assertInstanceOf(AstParser.Number.class, node);
        assertEquals(42, ((AstParser.Number) node).getValue());
    }

    // === ArrayExpression.parse tests ===

    @Test
    void test_ArrayExpression_parse_literal() {
        AstParser.AstNode node = parseExpr("[1, 2, 3]");
        assertInstanceOf(AstParser.Array.class, node);
        assertEquals(3, ((AstParser.Array) node).getItems().size());
    }

    @Test
    void test_ArrayExpression_parse_empty() {
        AstParser.AstNode node = parseExpr("[]");
        assertInstanceOf(AstParser.Array.class, node);
        assertEquals(0, ((AstParser.Array) node).getItems().size());
    }

    // === DictionaryExpression.parse tests ===

    @Test
    void test_DictionaryExpression_parse_literal() {
        AstParser.AstNode node = parseExpr("{\"a\": 1, \"b\": 2}");
        assertInstanceOf(AstParser.Dictionary.class, node);
        assertEquals(2, ((AstParser.Dictionary) node).getItems().size());
    }

    @Test
    void test_DictionaryExpression_parse_empty() {
        AstParser.AstNode node = parseExpr("{}");
        assertInstanceOf(AstParser.Dictionary.class, node);
        assertEquals(0, ((AstParser.Dictionary) node).getItems().size());
    }

    // === BinaryOperatorExpression.parse tests ===

    @Test
    void test_BinaryOperatorExpression_parse_add() {
        AstParser.AstNode node = parseExpr("1 + 2");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        assertEquals("+", ((AstParser.BinaryOperator) node).getOperator());
    }

    @Test
    void test_BinaryOperatorExpression_parse_precedence() {
        AstParser.AstNode node = parseExpr("1 + 2 * 3");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        AstParser.BinaryOperator binop = (AstParser.BinaryOperator) node;
        assertEquals("+", binop.getOperator());
        assertInstanceOf(AstParser.BinaryOperator.class, binop.getRight());
        assertEquals("*", ((AstParser.BinaryOperator) binop.getRight()).getOperator());
    }

    @Test
    void test_BinaryOperatorExpression_parse_comparison() {
        AstParser.AstNode node = parseExpr("a > b");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        assertEquals(">", ((AstParser.BinaryOperator) node).getOperator());
    }

    @Test
    void test_BinaryOperatorExpression_parse_logical() {
        AstParser.AstNode node = parseExpr("a && b || c");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        AstParser.BinaryOperator outer = (AstParser.BinaryOperator) node;
        assertEquals("||", outer.getOperator());
        assertInstanceOf(AstParser.BinaryOperator.class, outer.getLeft());
        assertEquals("&&", ((AstParser.BinaryOperator) outer.getLeft()).getOperator());
    }

    @Test
    void test_BinaryOperatorExpression_parse_range() {
        AstParser.AstNode node = parseExpr("1..10");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        assertEquals("..", ((AstParser.BinaryOperator) node).getOperator());
    }

    // === CallExpression.parse tests ===

    @Test
    void test_CallExpression_parse_no_args() {
        AstParser.AstNode node = parseExpr("f()");
        assertInstanceOf(AstParser.Call.class, node);
        AstParser.Call call = (AstParser.Call) node;
        assertEquals("f", ((AstParser.Identifier) call.getLeft()).getValue());
        assertEquals(0, call.getArguments().size());
    }

    @Test
    void test_CallExpression_parse_with_args() {
        AstParser.AstNode node = parseExpr("add(1, 2)");
        assertInstanceOf(AstParser.Call.class, node);
        assertEquals(2, ((AstParser.Call) node).getArguments().size());
    }

    @Test
    void test_CallExpression_parse_nested() {
        AstParser.AstNode node = parseExpr("f(g(x))");
        assertInstanceOf(AstParser.Call.class, node);
        assertInstanceOf(AstParser.Call.class, ((AstParser.Call) node).getArguments().get(0));
    }

    // === SubscriptOperatorExpression.parse tests ===

    @Test
    void test_SubscriptOperatorExpression_parse_index() {
        AstParser.AstNode node = parseExpr("arr[0]");
        assertInstanceOf(AstParser.SubscriptOperator.class, node);
        AstParser.SubscriptOperator sub = (AstParser.SubscriptOperator) node;
        assertEquals("arr", ((AstParser.Identifier) sub.getLeft()).getValue());
        assertEquals(0, ((AstParser.Number) sub.getKey()).getValue());
    }

    @Test
    void test_SubscriptOperatorExpression_parse_chained() {
        AstParser.AstNode node = parseExpr("a[0][1]");
        assertInstanceOf(AstParser.SubscriptOperator.class, node);
        assertInstanceOf(AstParser.SubscriptOperator.class, ((AstParser.SubscriptOperator) node).getLeft());
    }

    // === Expression.parse tests ===

    @Test
    void test_Expression_parse_complex() {
        AstParser.AstNode node = parseExpr("a + b * c - d");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        assertEquals("-", ((AstParser.BinaryOperator) node).getOperator());
    }

    @Test
    void test_Expression_parse_unary_in_binary() {
        AstParser.AstNode node = parseExpr("-a + b");
        assertInstanceOf(AstParser.BinaryOperator.class, node);
        assertInstanceOf(AstParser.UnaryOperator.class, ((AstParser.BinaryOperator) node).getLeft());
    }

    // === FunctionStatement.parse tests ===

    @Test
    void test_FunctionStatement_parse_simple() {
        AstParser.Program prog = parse("func add(a, b):\n    a + b");
        AstParser.Function func = (AstParser.Function) prog.getBody().get(0);
        assertInstanceOf(AstParser.Function.class, func);
        assertEquals("add", func.getName());
        assertEquals(List.of("a", "b"), func.getParams());
    }

    @Test
    void test_FunctionStatement_parse_no_params() {
        AstParser.Program prog = parse("func hello():\n    42");
        AstParser.Function func = (AstParser.Function) prog.getBody().get(0);
        assertInstanceOf(AstParser.Function.class, func);
        assertEquals(List.of(), func.getParams());
    }

    @Test
    void test_FunctionStatement_parse_body() {
        AstParser.Program prog = parse("func f(x):\n    x + 1");
        AstParser.Function func = (AstParser.Function) prog.getBody().get(0);
        assertEquals(1, func.getBody().size());
    }

    // === ConditionalStatement.parse tests ===

    @Test
    void test_ConditionalStatement_parse_if() {
        AstParser.Program prog = parse("if x:\n    1");
        AstParser.Condition cond = (AstParser.Condition) prog.getBody().get(0);
        assertInstanceOf(AstParser.Condition.class, cond);
        assertNull(cond.getElseBody());
        assertEquals(0, cond.getElifs().size());
    }

    @Test
    void test_ConditionalStatement_parse_if_else() {
        AstParser.Program prog = parse("if x:\n    1\nelse:\n    2");
        AstParser.Condition cond = (AstParser.Condition) prog.getBody().get(0);
        assertInstanceOf(AstParser.Condition.class, cond);
        assertNotNull(cond.getElseBody());
    }

    @Test
    void test_ConditionalStatement_parse_elif() {
        AstParser.Program prog = parse("if x:\n    1\nelif y:\n    2\nelse:\n    3");
        AstParser.Condition cond = (AstParser.Condition) prog.getBody().get(0);
        assertEquals(1, cond.getElifs().size());
        assertInstanceOf(AstParser.ConditionElif.class, cond.getElifs().get(0));
    }

    // === MatchStatement.parse tests ===

    @Test
    void test_MatchStatement_parse_basic() {
        AstParser.Program prog = parse("match x:\n    when 1:\n        10\n    when 2:\n        20");
        AstParser.Match match = (AstParser.Match) prog.getBody().get(0);
        assertInstanceOf(AstParser.Match.class, match);
        assertEquals(2, match.getPatterns().size());
        assertNull(match.getElseBody());
    }

    @Test
    void test_MatchStatement_parse_with_else() {
        AstParser.Program prog = parse("match x:\n    when 1:\n        10\n    else:\n        0");
        AstParser.Match match = (AstParser.Match) prog.getBody().get(0);
        assertInstanceOf(AstParser.Match.class, match);
        assertNotNull(match.getElseBody());
    }

    // === WhileLoopStatement.parse tests ===

    @Test
    void test_WhileLoopStatement_parse_basic() {
        AstParser.Program prog = parse("while x:\n    1");
        AstParser.WhileLoop loop = (AstParser.WhileLoop) prog.getBody().get(0);
        assertInstanceOf(AstParser.WhileLoop.class, loop);
    }

    // === ForLoopStatement.parse tests ===

    @Test
    void test_ForLoopStatement_parse_basic() {
        AstParser.Program prog = parse("for i in items:\n    i");
        AstParser.ForLoop loop = (AstParser.ForLoop) prog.getBody().get(0);
        assertInstanceOf(AstParser.ForLoop.class, loop);
        assertEquals("i", loop.getVarName());
    }

    @Test
    void test_ForLoopStatement_parse_range() {
        AstParser.Program prog = parse("for i in 0..10:\n    i");
        AstParser.ForLoop loop = (AstParser.ForLoop) prog.getBody().get(0);
        assertInstanceOf(AstParser.ForLoop.class, loop);
        assertInstanceOf(AstParser.BinaryOperator.class, loop.getCollection());
    }

    // === ReturnStatement.parse tests ===

    @Test
    void test_ReturnStatement_parse_value() {
        AstParser.Program prog = parse("func f():\n    return 42");
        AstParser.Function func = (AstParser.Function) prog.getBody().get(0);
        AstParser.Return ret = (AstParser.Return) func.getBody().get(0);
        assertInstanceOf(AstParser.Return.class, ret);
        assertInstanceOf(AstParser.Number.class, ret.getValue());
    }

    @Test
    void test_ReturnStatement_parse_expression() {
        AstParser.Program prog = parse("func f(x):\n    return x + 1");
        AstParser.Function func = (AstParser.Function) prog.getBody().get(0);
        AstParser.Return ret = (AstParser.Return) func.getBody().get(0);
        assertInstanceOf(AstParser.Return.class, ret);
        assertInstanceOf(AstParser.BinaryOperator.class, ret.getValue());
    }

    // === BreakStatement.parse tests ===

    @Test
    void test_BreakStatement_parse_in_loop() {
        AstParser.Program prog = parse("while x:\n    break");
        AstParser.WhileLoop loop = (AstParser.WhileLoop) prog.getBody().get(0);
        assertInstanceOf(AstParser.Break.class, loop.getBody().get(0));
    }

    @Test
    void test_BreakStatement_parse_outside_loop() {
        assertThrows(AstParser.AbrvalgSyntaxError.class, () -> parse("break"));
    }

    // === ContinueStatement.parse tests ===

    @Test
    void test_ContinueStatement_parse_in_loop() {
        AstParser.Program prog = parse("while x:\n    continue");
        AstParser.WhileLoop loop = (AstParser.WhileLoop) prog.getBody().get(0);
        assertInstanceOf(AstParser.Continue.class, loop.getBody().get(0));
    }

    @Test
    void test_ContinueStatement_parse_outside_loop() {
        assertThrows(AstParser.AbrvalgSyntaxError.class, () -> parse("continue"));
    }

    // === AssignmentStatement.parse tests ===

    @Test
    void test_AssignmentStatement_parse_simple() {
        AstParser.AstNode node = parseExpr("x = 42");
        assertInstanceOf(AstParser.Assignment.class, node);
        AstParser.Assignment assign = (AstParser.Assignment) node;
        assertEquals("x", ((AstParser.Identifier) assign.getLeft()).getValue());
        assertEquals(42, ((AstParser.Number) assign.getRight()).getValue());
    }

    @Test
    void test_AssignmentStatement_parse_expression() {
        AstParser.AstNode node = parseExpr("x = 1 + 2");
        assertInstanceOf(AstParser.Assignment.class, node);
        assertInstanceOf(AstParser.BinaryOperator.class, ((AstParser.Assignment) node).getRight());
    }

    // === ExpressionStatement.parse tests ===

    @Test
    void test_ExpressionStatement_parse_expr() {
        AstParser.AstNode node = parseExpr("42");
        assertInstanceOf(AstParser.Number.class, node);
    }

    @Test
    void test_ExpressionStatement_parse_assignment() {
        AstParser.AstNode node = parseExpr("x = 1");
        assertInstanceOf(AstParser.Assignment.class, node);
    }

    // === Statements.parse tests ===

    @Test
    void test_Statements_parse_multiple() {
        AstParser.Program prog = parse("x = 1\ny = 2\nx + y");
        assertEquals(3, prog.getBody().size());
    }

    @Test
    void test_Statements_parse_mixed() {
        AstParser.Program prog = parse("x = 1\nif x:\n    2");
        assertEquals(2, prog.getBody().size());
        assertInstanceOf(AstParser.Condition.class, prog.getBody().get(1));
    }

    // === Program.parse tests ===

    @Test
    void test_Program_parse_empty() {
        AstParser.Lexer lexer = new AstParser.Lexer();
        List<AstParser.Token> tokens = lexer.tokenize("");
        AstParser.TokenStream stream = new AstParser.TokenStream(tokens);
        AstParser.Program prog = new AstParser.Parser().parse(stream);
        assertInstanceOf(AstParser.Program.class, prog);
        assertEquals(List.of(), prog.getBody());
    }

    @Test
    void test_Program_parse_wraps_in_program() {
        AstParser.Program prog = parse("1");
        assertInstanceOf(AstParser.Program.class, prog);
        assertEquals(1, prog.getBody().size());
    }

    // === Parser.__init__ tests ===

    @Test
    void test_Parser___init___scope() {
        AstParser.Parser p = new AstParser.Parser();
        assertNull(p.getScope());
    }

    // === Parser.parse tests ===

    @Test
    void test_Parser_parse_sets_scope() {
        AstParser.Lexer lexer = new AstParser.Lexer();
        List<AstParser.Token> tokens = lexer.tokenize("1");
        AstParser.TokenStream stream = new AstParser.TokenStream(tokens);
        AstParser.Parser p = new AstParser.Parser();
        p.parse(stream);
        assertEquals(List.of(), p.getScope());
    }

    @Test
    void test_Parser_parse_full_program() {
        AstParser.Program prog = parse("func f(x):\n    if x > 0:\n        return x\n    return 0\nf(5)");
        assertInstanceOf(AstParser.Program.class, prog);
        assertEquals(2, prog.getBody().size());
        assertInstanceOf(AstParser.Function.class, prog.getBody().get(0));
        assertInstanceOf(AstParser.Call.class, prog.getBody().get(1));
    }
}
