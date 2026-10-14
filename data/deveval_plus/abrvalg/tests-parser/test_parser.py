"""Tests for parser.py"""

import pytest
from abrvalg import ast
from abrvalg.lexer import Lexer, TokenStream
from abrvalg.errors import AbrvalgSyntaxError
from parser import (
    Parser, ParserError, enter_scope,
    Subparser, PrefixSubparser, InfixSubparser,
    NumberExpression, StringExpression, NameExpression,
    UnaryOperatorExpression, GroupExpression,
    ArrayExpression, DictionaryExpression,
    BinaryOperatorExpression, CallExpression, SubscriptOperatorExpression,
    Expression, ListOfExpressions, Block,
    FunctionStatement, ConditionalStatement, MatchStatement,
    WhileLoopStatement, ForLoopStatement,
    ReturnStatement, BreakStatement, ContinueStatement,
    AssignmentStatement, ExpressionStatement,
    Statements, Program,
)


def parse(source):
    """Helper: tokenize + parse a source string."""
    lexer = Lexer()
    tokens = lexer.tokenize(source)
    stream = TokenStream(tokens)
    return Parser().parse(stream)


def parse_expr(source):
    """Parse a single expression (source is one line)."""
    prog = parse(source)
    assert len(prog.body) == 1
    return prog.body[0]


# === ParserError.__init__ tests ===

def test_ParserError___init___attributes():
    from abrvalg.lexer import Token
    tok = Token('NAME', 'x', 3, 7)
    err = ParserError('bad token', tok)
    assert err.line == 3
    assert err.column == 7
    assert 'bad token' in str(err)

def test_ParserError___init___is_syntax_error():
    from abrvalg.lexer import Token
    tok = Token('NAME', 'x', 1, 1)
    err = ParserError('msg', tok)
    assert isinstance(err, AbrvalgSyntaxError)


# === enter_scope tests ===

def test_enter_scope_appends_and_pops():
    p = Parser()
    p.scope = []
    with enter_scope(p, 'function'):
        assert p.scope == ['function']
    assert p.scope == []

def test_enter_scope_nested():
    p = Parser()
    p.scope = []
    with enter_scope(p, 'function'):
        with enter_scope(p, 'loop'):
            assert p.scope == ['function', 'loop']
        assert p.scope == ['function']
    assert p.scope == []


# === Subparser.get_subparser tests ===

def test_Subparser_get_subparser_found():
    from abrvalg.lexer import Token
    sp = Subparser()
    tok = Token('NUMBER', 42, 1, 1)
    result = sp.get_subparser(tok, {'NUMBER': NumberExpression})
    assert isinstance(result, NumberExpression)

def test_Subparser_get_subparser_not_found():
    from abrvalg.lexer import Token
    sp = Subparser()
    tok = Token('NAME', 'x', 1, 1)
    result = sp.get_subparser(tok, {'NUMBER': NumberExpression})
    assert result is None

def test_Subparser_get_subparser_default():
    from abrvalg.lexer import Token
    sp = Subparser()
    tok = Token('NAME', 'x', 1, 1)
    result = sp.get_subparser(tok, {}, default=NameExpression)
    assert isinstance(result, NameExpression)


# === NumberExpression.parse tests ===

def test_NumberExpression_parse_integer():
    node = parse_expr('42')
    assert isinstance(node, ast.Number)
    assert node.value == 42

def test_NumberExpression_parse_float():
    node = parse_expr('3.14')
    assert isinstance(node, ast.Number)
    assert node.value == 3.14


# === StringExpression.parse tests ===

def test_StringExpression_parse_double_quotes():
    node = parse_expr('"hello"')
    assert isinstance(node, ast.String)
    assert node.value == 'hello'

def test_StringExpression_parse_single_quotes():
    node = parse_expr("'world'")
    assert isinstance(node, ast.String)
    assert node.value == 'world'


# === NameExpression.parse tests ===

def test_NameExpression_parse_identifier():
    node = parse_expr('foo')
    assert isinstance(node, ast.Identifier)
    assert node.value == 'foo'


# === UnaryOperatorExpression.parse tests ===

def test_UnaryOperatorExpression_parse_neg():
    node = parse_expr('-5')
    assert isinstance(node, ast.UnaryOperator)
    assert node.operator == '-'
    assert isinstance(node.right, ast.Number)

def test_UnaryOperatorExpression_parse_not():
    node = parse_expr('!x')
    assert isinstance(node, ast.UnaryOperator)
    assert node.operator == '!'


# === GroupExpression.parse tests ===

def test_GroupExpression_parse_parens():
    node = parse_expr('(1 + 2)')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '+'

