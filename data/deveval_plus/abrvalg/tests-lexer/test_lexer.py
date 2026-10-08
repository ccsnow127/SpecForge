"""Tests for lexer.py"""

import pytest
from lexer import Lexer, TokenStream, Token, decode_str, decode_num
from abrvalg.errors import AbrvalgSyntaxError


# === decode_str tests ===

def test_decode_str_simple():
    assert decode_str('"hello"') == "hello"

def test_decode_str_single_quotes():
    assert decode_str("'hello'") == "hello"

def test_decode_str_escape_newline():
    assert decode_str(r'"hello\nworld"') == "hello\nworld"

def test_decode_str_escape_tab():
    assert decode_str(r'"col1\tcol2"') == "col1\tcol2"

def test_decode_str_escape_return():
    assert decode_str(r'"line\rend"') == "line\rend"

def test_decode_str_escape_backslash():
    assert decode_str(r'"path\\file"') == "path\\file"

def test_decode_str_escape_double_quote():
    assert decode_str(r'"say \"hi\""') == 'say "hi"'

def test_decode_str_empty():
    assert decode_str('""') == ""


# === decode_num tests ===

def test_decode_num_integer():
    assert decode_num("42") == 42
    assert isinstance(decode_num("42"), int)

def test_decode_num_float():
    assert decode_num("3.14") == 3.14
    assert isinstance(decode_num("3.14"), float)

def test_decode_num_zero():
    assert decode_num("0") == 0


# === Token.__repr__ tests ===

def test_Token___repr___basic():
    t = Token('NUMBER', 42, 1, 1)
    assert repr(t) == "('NUMBER', 42, 1, 1)"

def test_Token___repr___string():
    t = Token('STRING', 'hello', 2, 5)
    assert repr(t) == "('STRING', 'hello', 2, 5)"

def test_Token___repr___none_value():
    t = Token('IF', None, 1, 1)
    assert repr(t) == "('IF', None, 1, 1)"


# === Lexer.__init__ tests ===

def test_Lexer___init___source_lines():
    lexer = Lexer()
    assert lexer.source_lines == []

def test_Lexer___init___regex():
    lexer = Lexer()
    assert lexer._regex is not None


# === Lexer._convert_rules tests ===

def test_Lexer__convert_rules_single():
    lexer = Lexer()
    rules = [('NUMBER', r'\d+')]
    result = list(lexer._convert_rules(rules))
    assert len(result) == 1
    assert 'NUMBER' in result[0]

def test_Lexer__convert_rules_merged():
    lexer = Lexer()
    rules = [('NUMBER', r'\d+\.\d+'), ('NUMBER', r'\d+')]
    result = list(lexer._convert_rules(rules))
    assert len(result) == 1  # Same name merged into one group


# === Lexer._compile_rules tests ===

def test_Lexer__compile_rules_returns_pattern():
    lexer = Lexer()
    rules = [('NUMBER', r'\d+'), ('NAME', r'[a-zA-Z_]\w*')]
    regex = lexer._compile_rules(rules)
    assert regex.match('42') is not None
    assert regex.match('hello') is not None


# === Lexer._tokenize_line tests ===

def test_Lexer__tokenize_line_number():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('42', 1))
    assert len(tokens) == 1
    assert tokens[0].name == 'NUMBER'
    assert tokens[0].value == 42

def test_Lexer__tokenize_line_string():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('"hello"', 1))
    assert len(tokens) == 1
    assert tokens[0].name == 'STRING'
    assert tokens[0].value == 'hello'

def test_Lexer__tokenize_line_operator():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('2 + 3', 1))
    names = [t.name for t in tokens]
    assert names == ['NUMBER', 'OPERATOR', 'NUMBER']

def test_Lexer__tokenize_line_keyword():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('if x', 1))
    assert tokens[0].name == 'IF'
    assert tokens[0].value is None

def test_Lexer__tokenize_line_skips_whitespace():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('a   b', 1))
    names = [t.name for t in tokens]
    assert 'WHITESPACE' not in names

def test_Lexer__tokenize_line_skips_comment():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('x # comment', 1))
    names = [t.name for t in tokens]
    assert 'COMMENT' not in names

