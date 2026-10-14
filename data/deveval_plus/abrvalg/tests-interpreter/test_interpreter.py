"""Tests for interpreter.py — pure unit tests for DocSearch signal."""

import pytest
from interpreter import (
    evaluate, evaluate_env, create_global_env, add_builtins,
    Environment, BuiltinFunction, Break, Continue, Return,
    eval_node, eval_expression, eval_statement, eval_statements,
    eval_binary_operator, eval_unary_operator, eval_assignment,
    eval_condition, eval_match, eval_while_loop, eval_for_loop,
    eval_function_declaration, eval_call, eval_identifier,
    eval_getitem, eval_setitem, eval_array, eval_dict, eval_return,
    evaluators,
)
from abrvalg import ast


# =========================================================================
# Environment.__init__
# =========================================================================

def test_Environment___init___empty():
    env = Environment()
    assert env._values == {}
    assert env._parent is None

def test_Environment___init___with_parent():
    parent = Environment()
    child = Environment(parent=parent)
    assert child._parent is parent
    assert child._values == {}

def test_Environment___init___with_args():
    env = Environment(args={'x': 1, 'y': 2})
    assert env.get('x') == 1
    assert env.get('y') == 2

def test_Environment___init___with_parent_and_args():
    parent = Environment()
    parent.set('a', 10)
    child = Environment(parent=parent, args={'b': 20})
    assert child.get('b') == 20
    assert child.get('a') == 10


# =========================================================================
# Environment._from_dict
# =========================================================================

def test_Environment__from_dict_populates():
    env = Environment()
    env._from_dict({'a': 1, 'b': 2})
    assert env.get('a') == 1
    assert env.get('b') == 2

def test_Environment__from_dict_empty():
    env = Environment()
    env._from_dict({})
    assert env.asdict() == {}

def test_Environment__from_dict_uses_set():
    """_from_dict should use set() for each key-value pair."""
    env = Environment()
    env._from_dict({'x': 42})
    assert env._values['x'] == 42


# =========================================================================
# Environment.set
# =========================================================================

def test_Environment_set_new_key():
    env = Environment()
    env.set('x', 42)
    assert env._values['x'] == 42

def test_Environment_set_overwrite():
    env = Environment()
    env.set('x', 1)
    env.set('x', 2)
    assert env._values['x'] == 2

def test_Environment_set_none_value():
    env = Environment()
    env.set('x', None)
    assert 'x' in env._values
    assert env._values['x'] is None

def test_Environment_set_returns_none():
    """set() should store value but not return anything meaningful."""
    env = Environment()
    env.set('x', 42)
    assert env.get('x') == 42


# =========================================================================
# Environment.get
# =========================================================================

def test_Environment_get_local():
    env = Environment()
    env.set('x', 10)
    assert env.get('x') == 10

def test_Environment_get_parent_chain():
    parent = Environment()
    parent.set('x', 42)
    child = Environment(parent=parent)
    assert child.get('x') == 42

def test_Environment_get_missing_returns_none():
    env = Environment()
    assert env.get('missing') is None

def test_Environment_get_child_shadows_parent():
    parent = Environment()
    parent.set('x', 1)
    child = Environment(parent=parent)
    child.set('x', 2)
    assert child.get('x') == 2

def test_Environment_get_grandparent():
    gp = Environment()
    gp.set('x', 99)
    parent = Environment(parent=gp)
    child = Environment(parent=parent)
    assert child.get('x') == 99

def test_Environment_get_none_value_does_not_chain():
    """If local value is None, get() returns None without checking parent.
    This is a subtle behavior: val is None triggers parent lookup only if
    val was not found locally (i.e. key not in _values)."""
    parent = Environment()
    parent.set('x', 42)
    child = Environment(parent=parent)
    child.set('x', None)
    # The implementation checks `if val is None and self._parent is not None`
    # Since val=None (stored), it WILL look up parent and return 42
    assert child.get('x') == 42

def test_Environment_get_missing_no_parent():
    env = Environment()
    assert env.get('y') is None

def test_Environment_get_zero_is_not_none():
    """get() should return 0 without parent lookup since 0 is not None."""
    parent = Environment()
    parent.set('x', 999)
    child = Environment(parent=parent)
    child.set('x', 0)
    assert child.get('x') == 0

def test_Environment_get_empty_string_is_not_none():
    parent = Environment()
    parent.set('x', 999)
    child = Environment(parent=parent)
    child.set('x', '')
    assert child.get('x') == ''

def test_Environment_get_false_is_not_none():
    parent = Environment()
    parent.set('x', 999)
    child = Environment(parent=parent)
    child.set('x', False)
    assert child.get('x') == False


# =========================================================================
# Environment.asdict
# =========================================================================

def test_Environment_asdict():
    env = Environment()
    env.set('x', 1)
    env.set('y', 2)
    assert env.asdict() == {'x': 1, 'y': 2}

def test_Environment_asdict_empty():
    env = Environment()
    assert env.asdict() == {}

def test_Environment_asdict_does_not_include_parent():
    parent = Environment()
    parent.set('a', 1)
    child = Environment(parent=parent)
    child.set('b', 2)
    assert child.asdict() == {'b': 2}


# =========================================================================
# Environment.__repr__
# =========================================================================

def test_Environment___repr__():
    env = Environment()
    env.set('x', 1)
    result = repr(env)
    assert result == "Environment({'x': 1})"