def test_GroupExpression_parse_nested():
    node = parse_expr('((42))')
    assert isinstance(node, ast.Number)
    assert node.value == 42


# === ArrayExpression.parse tests ===

def test_ArrayExpression_parse_literal():
    node = parse_expr('[1, 2, 3]')
    assert isinstance(node, ast.Array)
    assert len(node.items) == 3

def test_ArrayExpression_parse_empty():
    node = parse_expr('[]')
    assert isinstance(node, ast.Array)
    assert len(node.items) == 0


# === DictionaryExpression.parse tests ===

def test_DictionaryExpression_parse_literal():
    node = parse_expr('{"a": 1, "b": 2}')
    assert isinstance(node, ast.Dictionary)
    assert len(node.items) == 2

def test_DictionaryExpression_parse_empty():
    node = parse_expr('{}')
    assert isinstance(node, ast.Dictionary)
    assert len(node.items) == 0


# === BinaryOperatorExpression.parse tests ===

def test_BinaryOperatorExpression_parse_add():
    node = parse_expr('1 + 2')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '+'

def test_BinaryOperatorExpression_parse_precedence():
    node = parse_expr('1 + 2 * 3')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '+'
    assert node.right.operator == '*'

def test_BinaryOperatorExpression_parse_comparison():
    node = parse_expr('a > b')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '>'

def test_BinaryOperatorExpression_parse_logical():
    node = parse_expr('a && b || c')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '||'
    assert node.left.operator == '&&'

def test_BinaryOperatorExpression_parse_range():
    node = parse_expr('1..10')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '..'


# === CallExpression.parse tests ===

def test_CallExpression_parse_no_args():
    node = parse_expr('f()')
    assert isinstance(node, ast.Call)
    assert node.left.value == 'f'
    assert len(node.arguments) == 0

def test_CallExpression_parse_with_args():
    node = parse_expr('add(1, 2)')
    assert isinstance(node, ast.Call)
    assert len(node.arguments) == 2

def test_CallExpression_parse_nested():
    node = parse_expr('f(g(x))')
    assert isinstance(node, ast.Call)
    assert isinstance(node.arguments[0], ast.Call)


# === SubscriptOperatorExpression.parse tests ===

def test_SubscriptOperatorExpression_parse_index():
    node = parse_expr('arr[0]')
    assert isinstance(node, ast.SubscriptOperator)
    assert node.left.value == 'arr'
    assert node.key.value == 0

def test_SubscriptOperatorExpression_parse_chained():
    node = parse_expr('a[0][1]')
    assert isinstance(node, ast.SubscriptOperator)
    assert isinstance(node.left, ast.SubscriptOperator)


# === Expression.parse tests ===

def test_Expression_parse_complex():
    node = parse_expr('a + b * c - d')
    assert isinstance(node, ast.BinaryOperator)
    assert node.operator == '-'

def test_Expression_parse_unary_in_binary():
    node = parse_expr('-a + b')
    assert isinstance(node, ast.BinaryOperator)
    assert isinstance(node.left, ast.UnaryOperator)


# === FunctionStatement.parse tests ===

def test_FunctionStatement_parse_simple():
    prog = parse('func add(a, b):\n    a + b')
    func = prog.body[0]
    assert isinstance(func, ast.Function)
    assert func.name == 'add'
    assert func.params == ['a', 'b']

def test_FunctionStatement_parse_no_params():
    prog = parse('func hello():\n    42')
    func = prog.body[0]
    assert isinstance(func, ast.Function)
    assert func.params == []

def test_FunctionStatement_parse_body():
    prog = parse('func f(x):\n    x + 1')
    func = prog.body[0]
    assert len(func.body) == 1


# === ConditionalStatement.parse tests ===

def test_ConditionalStatement_parse_if():
    prog = parse('if x:\n    1')
    cond = prog.body[0]
    assert isinstance(cond, ast.Condition)
    assert cond.else_body is None
    assert len(cond.elifs) == 0

def test_ConditionalStatement_parse_if_else():
    prog = parse('if x:\n    1\nelse:\n    2')
    cond = prog.body[0]
    assert isinstance(cond, ast.Condition)
    assert cond.else_body is not None

def test_ConditionalStatement_parse_elif():
    prog = parse('if x:\n    1\nelif y:\n    2\nelse:\n    3')
    cond = prog.body[0]
    assert len(cond.elifs) == 1
    assert isinstance(cond.elifs[0], ast.ConditionElif)


# === MatchStatement.parse tests ===