def test_Lexer__tokenize_line_unexpected_char():
    lexer = Lexer()
    with pytest.raises(AbrvalgSyntaxError):
        list(lexer._tokenize_line('$', 1))

def test_Lexer__tokenize_line_column():
    lexer = Lexer()
    tokens = list(lexer._tokenize_line('x + y', 1))
    assert tokens[0].column == 1
    assert tokens[2].column == 5


# === Lexer._count_leading_characters tests ===

def test_Lexer__count_leading_characters_spaces():
    lexer = Lexer()
    assert lexer._count_leading_characters('    hello', ' ') == 4

def test_Lexer__count_leading_characters_tabs():
    lexer = Lexer()
    assert lexer._count_leading_characters('\t\thello', '\t') == 2

def test_Lexer__count_leading_characters_none():
    lexer = Lexer()
    assert lexer._count_leading_characters('hello', ' ') == 0


# === Lexer._detect_indent tests ===

def test_Lexer__detect_indent_spaces():
    lexer = Lexer()
    result = lexer._detect_indent('    hello')
    assert result == '    '

def test_Lexer__detect_indent_tab():
    lexer = Lexer()
    result = lexer._detect_indent('\thello')
    assert result == '\t'

def test_Lexer__detect_indent_no_indent():
    lexer = Lexer()
    result = lexer._detect_indent('hello')
    assert result is None


# === Lexer.tokenize tests ===

def test_Lexer_tokenize_simple_expression():
    lexer = Lexer()
    tokens = lexer.tokenize('1 + 2')
    names = [t.name for t in tokens]
    assert names == ['NUMBER', 'OPERATOR', 'NUMBER', 'NEWLINE']

def test_Lexer_tokenize_assignment():
    lexer = Lexer()
    tokens = lexer.tokenize('x = 42')
    names = [t.name for t in tokens]
    assert names == ['NAME', 'ASSIGN', 'NUMBER', 'NEWLINE']

def test_Lexer_tokenize_indent_dedent():
    lexer = Lexer()
    source = 'if x:\n    y'
    tokens = lexer.tokenize(source)
    names = [t.name for t in tokens]
    assert 'INDENT' in names
    assert 'DEDENT' in names

def test_Lexer_tokenize_multiple_indent_levels():
    lexer = Lexer()
    source = 'a\n    b\n        c\n    d\ne'
    tokens = lexer.tokenize(source)
    names = [t.name for t in tokens]
    indent_count = names.count('INDENT')
    dedent_count = names.count('DEDENT')
    assert indent_count == dedent_count

def test_Lexer_tokenize_empty_lines_skipped():
    lexer = Lexer()
    source = 'a\n\nb'
    tokens = lexer.tokenize(source)
    values = [t.value for t in tokens if t.name == 'NAME']
    assert values == ['a', 'b']

def test_Lexer_tokenize_keywords():
    lexer = Lexer()
    tokens = lexer.tokenize('if while for func return')
    keyword_names = [t.name for t in tokens if t.name != 'NEWLINE']
    assert keyword_names == ['IF', 'WHILE', 'FOR', 'FUNCTION', 'RETURN']

def test_Lexer_tokenize_all_operators():
    lexer = Lexer()
    tokens = lexer.tokenize('+ - * / %')
    ops = [t.value for t in tokens if t.name == 'OPERATOR']
    assert ops == ['+', '-', '*', '/', '%']

def test_Lexer_tokenize_comparison_operators():
    lexer = Lexer()
    tokens = lexer.tokenize('< > <= >= == !=')
    ops = [t.value for t in tokens if t.name == 'OPERATOR']
    assert ops == ['<', '>', '<=', '>=', '==', '!=']

def test_Lexer_tokenize_range_operators():
    lexer = Lexer()
    tokens = lexer.tokenize('1..5')
    ops = [t.value for t in tokens if t.name == 'OPERATOR']
    assert ops == ['..']

def test_Lexer_tokenize_range_inclusive():
    lexer = Lexer()
    tokens = lexer.tokenize('1...5')
    ops = [t.value for t in tokens if t.name == 'OPERATOR']
    assert ops == ['...']