def test_Environment___repr___empty():
    env = Environment()
    assert repr(env) == 'Environment({})'


# =========================================================================
# Break / Continue / Return
# =========================================================================

def test_Break___init__():
    exc = Break()
    assert isinstance(exc, Exception)

def test_Continue___init__():
    exc = Continue()
    assert isinstance(exc, Exception)

def test_Return___init__():
    exc = Return(42)
    assert isinstance(exc, Exception)
    assert exc.value == 42

def test_Return___init___none():
    exc = Return(None)
    assert exc.value is None


# =========================================================================
# eval_binary_operator
# =========================================================================

def test_eval_binary_operator_add():
    env = Environment()
    node = ast.BinaryOperator('+', ast.Number(2), ast.Number(3))
    assert eval_binary_operator(node, env) == 5

def test_eval_binary_operator_sub():
    env = Environment()
    node = ast.BinaryOperator('-', ast.Number(10), ast.Number(4))
    assert eval_binary_operator(node, env) == 6

def test_eval_binary_operator_mul():
    env = Environment()
    node = ast.BinaryOperator('*', ast.Number(3), ast.Number(7))
    assert eval_binary_operator(node, env) == 21

def test_eval_binary_operator_div_returns_float():
    """Division uses truediv, so 15/3 returns 5.0 not 5."""
    env = Environment()
    node = ast.BinaryOperator('/', ast.Number(15), ast.Number(3))
    result = eval_binary_operator(node, env)
    assert result == 5.0
    assert isinstance(result, float)

def test_eval_binary_operator_mod():
    env = Environment()
    node = ast.BinaryOperator('%', ast.Number(10), ast.Number(3))
    assert eval_binary_operator(node, env) == 1

def test_eval_binary_operator_gt():
    env = Environment()
    assert eval_binary_operator(ast.BinaryOperator('>', ast.Number(3), ast.Number(2)), env) == True
    assert eval_binary_operator(ast.BinaryOperator('>', ast.Number(2), ast.Number(3)), env) == False

def test_eval_binary_operator_ge():
    env = Environment()
    assert eval_binary_operator(ast.BinaryOperator('>=', ast.Number(2), ast.Number(2)), env) == True
    assert eval_binary_operator(ast.BinaryOperator('>=', ast.Number(1), ast.Number(2)), env) == False

def test_eval_binary_operator_lt():
    env = Environment()
    assert eval_binary_operator(ast.BinaryOperator('<', ast.Number(1), ast.Number(2)), env) == True
    assert eval_binary_operator(ast.BinaryOperator('<', ast.Number(2), ast.Number(1)), env) == False

def test_eval_binary_operator_le():
    env = Environment()
    assert eval_binary_operator(ast.BinaryOperator('<=', ast.Number(3), ast.Number(2)), env) == False
    assert eval_binary_operator(ast.BinaryOperator('<=', ast.Number(2), ast.Number(2)), env) == True

def test_eval_binary_operator_eq():
    env = Environment()
    assert eval_binary_operator(ast.BinaryOperator('==', ast.Number(5), ast.Number(5)), env) == True
    assert eval_binary_operator(ast.BinaryOperator('==', ast.Number(5), ast.Number(3)), env) == False

def test_eval_binary_operator_ne():
    env = Environment()
    assert eval_binary_operator(ast.BinaryOperator('!=', ast.Number(5), ast.Number(3)), env) == True
    assert eval_binary_operator(ast.BinaryOperator('!=', ast.Number(5), ast.Number(5)), env) == False

def test_eval_binary_operator_logical_and_truthy():
    env = Environment()
    node = ast.BinaryOperator('&&', ast.Number(1), ast.Number(2))
    assert eval_binary_operator(node, env) == True

def test_eval_binary_operator_logical_and_falsy():
    env = Environment()
    node = ast.BinaryOperator('&&', ast.Number(0), ast.Number(1))
    assert eval_binary_operator(node, env) == False

def test_eval_binary_operator_logical_or_truthy():
    env = Environment()
    node = ast.BinaryOperator('||', ast.Number(0), ast.Number(1))
    assert eval_binary_operator(node, env) == True

def test_eval_binary_operator_logical_or_falsy():
    env = Environment()
    node = ast.BinaryOperator('||', ast.Number(0), ast.Number(0))
    assert eval_binary_operator(node, env) == False

def test_eval_binary_operator_and_short_circuit():
    """&& should not evaluate right side if left is falsy."""
    env = Environment()
    env.set('x', 0)
    node = ast.BinaryOperator('&&', ast.Identifier('x'), ast.Identifier('undefined_var'))
    assert eval_binary_operator(node, env) == False

def test_eval_binary_operator_or_short_circuit():
    """|| should not evaluate right side if left is truthy."""
    env = Environment()
    env.set('x', 1)
    node = ast.BinaryOperator('||', ast.Identifier('x'), ast.Identifier('undefined_var'))
    assert eval_binary_operator(node, env) == True

def test_eval_binary_operator_range_exclusive():
    """.. creates range(start, end) — exclusive of end."""
    env = Environment()
    node = ast.BinaryOperator('..', ast.Number(0), ast.Number(3))
    assert list(eval_binary_operator(node, env)) == [0, 1, 2]

def test_eval_binary_operator_range_inclusive():
    """... creates range(start, end+1) — inclusive of end."""
    env = Environment()
    node = ast.BinaryOperator('...', ast.Number(0), ast.Number(3))
    assert list(eval_binary_operator(node, env)) == [0, 1, 2, 3]