def test_MatchStatement_parse_basic():
    prog = parse('match x:\n    when 1:\n        10\n    when 2:\n        20')
    match = prog.body[0]
    assert isinstance(match, ast.Match)
    assert len(match.patterns) == 2
    assert match.else_body is None

def test_MatchStatement_parse_with_else():
    prog = parse('match x:\n    when 1:\n        10\n    else:\n        0')
    match = prog.body[0]
    assert isinstance(match, ast.Match)
    assert match.else_body is not None


# === WhileLoopStatement.parse tests ===

def test_WhileLoopStatement_parse_basic():
    prog = parse('while x:\n    1')
    loop = prog.body[0]
    assert isinstance(loop, ast.WhileLoop)


# === ForLoopStatement.parse tests ===

def test_ForLoopStatement_parse_basic():
    prog = parse('for i in items:\n    i')
    loop = prog.body[0]
    assert isinstance(loop, ast.ForLoop)
    assert loop.var_name == 'i'

def test_ForLoopStatement_parse_range():
    prog = parse('for i in 0..10:\n    i')
    loop = prog.body[0]
    assert isinstance(loop, ast.ForLoop)
    assert isinstance(loop.collection, ast.BinaryOperator)


# === ReturnStatement.parse tests ===

def test_ReturnStatement_parse_value():
    prog = parse('func f():\n    return 42')
    func = prog.body[0]
    ret = func.body[0]
    assert isinstance(ret, ast.Return)
    assert isinstance(ret.value, ast.Number)

def test_ReturnStatement_parse_expression():
    prog = parse('func f(x):\n    return x + 1')
    func = prog.body[0]
    ret = func.body[0]
    assert isinstance(ret, ast.Return)
    assert isinstance(ret.value, ast.BinaryOperator)


# === BreakStatement.parse tests ===

def test_BreakStatement_parse_in_loop():
    prog = parse('while x:\n    break')
    loop = prog.body[0]
    assert isinstance(loop.body[0], ast.Break)

def test_BreakStatement_parse_outside_loop():
    with pytest.raises(AbrvalgSyntaxError):
        parse('break')


# === ContinueStatement.parse tests ===

def test_ContinueStatement_parse_in_loop():
    prog = parse('while x:\n    continue')
    loop = prog.body[0]
    assert isinstance(loop.body[0], ast.Continue)

def test_ContinueStatement_parse_outside_loop():
    with pytest.raises(AbrvalgSyntaxError):
        parse('continue')


# === AssignmentStatement.parse tests ===

def test_AssignmentStatement_parse_simple():
    node = parse_expr('x = 42')
    assert isinstance(node, ast.Assignment)
    assert node.left.value == 'x'
    assert node.right.value == 42

def test_AssignmentStatement_parse_expression():
    node = parse_expr('x = 1 + 2')
    assert isinstance(node, ast.Assignment)
    assert isinstance(node.right, ast.BinaryOperator)


# === ExpressionStatement.parse tests ===

def test_ExpressionStatement_parse_expr():
    node = parse_expr('42')
    assert isinstance(node, ast.Number)

def test_ExpressionStatement_parse_assignment():
    node = parse_expr('x = 1')
    assert isinstance(node, ast.Assignment)


# === Statements.parse tests ===

def test_Statements_parse_multiple():
    prog = parse('x = 1\ny = 2\nx + y')
    assert len(prog.body) == 3

def test_Statements_parse_mixed():
    prog = parse('x = 1\nif x:\n    2')
    assert len(prog.body) == 2
    assert isinstance(prog.body[1], ast.Condition)


# === Program.parse tests ===

def test_Program_parse_empty():
    lexer = Lexer()
    tokens = lexer.tokenize('')
    stream = TokenStream(tokens)
    prog = Parser().parse(stream)
    assert isinstance(prog, ast.Program)
    assert prog.body == []

def test_Program_parse_wraps_in_program():
    prog = parse('1')
    assert isinstance(prog, ast.Program)
    assert len(prog.body) == 1


# === Parser.__init__ tests ===

def test_Parser___init___scope():
    p = Parser()
    assert p.scope is None


# === Parser.parse tests ===

def test_Parser_parse_sets_scope():
    lexer = Lexer()
    tokens = lexer.tokenize('1')
    stream = TokenStream(tokens)
    p = Parser()
    p.parse(stream)
    assert p.scope == []

def test_Parser_parse_full_program():
    prog = parse('func f(x):\n    if x > 0:\n        return x\n    return 0\nf(5)')
    assert isinstance(prog, ast.Program)
    assert len(prog.body) == 2
    assert isinstance(prog.body[0], ast.Function)
    assert isinstance(prog.body[1], ast.Call)