def test_Lexer_tokenize_boolean_operators():
    lexer = Lexer()
    tokens = lexer.tokenize('a && b || c')
    ops = [t.value for t in tokens if t.name == 'OPERATOR']
    assert ops == ['&&', '||']

def test_Lexer_tokenize_brackets():
    lexer = Lexer()
    tokens = lexer.tokenize('f(x)[0]')
    names = [t.name for t in tokens if t.name != 'NEWLINE']
    assert names == ['NAME', 'LPAREN', 'NAME', 'RPAREN', 'LBRACK', 'NUMBER', 'RBRACK']

def test_Lexer_tokenize_dict_braces():
    lexer = Lexer()
    tokens = lexer.tokenize('{"a": 1}')
    names = [t.name for t in tokens if t.name != 'NEWLINE']
    assert 'LCBRACK' in names
    assert 'RCBRACK' in names

def test_Lexer_tokenize_source_lines():
    lexer = Lexer()
    lexer.tokenize('x = 1\ny = 2')
    assert len(lexer.source_lines) == 2

def test_Lexer_tokenize_float():
    lexer = Lexer()
    tokens = lexer.tokenize('3.14')
    num = [t for t in tokens if t.name == 'NUMBER'][0]
    assert num.value == 3.14
    assert isinstance(num.value, float)


# === TokenStream.__init__ tests ===

def test_TokenStream___init___pos():
    tokens = [Token('NUMBER', 1, 1, 1)]
    stream = TokenStream(tokens)
    assert stream._pos == 0
    assert stream._tokens is tokens


# === TokenStream.consume_expected tests ===

def test_TokenStream_consume_expected_success():
    tokens = [Token('NUMBER', 42, 1, 1), Token('OPERATOR', '+', 1, 3)]
    stream = TokenStream(tokens)
    result = stream.consume_expected('NUMBER')
    assert result.value == 42

def test_TokenStream_consume_expected_multiple():
    tokens = [Token('LPAREN', '(', 1, 1), Token('NUMBER', 1, 1, 2), Token('RPAREN', ')', 1, 3)]
    stream = TokenStream(tokens)
    result = stream.consume_expected('LPAREN', 'NUMBER')
    assert result.name == 'NUMBER'

def test_TokenStream_consume_expected_failure():
    tokens = [Token('NAME', 'x', 1, 1)]
    stream = TokenStream(tokens)
    with pytest.raises(AbrvalgSyntaxError):
        stream.consume_expected('NUMBER')


# === TokenStream.consume tests ===

def test_TokenStream_consume_advances():
    tokens = [Token('NUMBER', 1, 1, 1), Token('NUMBER', 2, 1, 3)]
    stream = TokenStream(tokens)
    first = stream.consume()
    assert first.value == 1
    second = stream.consume()
    assert second.value == 2

def test_TokenStream_consume_returns_current():
    tokens = [Token('NAME', 'x', 1, 1)]
    stream = TokenStream(tokens)
    result = stream.consume()
    assert result.name == 'NAME'
    assert result.value == 'x'


# === TokenStream.current tests ===

def test_TokenStream_current_returns_token():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    assert stream.current().value == 42

def test_TokenStream_current_does_not_advance():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    stream.current()
    stream.current()
    assert stream._pos == 0

def test_TokenStream_current_at_end():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    stream.consume()
    with pytest.raises(AbrvalgSyntaxError):
        stream.current()


# === TokenStream.expect_end tests ===

def test_TokenStream_expect_end_at_end():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    stream.consume()
    stream.expect_end()  # Should not raise

def test_TokenStream_expect_end_not_at_end():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    with pytest.raises(AbrvalgSyntaxError):
        stream.expect_end()


# === TokenStream.is_end tests ===

def test_TokenStream_is_end_false():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    assert stream.is_end() is False

def test_TokenStream_is_end_true():
    tokens = [Token('NUMBER', 42, 1, 1)]
    stream = TokenStream(tokens)
    stream.consume()
    assert stream.is_end() is True

def test_TokenStream_is_end_empty():
    stream = TokenStream([])
    assert stream.is_end() is True