def test_eval_binary_operator_range_exclusive_empty():
    """0..0 should produce empty range."""
    env = Environment()
    node = ast.BinaryOperator('..', ast.Number(0), ast.Number(0))
    assert list(eval_binary_operator(node, env)) == []

def test_eval_binary_operator_range_inclusive_single():
    """0...0 should iterate once (just 0)."""
    env = Environment()
    node = ast.BinaryOperator('...', ast.Number(0), ast.Number(0))
    assert list(eval_binary_operator(node, env)) == [0]

def test_eval_binary_operator_invalid():
    env = create_global_env()
    node = ast.BinaryOperator('???', ast.Number(1), ast.Number(2))
    with pytest.raises(Exception, match='Invalid operator'):
        eval_binary_operator(node, env)

def test_eval_binary_operator_nested():
    env = Environment()
    node = ast.BinaryOperator('*', ast.BinaryOperator('+', ast.Number(2), ast.Number(3)), ast.Number(4))
    assert eval_binary_operator(node, env) == 20


# =========================================================================
# eval_unary_operator
# =========================================================================

def test_eval_unary_operator_neg():
    env = Environment()
    node = ast.UnaryOperator('-', ast.Number(5))
    assert eval_unary_operator(node, env) == -5

def test_eval_unary_operator_neg_expression():
    env = Environment()
    node = ast.UnaryOperator('-', ast.BinaryOperator('+', ast.Number(3), ast.Number(2)))
    assert eval_unary_operator(node, env) == -5

def test_eval_unary_operator_not_truthy():
    env = Environment()
    node = ast.UnaryOperator('!', ast.Number(1))
    assert eval_unary_operator(node, env) == False

def test_eval_unary_operator_not_falsy():
    env = Environment()
    node = ast.UnaryOperator('!', ast.Number(0))
    assert eval_unary_operator(node, env) == True

def test_eval_unary_operator_double_neg():
    env = Environment()
    node = ast.UnaryOperator('-', ast.UnaryOperator('-', ast.Number(5)))
    assert eval_unary_operator(node, env) == 5

def test_eval_unary_operator_neg_zero():
    env = Environment()
    node = ast.UnaryOperator('-', ast.Number(0))
    assert eval_unary_operator(node, env) == 0


# =========================================================================
# eval_assignment
# =========================================================================

def test_eval_assignment_simple():
    env = Environment()
    node = ast.Assignment(ast.Identifier('x'), ast.Number(42))
    eval_assignment(node, env)
    assert env.get('x') == 42

def test_eval_assignment_expression():
    env = Environment()
    node = ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Number(2), ast.Number(3)))
    eval_assignment(node, env)
    assert env.get('x') == 5

def test_eval_assignment_subscript():
    """Assignment to subscript delegates to eval_setitem."""
    env = Environment()
    env.set('arr', [1, 2, 3])
    node = ast.Assignment(ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(0)), ast.Number(99))
    eval_assignment(node, env)
    assert env.get('arr')[0] == 99

def test_eval_assignment_reassign():
    env = Environment()
    eval_assignment(ast.Assignment(ast.Identifier('x'), ast.Number(1)), env)
    eval_assignment(ast.Assignment(ast.Identifier('x'), ast.Number(2)), env)
    assert env.get('x') == 2

def test_eval_assignment_uses_env_set():
    """Assignment stores via env.set, so get() retrieves it."""
    env = create_global_env()
    node = ast.Assignment(ast.Identifier('x'), ast.Number(42))
    eval_assignment(node, env)
    assert env.get('x') == 42


# =========================================================================
# eval_condition
# =========================================================================

def test_eval_condition_if_true():
    env = Environment()
    env.set('x', 1)
    node = ast.Condition(
        test=ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(1)),
        if_body=[ast.Number(10)],
        elifs=[],
        else_body=None,
    )
    assert eval_condition(node, env) == 10

def test_eval_condition_if_false_returns_none():
    env = Environment()
    env.set('x', 1)
    node = ast.Condition(
        test=ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(2)),
        if_body=[ast.Number(10)],
        elifs=[],
        else_body=None,
    )
    assert eval_condition(node, env) is None

def test_eval_condition_if_else():
    env = Environment()
    env.set('x', 5)
    node = ast.Condition(
        test=ast.BinaryOperator('>', ast.Identifier('x'), ast.Number(10)),
        if_body=[ast.Number(1)],
        elifs=[],
        else_body=[ast.Number(2)],
    )
    assert eval_condition(node, env) == 2

def test_eval_condition_elif():
    env = Environment()
    env.set('x', 2)
    node = ast.Condition(
        test=ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(1)),
        if_body=[ast.Number(10)],
        elifs=[ast.ConditionElif(ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(2)), [ast.Number(20)])],
        else_body=[ast.Number(30)],
    )
    assert eval_condition(node, env) == 20

def test_eval_condition_elif_chain():
    env = Environment()
    env.set('x', 3)
    node = ast.Condition(
        test=ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(1)),
        if_body=[ast.Number(10)],
        elifs=[
            ast.ConditionElif(ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(2)), [ast.Number(20)]),
            ast.ConditionElif(ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(3)), [ast.Number(30)]),
        ],
        else_body=[ast.Number(40)],
    )
    assert eval_condition(node, env) == 30

def test_eval_condition_elif_falls_through_to_else():
    env = Environment()
    env.set('x', 99)
    node = ast.Condition(
        test=ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(1)),
        if_body=[ast.Number(10)],
        elifs=[ast.ConditionElif(ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(2)), [ast.Number(20)])],
        else_body=[ast.Number(30)],
    )
    assert eval_condition(node, env) == 30

def test_eval_condition_no_else_no_match():
    env = Environment()
    env.set('x', 99)
    node = ast.Condition(
        test=ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(1)),
        if_body=[ast.Number(10)],
        elifs=[ast.ConditionElif(ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(2)), [ast.Number(20)])],
        else_body=None,
    )
    assert eval_condition(node, env) is None

def test_eval_condition_truthy_int():
    """Non-zero ints are truthy."""
    env = Environment()
    node = ast.Condition(
        test=ast.Number(42),
        if_body=[ast.Number(1)],
        elifs=[],
        else_body=[ast.Number(2)],
    )
    assert eval_condition(node, env) == 1

def test_eval_condition_falsy_zero():
    """Zero is falsy."""
    env = Environment()
    node = ast.Condition(
        test=ast.Number(0),
        if_body=[ast.Number(1)],
        elifs=[],
        else_body=[ast.Number(2)],
    )
    assert eval_condition(node, env) == 2


# =========================================================================
# eval_match
# =========================================================================

def test_eval_match_first_pattern():
    env = Environment()
    node = ast.Match(
        test=ast.Number(1),
        patterns=[
            ast.MatchPattern(ast.Number(1), [ast.Number(10)]),
            ast.MatchPattern(ast.Number(2), [ast.Number(20)]),
        ],
        else_body=None,
    )
    assert eval_match(node, env) == 10

def test_eval_match_second_pattern():
    env = Environment()
    node = ast.Match(
        test=ast.Number(2),
        patterns=[
            ast.MatchPattern(ast.Number(1), [ast.Number(10)]),
            ast.MatchPattern(ast.Number(2), [ast.Number(20)]),
            ast.MatchPattern(ast.Number(3), [ast.Number(30)]),
        ],
        else_body=None,
    )
    assert eval_match(node, env) == 20

def test_eval_match_else():
    env = Environment()
    node = ast.Match(
        test=ast.Number(99),
        patterns=[ast.MatchPattern(ast.Number(1), [ast.Number(10)])],
        else_body=[ast.Number(0)],
    )
    assert eval_match(node, env) == 0

def test_eval_match_no_match_no_else():
    env = Environment()
    node = ast.Match(
        test=ast.Number(99),
        patterns=[
            ast.MatchPattern(ast.Number(1), [ast.Number(10)]),
            ast.MatchPattern(ast.Number(2), [ast.Number(20)]),
        ],
        else_body=None,
    )
    assert eval_match(node, env) is None

def test_eval_match_string_pattern():
    env = Environment()
    node = ast.Match(
        test=ast.String("hello"),
        patterns=[
            ast.MatchPattern(ast.String("hello"), [ast.Number(1)]),
            ast.MatchPattern(ast.String("world"), [ast.Number(2)]),
        ],
        else_body=None,
    )
    assert eval_match(node, env) == 1

def test_eval_match_evaluates_test_once():
    """Match evaluates test expression, then compares with == against each pattern."""
    env = Environment()
    node = ast.Match(
        test=ast.BinaryOperator('+', ast.Number(1), ast.Number(1)),
        patterns=[
            ast.MatchPattern(ast.Number(2), [ast.String("found")]),
        ],
        else_body=[ast.String("not found")],
    )
    assert eval_match(node, env) == "found"


# =========================================================================
# eval_while_loop
# =========================================================================

def test_eval_while_loop_basic():
    env = Environment()
    env.set('x', 0)
    node = ast.WhileLoop(
        test=ast.BinaryOperator('<', ast.Identifier('x'), ast.Number(5)),
        body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1)))],
    )
    eval_while_loop(node, env)
    assert env.get('x') == 5

def test_eval_while_loop_break():
    env = Environment()
    env.set('x', 0)
    node = ast.WhileLoop(
        test=ast.BinaryOperator('<', ast.Identifier('x'), ast.Number(100)),
        body=[
            ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1))),
            ast.Condition(
                ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(3)),
                [ast.Break()],
                [],
                None,
            ),
        ],
    )
    eval_while_loop(node, env)
    assert env.get('x') == 3

def test_eval_while_loop_continue():
    env = Environment()
    env.set('x', 0)
    env.set('y', 0)
    node = ast.WhileLoop(
        test=ast.BinaryOperator('<', ast.Identifier('x'), ast.Number(5)),
        body=[
            ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1))),
            ast.Condition(
                ast.BinaryOperator('==', ast.Identifier('x'), ast.Number(3)),
                [ast.Continue()],
                [],
                None,
            ),
            ast.Assignment(ast.Identifier('y'), ast.BinaryOperator('+', ast.Identifier('y'), ast.Identifier('x'))),
        ],
    )
    eval_while_loop(node, env)
    assert env.get('y') == 12  # 1+2+4+5

def test_eval_while_loop_false_condition():
    """While loop with initially false condition should not execute body."""
    env = Environment()
    env.set('x', 10)
    node = ast.WhileLoop(
        test=ast.BinaryOperator('<', ast.Identifier('x'), ast.Number(5)),
        body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1)))],
    )
    eval_while_loop(node, env)
    assert env.get('x') == 10

def test_eval_while_loop_single_iteration():
    env = Environment()
    env.set('x', 0)
    node = ast.WhileLoop(
        test=ast.BinaryOperator('<', ast.Identifier('x'), ast.Number(1)),
        body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1)))],
    )
    eval_while_loop(node, env)
    assert env.get('x') == 1


# =========================================================================
# eval_for_loop
# =========================================================================

def test_eval_for_loop_range():
    env = Environment()
    env.set('x', 0)
    node = ast.ForLoop(
        var_name='i',
        collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(5)),
        body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Identifier('i')))],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 10  # 0+1+2+3+4

def test_eval_for_loop_break():
    env = Environment()
    env.set('x', 0)
    node = ast.ForLoop(
        var_name='i',
        collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(10)),
        body=[
            ast.Condition(
                ast.BinaryOperator('==', ast.Identifier('i'), ast.Number(3)),
                [ast.Break()],
                [],
                None,
            ),
            ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Identifier('i'))),
        ],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 3  # 0+1+2

def test_eval_for_loop_continue():
    env = Environment()
    env.set('x', 0)
    node = ast.ForLoop(
        var_name='i',
        collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(5)),
        body=[
            ast.Condition(
                ast.BinaryOperator('==', ast.Identifier('i'), ast.Number(2)),
                [ast.Continue()],
                [],
                None,
            ),
            ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Identifier('i'))),
        ],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 8  # 0+1+3+4

def test_eval_for_loop_array():
    env = Environment()
    env.set('x', 0)
    node = ast.ForLoop(
        var_name='v',
        collection=ast.Array([ast.Number(10), ast.Number(20), ast.Number(30)]),
        body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Identifier('v')))],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 60

def test_eval_for_loop_sets_var():
    """For loop variable should be accessible after loop ends."""
    env = Environment()
    node = ast.ForLoop(
        var_name='i',
        collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(3)),
        body=[ast.Assignment(ast.Identifier('x'), ast.Identifier('i'))],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 2

def test_eval_for_loop_empty_range():
    env = Environment()
    env.set('x', 0)
    node = ast.ForLoop(
        var_name='i',
        collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(0)),
        body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1)))],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 0

def test_eval_for_loop_nested():
    env = Environment()
    env.set('x', 0)
    node = ast.ForLoop(
        var_name='i',
        collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(3)),
        body=[ast.ForLoop(
            var_name='j',
            collection=ast.BinaryOperator('..', ast.Number(0), ast.Number(3)),
            body=[ast.Assignment(ast.Identifier('x'), ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(1)))],
        )],
    )
    eval_for_loop(node, env)
    assert env.get('x') == 9


# =========================================================================
# eval_function_declaration
# =========================================================================

def test_eval_function_declaration_stores():
    """Function declaration stores the AST Function node in env."""
    env = create_global_env()
    func_node = ast.Function('myFunc', ['a'], [ast.Return(ast.Identifier('a'))])
    eval_function_declaration(func_node, env)
    assert env.get('myFunc') is func_node

def test_eval_function_declaration_via_evaluate():
    env = Environment()
    func = ast.Function('f', [], [ast.Number(42)])
    eval_function_declaration(func, env)
    call = ast.Call(ast.Identifier('f'), [])
    assert eval_call(call, env) == 42


# =========================================================================
# eval_call
# =========================================================================

def test_eval_call_simple():
    env = Environment()
    func = ast.Function('double', ['x'], [ast.BinaryOperator('*', ast.Identifier('x'), ast.Number(2))])
    env.set('double', func)
    call = ast.Call(ast.Identifier('double'), [ast.Number(5)])
    assert eval_call(call, env) == 10

def test_eval_call_wrong_arg_count():
    env = Environment()
    func = ast.Function('f', ['a', 'b'], [ast.BinaryOperator('+', ast.Identifier('a'), ast.Identifier('b'))])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [ast.Number(1)])
    with pytest.raises(TypeError, match='Expected 2 arguments, got 1'):
        eval_call(call, env)

def test_eval_call_with_return():
    env = Environment()
    func = ast.Function('f', ['x'], [
        ast.Condition(
            ast.BinaryOperator('>', ast.Identifier('x'), ast.Number(0)),
            [ast.Return(ast.Identifier('x'))],
            [],
            None,
        ),
        ast.Return(ast.Number(0)),
    ])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [ast.Number(5)])
    assert eval_call(call, env) == 5

def test_eval_call_return_stops_execution():
    """Return should stop further statement evaluation in function body."""
    env = Environment()
    func = ast.Function('f', [], [ast.Return(ast.Number(1)), ast.Return(ast.Number(2))])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [])
    assert eval_call(call, env) == 1

def test_eval_call_no_return_uses_last_value():
    """Without return, the function returns the last evaluated expression."""
    env = Environment()
    func = ast.Function('f', [], [ast.Number(1), ast.Number(2), ast.Number(3)])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [])
    assert eval_call(call, env) == 3

def test_eval_call_creates_new_scope():
    """Function call creates new Environment with parent=calling env."""
    env = Environment()
    env.set('x', 10)
    func = ast.Function('f', [], [ast.Identifier('x')])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [])
    assert eval_call(call, env) == 10

def test_eval_call_does_not_modify_outer_scope():
    """Assignment in function should not modify outer env."""
    env = Environment()
    env.set('x', 10)
    func = ast.Function('f', [], [ast.Assignment(ast.Identifier('x'), ast.Number(99))])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [])
    eval_call(call, env)
    assert env.get('x') == 10

def test_eval_call_builtin_len():
    env = create_global_env()
    call = ast.Call(ast.Identifier('len'), [ast.Array([ast.Number(1), ast.Number(2), ast.Number(3)])])
    assert eval_call(call, env) == 3

def test_eval_call_builtin_len_empty():
    env = create_global_env()
    call = ast.Call(ast.Identifier('len'), [ast.Array([])])
    assert eval_call(call, env) == 0

def test_eval_call_builtin_str():
    env = create_global_env()
    call = ast.Call(ast.Identifier('str'), [ast.Number(42)])
    assert eval_call(call, env) == '42'

def test_eval_call_builtin_int():
    env = create_global_env()
    call = ast.Call(ast.Identifier('int'), [ast.String('42')])
    assert eval_call(call, env) == 42

def test_eval_call_builtin_slice():
    env = create_global_env()
    call = ast.Call(ast.Identifier('slice'), [
        ast.Array([ast.Number(1), ast.Number(2), ast.Number(3), ast.Number(4), ast.Number(5)]),
        ast.Number(1),
        ast.Number(3),
    ])
    assert eval_call(call, env) == [2, 3]

def test_eval_call_builtin_slice_from_start():
    env = create_global_env()
    call = ast.Call(ast.Identifier('slice'), [
        ast.Array([ast.Number(1), ast.Number(2), ast.Number(3)]),
        ast.Number(0),
        ast.Number(2),
    ])
    assert eval_call(call, env) == [1, 2]

def test_eval_call_builtin_is_builtin_function():
    """Builtins are BuiltinFunction namedtuples, dispatched differently from user functions."""
    env = create_global_env()
    fn = env.get('len')
    assert isinstance(fn, BuiltinFunction)
    assert fn.params == ['iter']

def test_eval_call_recursive():
    env = create_global_env()
    func = ast.Function('fact', ['n'], [
        ast.Condition(
            ast.BinaryOperator('<=', ast.Identifier('n'), ast.Number(1)),
            [ast.Return(ast.Number(1))],
            [],
            None,
        ),
        ast.Return(ast.BinaryOperator('*', ast.Identifier('n'),
            ast.Call(ast.Identifier('fact'), [ast.BinaryOperator('-', ast.Identifier('n'), ast.Number(1))])
        )),
    ])
    env.set('fact', func)
    call = ast.Call(ast.Identifier('fact'), [ast.Number(5)])
    assert eval_call(call, env) == 120

def test_eval_call_multiple_args():
    env = Environment()
    func = ast.Function('add3', ['a', 'b', 'c'], [
        ast.BinaryOperator('+', ast.BinaryOperator('+', ast.Identifier('a'), ast.Identifier('b')), ast.Identifier('c'))
    ])
    env.set('add3', func)
    call = ast.Call(ast.Identifier('add3'), [ast.Number(1), ast.Number(2), ast.Number(3)])
    assert eval_call(call, env) == 6

def test_eval_call_args_evaluated_in_caller_env():
    """Arguments are evaluated in calling env, not function env."""
    env = Environment()
    env.set('x', 10)
    func = ast.Function('f', ['a'], [ast.Identifier('a')])
    env.set('f', func)
    call = ast.Call(ast.Identifier('f'), [ast.BinaryOperator('+', ast.Identifier('x'), ast.Number(5))])
    assert eval_call(call, env) == 15


# =========================================================================
# eval_identifier
# =========================================================================

def test_eval_identifier_defined():
    env = Environment()
    env.set('x', 42)
    assert eval_identifier(ast.Identifier('x'), env) == 42

def test_eval_identifier_undefined():
    env = Environment()
    with pytest.raises(NameError, match='Name "undefined_var" is not defined'):
        eval_identifier(ast.Identifier('undefined_var'), env)

def test_eval_identifier_builtin():
    env = create_global_env()
    node = ast.Identifier('len')
    result = eval_identifier(node, env)
    assert isinstance(result, BuiltinFunction)


# =========================================================================
# eval_getitem
# =========================================================================

def test_eval_getitem_array():
    env = Environment()
    env.set('arr', [10, 20, 30])
    node = ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(1))
    assert eval_getitem(node, env) == 20

def test_eval_getitem_array_first():
    env = Environment()
    env.set('arr', [10, 20, 30])
    node = ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(0))
    assert eval_getitem(node, env) == 10

def test_eval_getitem_array_last():
    env = Environment()
    env.set('arr', [10, 20, 30])
    node = ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(2))
    assert eval_getitem(node, env) == 30

def test_eval_getitem_dict():
    env = Environment()
    env.set('d', {"a": 1, "b": 2})
    node = ast.SubscriptOperator(ast.Identifier('d'), ast.String("a"))
    assert eval_getitem(node, env) == 1

def test_eval_getitem_nested():
    env = Environment()
    env.set('arr', [[1, 2], [3, 4]])
    inner = ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(1))
    node = ast.SubscriptOperator(inner, ast.Number(0))
    assert eval_getitem(node, env) == 3

def test_eval_getitem_negative_index():
    env = Environment()
    env.set('arr', [10, 20, 30])
    node = ast.SubscriptOperator(ast.Identifier('arr'), ast.UnaryOperator('-', ast.Number(1)))
    assert eval_getitem(node, env) == 30


# =========================================================================
# eval_setitem
# =========================================================================

def test_eval_setitem_array():
    env = Environment()
    env.set('arr', [1, 2, 3])
    node = ast.Assignment(ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(1)), ast.Number(99))
    eval_setitem(node, env)
    assert env.get('arr')[1] == 99

def test_eval_setitem_dict():
    env = Environment()
    env.set('d', {"a": 1})
    node = ast.Assignment(ast.SubscriptOperator(ast.Identifier('d'), ast.String("b")), ast.Number(2))
    eval_setitem(node, env)
    assert env.get('d')["b"] == 2

def test_eval_setitem_dict_overwrite():
    env = Environment()
    env.set('d', {"a": 1})
    node = ast.Assignment(ast.SubscriptOperator(ast.Identifier('d'), ast.String("a")), ast.Number(99))
    eval_setitem(node, env)
    assert env.get('d')["a"] == 99

def test_eval_setitem_array_first():
    env = Environment()
    env.set('arr', [1, 2, 3])
    node = ast.Assignment(ast.SubscriptOperator(ast.Identifier('arr'), ast.Number(0)), ast.Number(0))
    eval_setitem(node, env)
    assert env.get('arr')[0] == 0


# =========================================================================
# eval_array
# =========================================================================

def test_eval_array_literal():
    env = Environment()
    node = ast.Array([ast.Number(1), ast.Number(2), ast.Number(3)])
    assert eval_array(node, env) == [1, 2, 3]

def test_eval_array_empty():
    env = Environment()
    node = ast.Array([])
    assert eval_array(node, env) == []

def test_eval_array_expressions():
    env = Environment()
    node = ast.Array([
        ast.BinaryOperator('+', ast.Number(1), ast.Number(1)),
        ast.BinaryOperator('*', ast.Number(2), ast.Number(3)),
        ast.BinaryOperator('-', ast.Number(10), ast.Number(1)),
    ])
    assert eval_array(node, env) == [2, 6, 9]

def test_eval_array_single():
    env = Environment()
    node = ast.Array([ast.Number(42)])
    assert eval_array(node, env) == [42]

def test_eval_array_nested():
    env = Environment()
    node = ast.Array([
        ast.Array([ast.Number(1), ast.Number(2)]),
        ast.Array([ast.Number(3), ast.Number(4)]),
    ])
    assert eval_array(node, env) == [[1, 2], [3, 4]]

def test_eval_array_strings():
    env = Environment()
    node = ast.Array([ast.String("hello"), ast.String("world")])
    assert eval_array(node, env) == ["hello", "world"]


# =========================================================================
# eval_dict
# =========================================================================

def test_eval_dict_literal():
    env = Environment()
    node = ast.Dictionary([(ast.String("a"), ast.Number(1)), (ast.String("b"), ast.Number(2))])
    assert eval_dict(node, env) == {"a": 1, "b": 2}

def test_eval_dict_expression_values():
    env = Environment()
    node = ast.Dictionary([(ast.String("sum"), ast.BinaryOperator('+', ast.Number(1), ast.Number(2)))])
    assert eval_dict(node, env) == {"sum": 3}

def test_eval_dict_single():
    env = Environment()
    node = ast.Dictionary([(ast.String("x"), ast.Number(42))])
    assert eval_dict(node, env) == {"x": 42}

def test_eval_dict_numeric_keys():
    env = Environment()
    node = ast.Dictionary([(ast.Number(1), ast.String("one")), (ast.Number(2), ast.String("two"))])
    assert eval_dict(node, env) == {1: "one", 2: "two"}


# =========================================================================
# eval_return
# =========================================================================

def test_eval_return_value():
    """eval_return evaluates and returns the value expression."""
    env = Environment()
    node = ast.Return(value=ast.BinaryOperator('+', ast.Number(40), ast.Number(2)))
    assert eval_return(node, env) == 42

def test_eval_return_early():
    """Return in eval_statements stops further execution."""
    env = Environment()
    stmts = [ast.Return(ast.Number(1)), ast.Return(ast.Number(2))]
    with pytest.raises(Return) as exc_info:
        eval_statements(stmts, env)
    assert exc_info.value.value == 1

def test_eval_return_conditional():
    """eval_return evaluates expression and returns its value."""
    env = Environment()
    env.set('x', -5)
    node = ast.Return(value=ast.UnaryOperator('-', ast.Identifier('x')))
    assert eval_return(node, env) == 5

def test_eval_return_none():
    """Return without value returns None."""
    env = create_global_env()
    node = ast.Return(value=None)
    result = eval_return(node, env)
    assert result is None

def test_eval_return_with_expression():
    env = create_global_env()
    node = ast.Return(value=ast.Number(42))
    result = eval_return(node, env)
    assert result == 42


# =========================================================================
# evaluators dict
# =========================================================================

def test_evaluators_contains_all_types():
    expected_types = [
        ast.Number, ast.String, ast.Array, ast.Dictionary,
        ast.Identifier, ast.BinaryOperator, ast.UnaryOperator,
        ast.SubscriptOperator, ast.Assignment, ast.Condition,
        ast.Match, ast.WhileLoop, ast.ForLoop, ast.Function,
        ast.Call, ast.Return,
    ]
    for tp in expected_types:
        assert tp in evaluators, f'{tp.__name__} not in evaluators'

def test_evaluators_number():
    env = Environment()
    assert evaluators[ast.Number](ast.Number(42), env) == 42

def test_evaluators_string():
    env = Environment()
    assert evaluators[ast.String](ast.String("hello"), env) == "hello"

def test_evaluators_count():
    """evaluators dict should have exactly 16 entries."""
    assert len(evaluators) == 16


# =========================================================================
# eval_node
# =========================================================================

def test_eval_node_number():
    env = create_global_env()
    assert eval_node(ast.Number(42), env) == 42

def test_eval_node_string():
    env = create_global_env()
    assert eval_node(ast.String("hello"), env) == "hello"

def test_eval_node_unknown_type():
    env = create_global_env()
    with pytest.raises(Exception, match='Unknown node'):
        eval_node("not_a_node", env)


# =========================================================================
# eval_expression
# =========================================================================

def test_eval_expression_delegates():
    env = create_global_env()
    assert eval_expression(ast.Number(7), env) == 7


# =========================================================================
# eval_statement
# =========================================================================

def test_eval_statement_delegates():
    env = create_global_env()
    assert eval_statement(ast.Number(99), env) == 99


# =========================================================================
# eval_statements
# =========================================================================

def test_eval_statements_returns_last():
    env = Environment()
    assert eval_statements([ast.Number(1), ast.Number(2), ast.Number(3)], env) == 3

def test_eval_statements_sequence():
    env = Environment()
    stmts = [
        ast.Assignment(ast.Identifier('x'), ast.Number(1)),
        ast.Assignment(ast.Identifier('y'), ast.Number(2)),
        ast.BinaryOperator('+', ast.Identifier('x'), ast.Identifier('y')),
    ]
    assert eval_statements(stmts, env) == 3

def test_eval_statements_break_raises():
    """Break AST node in statements raises Break exception."""
    env = create_global_env()
    stmts = [ast.Number(1), ast.Break()]
    with pytest.raises(Break):
        eval_statements(stmts, env)

def test_eval_statements_continue_raises():
    env = create_global_env()
    stmts = [ast.Number(1), ast.Continue()]
    with pytest.raises(Continue):
        eval_statements(stmts, env)

def test_eval_statements_return_raises():
    env = create_global_env()
    stmts = [ast.Return(ast.Number(42))]
    with pytest.raises(Return) as exc_info:
        eval_statements(stmts, env)
    assert exc_info.value.value == 42

def test_eval_statements_empty():
    env = create_global_env()
    assert eval_statements([], env) is None


# =========================================================================
# add_builtins
# =========================================================================

def test_add_builtins_all_present():
    env = Environment()
    add_builtins(env)
    for name in ['print', 'len', 'slice', 'str', 'int']:
        assert env.get(name) is not None

def test_add_builtins_are_builtin_functions():
    env = Environment()
    add_builtins(env)
    for name in ['print', 'len', 'slice', 'str', 'int']:
        assert isinstance(env.get(name), BuiltinFunction)

def test_add_builtins_len_params():
    env = Environment()
    add_builtins(env)
    assert env.get('len').params == ['iter']

def test_add_builtins_slice_params():
    env = Environment()
    add_builtins(env)
    assert env.get('slice').params == ['iter', 'start', 'stop']


# =========================================================================
# create_global_env
# =========================================================================

def test_create_global_env_type():
    env = create_global_env()
    assert isinstance(env, Environment)

def test_create_global_env_has_builtins():
    env = create_global_env()
    assert env.get('len') is not None
    assert env.get('print') is not None

def test_create_global_env_no_parent():
    env = create_global_env()
    assert env._parent is None


# =========================================================================
# evaluate_env
# =========================================================================

def test_evaluate_env_basic():
    env = create_global_env()
    assert evaluate_env('1 + 2', env) == 3

def test_evaluate_env_persists_state():
    env = create_global_env()
    evaluate_env('x = 42', env)
    assert evaluate_env('x', env) == 42

def test_evaluate_env_syntax_error_returns_none():
    env = create_global_env()
    result = evaluate_env('===invalid===', env)
    assert result is None

def test_evaluate_env_shares_env():
    """Multiple evaluate_env calls share the same environment."""
    env = create_global_env()
    evaluate_env('x = 1', env)
    evaluate_env('y = 2', env)
    assert evaluate_env('x + y', env) == 3


# =========================================================================
# evaluate
# =========================================================================

def test_evaluate_expression():
    assert evaluate('2 + 3 * 4') == 14

def test_evaluate_string():
    assert evaluate('"hello"') == "hello"

def test_evaluate_assignment_and_use():
    assert evaluate('x = 10\nx * 2') == 20

def test_evaluate_creates_fresh_env():
    """Each evaluate() call starts with a fresh global env."""
    evaluate('x = 42')
    with pytest.raises(NameError):
        evaluate('x')


# =========================================================================
# BuiltinFunction
# =========================================================================

def test_BuiltinFunction_is_namedtuple():
    bf = BuiltinFunction(params=['a'], body=lambda args, e: args['a'])
    assert bf.params == ['a']
    assert callable(bf.body)

def test_BuiltinFunction_params_and_body():
    bf = BuiltinFunction(['x', 'y'], lambda args, e: args['x'] + args['y'])
    assert bf.params == ['x', 'y']
    assert bf.body({'x': 1, 'y': 2}, None) == 3
