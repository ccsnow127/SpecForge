import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Tests for Interpreter.java -- pure unit tests for DocSearch signal.
 * Every Python test from test_interpreter.py has a corresponding Java test.
 */
public class InterpreterTest {

    // =========================================================================
    // Environment.__init__
    // =========================================================================

    @Test
    void test_Environment___init___empty() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertTrue(env.getValues().isEmpty());
        assertNull(env.getParent());
    }

    @Test
    void test_Environment___init___with_parent() {
        Interpreter.Environment parent = new Interpreter.Environment();
        Interpreter.Environment child = new Interpreter.Environment(parent);
        assertSame(parent, child.getParent());
        assertTrue(child.getValues().isEmpty());
    }

    @Test
    void test_Environment___init___with_args() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("x", 1);
        args.put("y", 2);
        Interpreter.Environment env = new Interpreter.Environment(null, args);
        assertEquals(1, env.get("x"));
        assertEquals(2, env.get("y"));
    }

    @Test
    void test_Environment___init___with_parent_and_args() {
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("a", 10);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("b", 20);
        Interpreter.Environment child = new Interpreter.Environment(parent, args);
        assertEquals(20, child.get("b"));
        assertEquals(10, child.get("a"));
    }

    // =========================================================================
    // Environment._from_dict
    // =========================================================================

    @Test
    void test_Environment__from_dict_populates() {
        Interpreter.Environment env = new Interpreter.Environment();
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("a", 1);
        dict.put("b", 2);
        env.fromDict(dict);
        assertEquals(1, env.get("a"));
        assertEquals(2, env.get("b"));
    }

    @Test
    void test_Environment__from_dict_empty() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.fromDict(new LinkedHashMap<>());
        assertTrue(env.asdict().isEmpty());
    }

    @Test
    void test_Environment__from_dict_uses_set() {
        Interpreter.Environment env = new Interpreter.Environment();
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("x", 42);
        env.fromDict(dict);
        assertEquals(42, env.getValues().get("x"));
    }

    // =========================================================================
    // Environment.set
    // =========================================================================

    @Test
    void test_Environment_set_new_key() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 42);
        assertEquals(42, env.getValues().get("x"));
    }

    @Test
    void test_Environment_set_overwrite() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 1);
        env.set("x", 2);
        assertEquals(2, env.getValues().get("x"));
    }

    @Test
    void test_Environment_set_none_value() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", null);
        assertTrue(env.getValues().containsKey("x"));
        assertNull(env.getValues().get("x"));
    }

    @Test
    void test_Environment_set_returns_none() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 42);
        assertEquals(42, env.get("x"));
    }

    // =========================================================================
    // Environment.get
    // =========================================================================

    @Test
    void test_Environment_get_local() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 10);
        assertEquals(10, env.get("x"));
    }

    @Test
    void test_Environment_get_parent_chain() {
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("x", 42);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        assertEquals(42, child.get("x"));
    }

    @Test
    void test_Environment_get_missing_returns_none() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertNull(env.get("missing"));
    }

    @Test
    void test_Environment_get_child_shadows_parent() {
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("x", 1);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        child.set("x", 2);
        assertEquals(2, child.get("x"));
    }

    @Test
    void test_Environment_get_grandparent() {
        Interpreter.Environment gp = new Interpreter.Environment();
        gp.set("x", 99);
        Interpreter.Environment parent = new Interpreter.Environment(gp);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        assertEquals(99, child.get("x"));
    }

    @Test
    void test_Environment_get_none_value_does_not_chain() {
        // If local value is null, get() returns null but checks parent due to Python semantics
        // Python: if val is None and self._parent is not None: return self._parent.get(key)
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("x", 42);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        child.set("x", null);
        // returns parent value 42 because null triggers parent lookup
        assertEquals(42, child.get("x"));
    }

    @Test
    void test_Environment_get_missing_no_parent() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertNull(env.get("y"));
    }

    @Test
    void test_Environment_get_zero_is_not_none() {
        // get() should return 0 without parent lookup since 0 is not null
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("x", 999);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        child.set("x", 0);
        assertEquals(0, child.get("x"));
    }

    @Test
    void test_Environment_get_empty_string_is_not_none() {
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("x", 999);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        child.set("x", "");
        assertEquals("", child.get("x"));
    }

    @Test
    void test_Environment_get_false_is_not_none() {
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("x", 999);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        child.set("x", false);
        assertEquals(false, child.get("x"));
    }

    // =========================================================================
    // Environment.asdict
    // =========================================================================

    @Test
    void test_Environment_asdict() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 1);
        env.set("y", 2);
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("x", 1);
        expected.put("y", 2);
        assertEquals(expected, env.asdict());
    }

    @Test
    void test_Environment_asdict_empty() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertTrue(env.asdict().isEmpty());
    }

    @Test
    void test_Environment_asdict_does_not_include_parent() {
        Interpreter.Environment parent = new Interpreter.Environment();
        parent.set("a", 1);
        Interpreter.Environment child = new Interpreter.Environment(parent);
        child.set("b", 2);
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("b", 2);
        assertEquals(expected, child.asdict());
    }

    // =========================================================================
    // Environment.__repr__
    // =========================================================================

    @Test
    void test_Environment___repr__() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 1);
        String result = env.toString();
        assertEquals("Environment({x=1})", result);
    }

    @Test
    void test_Environment___repr___empty() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals("Environment({})", env.toString());
    }

    // =========================================================================
    // Break / Continue / Return
    // =========================================================================

    @Test
    void test_Break___init__() {
        Interpreter.BreakException exc = new Interpreter.BreakException();
        assertNotNull(exc);
        assertTrue(exc instanceof RuntimeException);
    }

    @Test
    void test_Continue___init__() {
        Interpreter.ContinueException exc = new Interpreter.ContinueException();
        assertNotNull(exc);
        assertTrue(exc instanceof RuntimeException);
    }

    @Test
    void test_Return___init__() {
        Interpreter.ReturnException exc = new Interpreter.ReturnException(42);
        assertTrue(exc instanceof RuntimeException);
        assertEquals(42, exc.getReturnValue());
    }

    @Test
    void test_Return___init___none() {
        Interpreter.ReturnException exc = new Interpreter.ReturnException(null);
        assertNull(exc.getReturnValue());
    }

    // =========================================================================
    // eval_binary_operator
    // =========================================================================

    @Test
    void test_eval_binary_operator_add() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("+", new Interpreter.Number(2), new Interpreter.Number(3));
        assertEquals(5, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_sub() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("-", new Interpreter.Number(10), new Interpreter.Number(4));
        assertEquals(6, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_mul() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("*", new Interpreter.Number(3), new Interpreter.Number(7));
        assertEquals(21, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_div_returns_float() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("/", new Interpreter.Number(15), new Interpreter.Number(3));
        Object result = Interpreter.eval_binary_operator(node, env);
        assertEquals(5.0, result);
        assertTrue(result instanceof Double);
    }

    @Test
    void test_eval_binary_operator_mod() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("%", new Interpreter.Number(10), new Interpreter.Number(3));
        assertEquals(1, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_gt() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(true, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator(">", new Interpreter.Number(3), new Interpreter.Number(2)), env));
        assertEquals(false, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator(">", new Interpreter.Number(2), new Interpreter.Number(3)), env));
    }

    @Test
    void test_eval_binary_operator_ge() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(true, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator(">=", new Interpreter.Number(2), new Interpreter.Number(2)), env));
        assertEquals(false, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator(">=", new Interpreter.Number(1), new Interpreter.Number(2)), env));
    }

    @Test
    void test_eval_binary_operator_lt() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(true, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("<", new Interpreter.Number(1), new Interpreter.Number(2)), env));
        assertEquals(false, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("<", new Interpreter.Number(2), new Interpreter.Number(1)), env));
    }

    @Test
    void test_eval_binary_operator_le() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(false, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("<=", new Interpreter.Number(3), new Interpreter.Number(2)), env));
        assertEquals(true, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("<=", new Interpreter.Number(2), new Interpreter.Number(2)), env));
    }

    @Test
    void test_eval_binary_operator_eq() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(true, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("==", new Interpreter.Number(5), new Interpreter.Number(5)), env));
        assertEquals(false, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("==", new Interpreter.Number(5), new Interpreter.Number(3)), env));
    }

    @Test
    void test_eval_binary_operator_ne() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(true, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("!=", new Interpreter.Number(5), new Interpreter.Number(3)), env));
        assertEquals(false, Interpreter.eval_binary_operator(new Interpreter.BinaryOperator("!=", new Interpreter.Number(5), new Interpreter.Number(5)), env));
    }

    @Test
    void test_eval_binary_operator_logical_and_truthy() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("&&", new Interpreter.Number(1), new Interpreter.Number(2));
        assertEquals(true, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_logical_and_falsy() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("&&", new Interpreter.Number(0), new Interpreter.Number(1));
        assertEquals(false, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_logical_or_truthy() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("||", new Interpreter.Number(0), new Interpreter.Number(1));
        assertEquals(true, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_logical_or_falsy() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("||", new Interpreter.Number(0), new Interpreter.Number(0));
        assertEquals(false, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_and_short_circuit() {
        // && should not evaluate right side if left is falsy
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("&&", new Interpreter.Identifier("x"), new Interpreter.Identifier("undefined_var"));
        assertEquals(false, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_or_short_circuit() {
        // || should not evaluate right side if left is truthy
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 1);
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("||", new Interpreter.Identifier("x"), new Interpreter.Identifier("undefined_var"));
        assertEquals(true, Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_range_exclusive() {
        // .. creates range(start, end) -- exclusive of end
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(3));
        assertEquals(Arrays.asList(0, 1, 2), Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_range_inclusive() {
        // ... creates range(start, end+1) -- inclusive of end
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("...", new Interpreter.Number(0), new Interpreter.Number(3));
        assertEquals(Arrays.asList(0, 1, 2, 3), Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_range_exclusive_empty() {
        // 0..0 should produce empty range
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(0));
        assertEquals(Collections.emptyList(), Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_range_inclusive_single() {
        // 0...0 should iterate once (just 0)
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("...", new Interpreter.Number(0), new Interpreter.Number(0));
        assertEquals(Arrays.asList(0), Interpreter.eval_binary_operator(node, env));
    }

    @Test
    void test_eval_binary_operator_invalid() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("???", new Interpreter.Number(1), new Interpreter.Number(2));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Interpreter.eval_binary_operator(node, env));
        assertTrue(ex.getMessage().contains("Invalid operator"));
    }

    @Test
    void test_eval_binary_operator_nested() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.BinaryOperator node = new Interpreter.BinaryOperator("*",
            new Interpreter.BinaryOperator("+", new Interpreter.Number(2), new Interpreter.Number(3)),
            new Interpreter.Number(4));
        assertEquals(20, Interpreter.eval_binary_operator(node, env));
    }

    // =========================================================================
    // eval_unary_operator
    // =========================================================================

    @Test
    void test_eval_unary_operator_neg() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.UnaryOperator node = new Interpreter.UnaryOperator("-", new Interpreter.Number(5));
        assertEquals(-5, Interpreter.eval_unary_operator(node, env));
    }

    @Test
    void test_eval_unary_operator_neg_expression() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.UnaryOperator node = new Interpreter.UnaryOperator("-",
            new Interpreter.BinaryOperator("+", new Interpreter.Number(3), new Interpreter.Number(2)));
        assertEquals(-5, Interpreter.eval_unary_operator(node, env));
    }

    @Test
    void test_eval_unary_operator_not_truthy() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.UnaryOperator node = new Interpreter.UnaryOperator("!", new Interpreter.Number(1));
        assertEquals(false, Interpreter.eval_unary_operator(node, env));
    }

    @Test
    void test_eval_unary_operator_not_falsy() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.UnaryOperator node = new Interpreter.UnaryOperator("!", new Interpreter.Number(0));
        assertEquals(true, Interpreter.eval_unary_operator(node, env));
    }

    @Test
    void test_eval_unary_operator_double_neg() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.UnaryOperator node = new Interpreter.UnaryOperator("-",
            new Interpreter.UnaryOperator("-", new Interpreter.Number(5)));
        assertEquals(5, Interpreter.eval_unary_operator(node, env));
    }

    @Test
    void test_eval_unary_operator_neg_zero() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.UnaryOperator node = new Interpreter.UnaryOperator("-", new Interpreter.Number(0));
        assertEquals(0, Interpreter.eval_unary_operator(node, env));
    }

    // =========================================================================
    // eval_assignment
    // =========================================================================

    @Test
    void test_eval_assignment_simple() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Assignment node = new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Number(42));
        Interpreter.eval_assignment(node, env);
        assertEquals(42, env.get("x"));
    }

    @Test
    void test_eval_assignment_expression() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Assignment node = new Interpreter.Assignment(new Interpreter.Identifier("x"),
            new Interpreter.BinaryOperator("+", new Interpreter.Number(2), new Interpreter.Number(3)));
        Interpreter.eval_assignment(node, env);
        assertEquals(5, env.get("x"));
    }

    @Test
    void test_eval_assignment_subscript() {
        // Assignment to subscript delegates to eval_setitem
        Interpreter.Environment env = new Interpreter.Environment();
        List<Object> arr = new ArrayList<>(Arrays.asList(1, 2, 3));
        env.set("arr", arr);
        Interpreter.Assignment node = new Interpreter.Assignment(
            new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(0)),
            new Interpreter.Number(99));
        Interpreter.eval_assignment(node, env);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) env.get("arr");
        assertEquals(99, result.get(0));
    }

    @Test
    void test_eval_assignment_reassign() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.eval_assignment(new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Number(1)), env);
        Interpreter.eval_assignment(new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Number(2)), env);
        assertEquals(2, env.get("x"));
    }

    @Test
    void test_eval_assignment_uses_env_set() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Assignment node = new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Number(42));
        Interpreter.eval_assignment(node, env);
        assertEquals(42, env.get("x"));
    }

    // =========================================================================
    // eval_condition
    // =========================================================================

    @Test
    void test_eval_condition_if_true() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 1);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            Arrays.asList(new Interpreter.Number(10)),
            Collections.emptyList(),
            null
        );
        assertEquals(10, Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_if_false_returns_none() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 1);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(2)),
            Arrays.asList(new Interpreter.Number(10)),
            Collections.emptyList(),
            null
        );
        assertNull(Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_if_else() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 5);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator(">", new Interpreter.Identifier("x"), new Interpreter.Number(10)),
            Arrays.asList(new Interpreter.Number(1)),
            Collections.emptyList(),
            Arrays.asList(new Interpreter.Number(2))
        );
        assertEquals(2, Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_elif() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 2);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            Arrays.asList(new Interpreter.Number(10)),
            Arrays.asList(new Interpreter.ConditionElif(
                new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(2)),
                Arrays.asList(new Interpreter.Number(20))
            )),
            Arrays.asList(new Interpreter.Number(30))
        );
        assertEquals(20, Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_elif_chain() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 3);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            Arrays.asList(new Interpreter.Number(10)),
            Arrays.asList(
                new Interpreter.ConditionElif(
                    new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(2)),
                    Arrays.asList(new Interpreter.Number(20))),
                new Interpreter.ConditionElif(
                    new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(3)),
                    Arrays.asList(new Interpreter.Number(30)))
            ),
            Arrays.asList(new Interpreter.Number(40))
        );
        assertEquals(30, Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_elif_falls_through_to_else() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 99);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            Arrays.asList(new Interpreter.Number(10)),
            Arrays.asList(new Interpreter.ConditionElif(
                new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(2)),
                Arrays.asList(new Interpreter.Number(20))
            )),
            Arrays.asList(new Interpreter.Number(30))
        );
        assertEquals(30, Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_no_else_no_match() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 99);
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            Arrays.asList(new Interpreter.Number(10)),
            Arrays.asList(new Interpreter.ConditionElif(
                new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(2)),
                Arrays.asList(new Interpreter.Number(20))
            )),
            null
        );
        assertNull(Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_truthy_int() {
        // Non-zero ints are truthy
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.Number(42),
            Arrays.asList(new Interpreter.Number(1)),
            Collections.emptyList(),
            Arrays.asList(new Interpreter.Number(2))
        );
        assertEquals(1, Interpreter.eval_condition(node, env));
    }

    @Test
    void test_eval_condition_falsy_zero() {
        // Zero is falsy
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Condition node = new Interpreter.Condition(
            new Interpreter.Number(0),
            Arrays.asList(new Interpreter.Number(1)),
            Collections.emptyList(),
            Arrays.asList(new Interpreter.Number(2))
        );
        assertEquals(2, Interpreter.eval_condition(node, env));
    }

    // =========================================================================
    // eval_match
    // =========================================================================

    @Test
    void test_eval_match_first_pattern() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Match node = new Interpreter.Match(
            new Interpreter.Number(1),
            Arrays.asList(
                new Interpreter.MatchPattern(new Interpreter.Number(1), Arrays.asList(new Interpreter.Number(10))),
                new Interpreter.MatchPattern(new Interpreter.Number(2), Arrays.asList(new Interpreter.Number(20)))
            ),
            null
        );
        assertEquals(10, Interpreter.eval_match(node, env));
    }

    @Test
    void test_eval_match_second_pattern() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Match node = new Interpreter.Match(
            new Interpreter.Number(2),
            Arrays.asList(
                new Interpreter.MatchPattern(new Interpreter.Number(1), Arrays.asList(new Interpreter.Number(10))),
                new Interpreter.MatchPattern(new Interpreter.Number(2), Arrays.asList(new Interpreter.Number(20))),
                new Interpreter.MatchPattern(new Interpreter.Number(3), Arrays.asList(new Interpreter.Number(30)))
            ),
            null
        );
        assertEquals(20, Interpreter.eval_match(node, env));
    }

    @Test
    void test_eval_match_else() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Match node = new Interpreter.Match(
            new Interpreter.Number(99),
            Arrays.asList(new Interpreter.MatchPattern(new Interpreter.Number(1), Arrays.asList(new Interpreter.Number(10)))),
            Arrays.asList(new Interpreter.Number(0))
        );
        assertEquals(0, Interpreter.eval_match(node, env));
    }

    @Test
    void test_eval_match_no_match_no_else() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Match node = new Interpreter.Match(
            new Interpreter.Number(99),
            Arrays.asList(
                new Interpreter.MatchPattern(new Interpreter.Number(1), Arrays.asList(new Interpreter.Number(10))),
                new Interpreter.MatchPattern(new Interpreter.Number(2), Arrays.asList(new Interpreter.Number(20)))
            ),
            null
        );
        assertNull(Interpreter.eval_match(node, env));
    }

    @Test
    void test_eval_match_string_pattern() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Match node = new Interpreter.Match(
            new Interpreter.StringNode("hello"),
            Arrays.asList(
                new Interpreter.MatchPattern(new Interpreter.StringNode("hello"), Arrays.asList(new Interpreter.Number(1))),
                new Interpreter.MatchPattern(new Interpreter.StringNode("world"), Arrays.asList(new Interpreter.Number(2)))
            ),
            null
        );
        assertEquals(1, Interpreter.eval_match(node, env));
    }

    @Test
    void test_eval_match_evaluates_test_once() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Match node = new Interpreter.Match(
            new Interpreter.BinaryOperator("+", new Interpreter.Number(1), new Interpreter.Number(1)),
            Arrays.asList(
                new Interpreter.MatchPattern(new Interpreter.Number(2), Arrays.asList(new Interpreter.StringNode("found")))
            ),
            Arrays.asList(new Interpreter.StringNode("not found"))
        );
        assertEquals("found", Interpreter.eval_match(node, env));
    }

    // =========================================================================
    // eval_while_loop
    // =========================================================================

    @Test
    void test_eval_while_loop_basic() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.WhileLoop node = new Interpreter.WhileLoop(
            new Interpreter.BinaryOperator("<", new Interpreter.Identifier("x"), new Interpreter.Number(5)),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))))
        );
        Interpreter.eval_while_loop(node, env);
        assertEquals(5, env.get("x"));
    }

    @Test
    void test_eval_while_loop_break() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.WhileLoop node = new Interpreter.WhileLoop(
            new Interpreter.BinaryOperator("<", new Interpreter.Identifier("x"), new Interpreter.Number(100)),
            Arrays.asList(
                new Interpreter.Assignment(new Interpreter.Identifier("x"),
                    new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))),
                new Interpreter.Condition(
                    new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(3)),
                    Arrays.asList(new Interpreter.Break()),
                    Collections.emptyList(),
                    null
                )
            )
        );
        Interpreter.eval_while_loop(node, env);
        assertEquals(3, env.get("x"));
    }

    @Test
    void test_eval_while_loop_continue() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        env.set("y", 0);
        Interpreter.WhileLoop node = new Interpreter.WhileLoop(
            new Interpreter.BinaryOperator("<", new Interpreter.Identifier("x"), new Interpreter.Number(5)),
            Arrays.asList(
                new Interpreter.Assignment(new Interpreter.Identifier("x"),
                    new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))),
                new Interpreter.Condition(
                    new Interpreter.BinaryOperator("==", new Interpreter.Identifier("x"), new Interpreter.Number(3)),
                    Arrays.asList(new Interpreter.Continue()),
                    Collections.emptyList(),
                    null
                ),
                new Interpreter.Assignment(new Interpreter.Identifier("y"),
                    new Interpreter.BinaryOperator("+", new Interpreter.Identifier("y"), new Interpreter.Identifier("x")))
            )
        );
        Interpreter.eval_while_loop(node, env);
        assertEquals(12, env.get("y")); // 1+2+4+5
    }

    @Test
    void test_eval_while_loop_false_condition() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 10);
        Interpreter.WhileLoop node = new Interpreter.WhileLoop(
            new Interpreter.BinaryOperator("<", new Interpreter.Identifier("x"), new Interpreter.Number(5)),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))))
        );
        Interpreter.eval_while_loop(node, env);
        assertEquals(10, env.get("x"));
    }

    @Test
    void test_eval_while_loop_single_iteration() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.WhileLoop node = new Interpreter.WhileLoop(
            new Interpreter.BinaryOperator("<", new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))))
        );
        Interpreter.eval_while_loop(node, env);
        assertEquals(1, env.get("x"));
    }

    // =========================================================================
    // eval_for_loop
    // =========================================================================

    @Test
    void test_eval_for_loop_range() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "i",
            new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(5)),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Identifier("i"))))
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(10, env.get("x")); // 0+1+2+3+4
    }

    @Test
    void test_eval_for_loop_break() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "i",
            new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(10)),
            Arrays.asList(
                new Interpreter.Condition(
                    new Interpreter.BinaryOperator("==", new Interpreter.Identifier("i"), new Interpreter.Number(3)),
                    Arrays.asList(new Interpreter.Break()),
                    Collections.emptyList(),
                    null
                ),
                new Interpreter.Assignment(new Interpreter.Identifier("x"),
                    new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Identifier("i")))
            )
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(3, env.get("x")); // 0+1+2
    }

    @Test
    void test_eval_for_loop_continue() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "i",
            new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(5)),
            Arrays.asList(
                new Interpreter.Condition(
                    new Interpreter.BinaryOperator("==", new Interpreter.Identifier("i"), new Interpreter.Number(2)),
                    Arrays.asList(new Interpreter.Continue()),
                    Collections.emptyList(),
                    null
                ),
                new Interpreter.Assignment(new Interpreter.Identifier("x"),
                    new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Identifier("i")))
            )
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(8, env.get("x")); // 0+1+3+4
    }

    @Test
    void test_eval_for_loop_array() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "v",
            new Interpreter.Array(Arrays.asList(new Interpreter.Number(10), new Interpreter.Number(20), new Interpreter.Number(30))),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Identifier("v"))))
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(60, env.get("x"));
    }

    @Test
    void test_eval_for_loop_sets_var() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "i",
            new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(3)),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Identifier("i")))
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(2, env.get("x"));
    }

    @Test
    void test_eval_for_loop_empty_range() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "i",
            new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(0)),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))))
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(0, env.get("x"));
    }

    @Test
    void test_eval_for_loop_nested() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 0);
        Interpreter.ForLoop node = new Interpreter.ForLoop(
            "i",
            new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(3)),
            Arrays.asList(new Interpreter.ForLoop(
                "j",
                new Interpreter.BinaryOperator("..", new Interpreter.Number(0), new Interpreter.Number(3)),
                Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"),
                    new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(1))))
            ))
        );
        Interpreter.eval_for_loop(node, env);
        assertEquals(9, env.get("x"));
    }

    // =========================================================================
    // eval_function_declaration
    // =========================================================================

    @Test
    void test_eval_function_declaration_stores() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Function funcNode = new Interpreter.Function("myFunc", Arrays.asList("a"),
            Arrays.asList(new Interpreter.Return(new Interpreter.Identifier("a"))));
        Interpreter.eval_function_declaration(funcNode, env);
        assertSame(funcNode, env.get("myFunc"));
    }

    @Test
    void test_eval_function_declaration_via_evaluate() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("f", Collections.emptyList(),
            Arrays.asList(new Interpreter.Number(42)));
        Interpreter.eval_function_declaration(func, env);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Collections.emptyList());
        assertEquals(42, Interpreter.eval_call(call, env));
    }

    // =========================================================================
    // eval_call
    // =========================================================================

    @Test
    void test_eval_call_simple() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("double", Arrays.asList("x"),
            Arrays.asList(new Interpreter.BinaryOperator("*", new Interpreter.Identifier("x"), new Interpreter.Number(2))));
        env.set("double", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("double"), Arrays.asList(new Interpreter.Number(5)));
        assertEquals(10, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_wrong_arg_count() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("f", Arrays.asList("a", "b"),
            Arrays.asList(new Interpreter.BinaryOperator("+", new Interpreter.Identifier("a"), new Interpreter.Identifier("b"))));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Arrays.asList(new Interpreter.Number(1)));
        Interpreter.TypeError ex = assertThrows(Interpreter.TypeError.class, () -> Interpreter.eval_call(call, env));
        assertTrue(ex.getMessage().contains("Expected 2 arguments, got 1"));
    }

    @Test
    void test_eval_call_with_return() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("f", Arrays.asList("x"), Arrays.asList(
            new Interpreter.Condition(
                new Interpreter.BinaryOperator(">", new Interpreter.Identifier("x"), new Interpreter.Number(0)),
                Arrays.asList(new Interpreter.Return(new Interpreter.Identifier("x"))),
                Collections.emptyList(),
                null
            ),
            new Interpreter.Return(new Interpreter.Number(0))
        ));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Arrays.asList(new Interpreter.Number(5)));
        assertEquals(5, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_return_stops_execution() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("f", Collections.emptyList(),
            Arrays.asList(new Interpreter.Return(new Interpreter.Number(1)), new Interpreter.Return(new Interpreter.Number(2))));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Collections.emptyList());
        assertEquals(1, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_no_return_uses_last_value() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("f", Collections.emptyList(),
            Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3)));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Collections.emptyList());
        assertEquals(3, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_creates_new_scope() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 10);
        Interpreter.Function func = new Interpreter.Function("f", Collections.emptyList(),
            Arrays.asList(new Interpreter.Identifier("x")));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Collections.emptyList());
        assertEquals(10, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_does_not_modify_outer_scope() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 10);
        Interpreter.Function func = new Interpreter.Function("f", Collections.emptyList(),
            Arrays.asList(new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Number(99))));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"), Collections.emptyList());
        Interpreter.eval_call(call, env);
        assertEquals(10, env.get("x"));
    }

    @Test
    void test_eval_call_builtin_len() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("len"),
            Arrays.asList(new Interpreter.Array(Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3)))));
        assertEquals(3, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_builtin_len_empty() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("len"),
            Arrays.asList(new Interpreter.Array(Collections.emptyList())));
        assertEquals(0, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_builtin_str() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("str"),
            Arrays.asList(new Interpreter.Number(42)));
        assertEquals("42", Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_builtin_int() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("int"),
            Arrays.asList(new Interpreter.StringNode("42")));
        assertEquals(42, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_builtin_slice() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("slice"),
            Arrays.asList(
                new Interpreter.Array(Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3), new Interpreter.Number(4), new Interpreter.Number(5))),
                new Interpreter.Number(1),
                new Interpreter.Number(3)
            ));
        assertEquals(Arrays.asList(2, 3), Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_builtin_slice_from_start() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("slice"),
            Arrays.asList(
                new Interpreter.Array(Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3))),
                new Interpreter.Number(0),
                new Interpreter.Number(2)
            ));
        assertEquals(Arrays.asList(1, 2), Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_builtin_is_builtin_function() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Object fn = env.get("len");
        assertTrue(fn instanceof Interpreter.BuiltinFunction);
        assertEquals(Arrays.asList("iter"), ((Interpreter.BuiltinFunction) fn).getParams());
    }

    @Test
    void test_eval_call_recursive() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Function func = new Interpreter.Function("fact", Arrays.asList("n"), Arrays.asList(
            new Interpreter.Condition(
                new Interpreter.BinaryOperator("<=", new Interpreter.Identifier("n"), new Interpreter.Number(1)),
                Arrays.asList(new Interpreter.Return(new Interpreter.Number(1))),
                Collections.emptyList(),
                null
            ),
            new Interpreter.Return(new Interpreter.BinaryOperator("*", new Interpreter.Identifier("n"),
                new Interpreter.Call(new Interpreter.Identifier("fact"),
                    Arrays.asList(new Interpreter.BinaryOperator("-", new Interpreter.Identifier("n"), new Interpreter.Number(1))))))
        ));
        env.set("fact", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("fact"), Arrays.asList(new Interpreter.Number(5)));
        assertEquals(120, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_multiple_args() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Function func = new Interpreter.Function("add3", Arrays.asList("a", "b", "c"), Arrays.asList(
            new Interpreter.BinaryOperator("+",
                new Interpreter.BinaryOperator("+", new Interpreter.Identifier("a"), new Interpreter.Identifier("b")),
                new Interpreter.Identifier("c"))
        ));
        env.set("add3", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("add3"),
            Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3)));
        assertEquals(6, Interpreter.eval_call(call, env));
    }

    @Test
    void test_eval_call_args_evaluated_in_caller_env() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 10);
        Interpreter.Function func = new Interpreter.Function("f", Arrays.asList("a"),
            Arrays.asList(new Interpreter.Identifier("a")));
        env.set("f", func);
        Interpreter.Call call = new Interpreter.Call(new Interpreter.Identifier("f"),
            Arrays.asList(new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Number(5))));
        assertEquals(15, Interpreter.eval_call(call, env));
    }

    // =========================================================================
    // eval_identifier
    // =========================================================================

    @Test
    void test_eval_identifier_defined() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", 42);
        assertEquals(42, Interpreter.eval_identifier(new Interpreter.Identifier("x"), env));
    }

    @Test
    void test_eval_identifier_undefined() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.NameError ex = assertThrows(Interpreter.NameError.class,
            () -> Interpreter.eval_identifier(new Interpreter.Identifier("undefined_var"), env));
        assertTrue(ex.getMessage().contains("Name \"undefined_var\" is not defined"));
    }

    @Test
    void test_eval_identifier_builtin() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Identifier node = new Interpreter.Identifier("len");
        Object result = Interpreter.eval_identifier(node, env);
        assertTrue(result instanceof Interpreter.BuiltinFunction);
    }

    // =========================================================================
    // eval_getitem
    // =========================================================================

    @Test
    void test_eval_getitem_array() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("arr", new ArrayList<>(Arrays.asList(10, 20, 30)));
        Interpreter.SubscriptOperator node = new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(1));
        assertEquals(20, Interpreter.eval_getitem(node, env));
    }

    @Test
    void test_eval_getitem_array_first() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("arr", new ArrayList<>(Arrays.asList(10, 20, 30)));
        Interpreter.SubscriptOperator node = new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(0));
        assertEquals(10, Interpreter.eval_getitem(node, env));
    }

    @Test
    void test_eval_getitem_array_last() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("arr", new ArrayList<>(Arrays.asList(10, 20, 30)));
        Interpreter.SubscriptOperator node = new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(2));
        assertEquals(30, Interpreter.eval_getitem(node, env));
    }

    @Test
    void test_eval_getitem_dict() {
        Interpreter.Environment env = new Interpreter.Environment();
        Map<Object, Object> d = new LinkedHashMap<>();
        d.put("a", 1);
        d.put("b", 2);
        env.set("d", d);
        Interpreter.SubscriptOperator node = new Interpreter.SubscriptOperator(new Interpreter.Identifier("d"), new Interpreter.StringNode("a"));
        assertEquals(1, Interpreter.eval_getitem(node, env));
    }

    @Test
    void test_eval_getitem_nested() {
        Interpreter.Environment env = new Interpreter.Environment();
        List<Object> inner1 = new ArrayList<>(Arrays.asList(1, 2));
        List<Object> inner2 = new ArrayList<>(Arrays.asList(3, 4));
        List<Object> outer = new ArrayList<>(Arrays.asList(inner1, inner2));
        env.set("arr", outer);
        Interpreter.SubscriptOperator inner = new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(1));
        Interpreter.SubscriptOperator node = new Interpreter.SubscriptOperator(inner, new Interpreter.Number(0));
        assertEquals(3, Interpreter.eval_getitem(node, env));
    }

    @Test
    void test_eval_getitem_negative_index() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("arr", new ArrayList<>(Arrays.asList(10, 20, 30)));
        Interpreter.SubscriptOperator node = new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"),
            new Interpreter.UnaryOperator("-", new Interpreter.Number(1)));
        assertEquals(30, Interpreter.eval_getitem(node, env));
    }

    // =========================================================================
    // eval_setitem
    // =========================================================================

    @Test
    void test_eval_setitem_array() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("arr", new ArrayList<>(Arrays.asList(1, 2, 3)));
        Interpreter.Assignment node = new Interpreter.Assignment(
            new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(1)),
            new Interpreter.Number(99));
        Interpreter.eval_setitem(node, env);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) env.get("arr");
        assertEquals(99, result.get(1));
    }

    @Test
    void test_eval_setitem_dict() {
        Interpreter.Environment env = new Interpreter.Environment();
        Map<Object, Object> d = new LinkedHashMap<>();
        d.put("a", 1);
        env.set("d", d);
        Interpreter.Assignment node = new Interpreter.Assignment(
            new Interpreter.SubscriptOperator(new Interpreter.Identifier("d"), new Interpreter.StringNode("b")),
            new Interpreter.Number(2));
        Interpreter.eval_setitem(node, env);
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = (Map<Object, Object>) env.get("d");
        assertEquals(2, result.get("b"));
    }

    @Test
    void test_eval_setitem_dict_overwrite() {
        Interpreter.Environment env = new Interpreter.Environment();
        Map<Object, Object> d = new LinkedHashMap<>();
        d.put("a", 1);
        env.set("d", d);
        Interpreter.Assignment node = new Interpreter.Assignment(
            new Interpreter.SubscriptOperator(new Interpreter.Identifier("d"), new Interpreter.StringNode("a")),
            new Interpreter.Number(99));
        Interpreter.eval_setitem(node, env);
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = (Map<Object, Object>) env.get("d");
        assertEquals(99, result.get("a"));
    }

    @Test
    void test_eval_setitem_array_first() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("arr", new ArrayList<>(Arrays.asList(1, 2, 3)));
        Interpreter.Assignment node = new Interpreter.Assignment(
            new Interpreter.SubscriptOperator(new Interpreter.Identifier("arr"), new Interpreter.Number(0)),
            new Interpreter.Number(0));
        Interpreter.eval_setitem(node, env);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) env.get("arr");
        assertEquals(0, result.get(0));
    }

    // =========================================================================
    // eval_array
    // =========================================================================

    @Test
    void test_eval_array_literal() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Array node = new Interpreter.Array(Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3)));
        assertEquals(Arrays.asList(1, 2, 3), Interpreter.eval_array(node, env));
    }

    @Test
    void test_eval_array_empty() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Array node = new Interpreter.Array(Collections.emptyList());
        assertEquals(Collections.emptyList(), Interpreter.eval_array(node, env));
    }

    @Test
    void test_eval_array_expressions() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Array node = new Interpreter.Array(Arrays.asList(
            new Interpreter.BinaryOperator("+", new Interpreter.Number(1), new Interpreter.Number(1)),
            new Interpreter.BinaryOperator("*", new Interpreter.Number(2), new Interpreter.Number(3)),
            new Interpreter.BinaryOperator("-", new Interpreter.Number(10), new Interpreter.Number(1))
        ));
        assertEquals(Arrays.asList(2, 6, 9), Interpreter.eval_array(node, env));
    }

    @Test
    void test_eval_array_single() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Array node = new Interpreter.Array(Arrays.asList(new Interpreter.Number(42)));
        assertEquals(Arrays.asList(42), Interpreter.eval_array(node, env));
    }

    @Test
    void test_eval_array_nested() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Array node = new Interpreter.Array(Arrays.asList(
            new Interpreter.Array(Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2))),
            new Interpreter.Array(Arrays.asList(new Interpreter.Number(3), new Interpreter.Number(4)))
        ));
        List<Object> expected = new ArrayList<>();
        expected.add(Arrays.asList(1, 2));
        expected.add(Arrays.asList(3, 4));
        assertEquals(expected, Interpreter.eval_array(node, env));
    }

    @Test
    void test_eval_array_strings() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Array node = new Interpreter.Array(Arrays.asList(new Interpreter.StringNode("hello"), new Interpreter.StringNode("world")));
        assertEquals(Arrays.asList("hello", "world"), Interpreter.eval_array(node, env));
    }

    // =========================================================================
    // eval_dict
    // =========================================================================

    @Test
    void test_eval_dict_literal() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Dictionary node = new Interpreter.Dictionary(Arrays.asList(
            new AbstractMap.SimpleEntry<>(new Interpreter.StringNode("a"), new Interpreter.Number(1)),
            new AbstractMap.SimpleEntry<>(new Interpreter.StringNode("b"), new Interpreter.Number(2))
        ));
        Map<Object, Object> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        assertEquals(expected, Interpreter.eval_dict(node, env));
    }

    @Test
    void test_eval_dict_expression_values() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Dictionary node = new Interpreter.Dictionary(Arrays.asList(
            new AbstractMap.SimpleEntry<>(new Interpreter.StringNode("sum"),
                new Interpreter.BinaryOperator("+", new Interpreter.Number(1), new Interpreter.Number(2)))
        ));
        Map<Object, Object> expected = new LinkedHashMap<>();
        expected.put("sum", 3);
        assertEquals(expected, Interpreter.eval_dict(node, env));
    }

    @Test
    void test_eval_dict_single() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Dictionary node = new Interpreter.Dictionary(Arrays.asList(
            new AbstractMap.SimpleEntry<>(new Interpreter.StringNode("x"), new Interpreter.Number(42))
        ));
        Map<Object, Object> expected = new LinkedHashMap<>();
        expected.put("x", 42);
        assertEquals(expected, Interpreter.eval_dict(node, env));
    }

    @Test
    void test_eval_dict_numeric_keys() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Dictionary node = new Interpreter.Dictionary(Arrays.asList(
            new AbstractMap.SimpleEntry<>(new Interpreter.Number(1), new Interpreter.StringNode("one")),
            new AbstractMap.SimpleEntry<>(new Interpreter.Number(2), new Interpreter.StringNode("two"))
        ));
        Map<Object, Object> expected = new LinkedHashMap<>();
        expected.put(1, "one");
        expected.put(2, "two");
        assertEquals(expected, Interpreter.eval_dict(node, env));
    }

    // =========================================================================
    // eval_return
    // =========================================================================

    @Test
    void test_eval_return_value() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.Return node = new Interpreter.Return(new Interpreter.BinaryOperator("+", new Interpreter.Number(40), new Interpreter.Number(2)));
        assertEquals(42, Interpreter.eval_return(node, env));
    }

    @Test
    void test_eval_return_early() {
        Interpreter.Environment env = new Interpreter.Environment();
        List<Interpreter.AstNode> stmts = Arrays.asList(new Interpreter.Return(new Interpreter.Number(1)), new Interpreter.Return(new Interpreter.Number(2)));
        Interpreter.ReturnException ex = assertThrows(Interpreter.ReturnException.class, () -> Interpreter.eval_statements(stmts, env));
        assertEquals(1, ex.getReturnValue());
    }

    @Test
    void test_eval_return_conditional() {
        Interpreter.Environment env = new Interpreter.Environment();
        env.set("x", -5);
        Interpreter.Return node = new Interpreter.Return(new Interpreter.UnaryOperator("-", new Interpreter.Identifier("x")));
        assertEquals(5, Interpreter.eval_return(node, env));
    }

    @Test
    void test_eval_return_none() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Return node = new Interpreter.Return(null);
        Object result = Interpreter.eval_return(node, env);
        assertNull(result);
    }

    @Test
    void test_eval_return_with_expression() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.Return node = new Interpreter.Return(new Interpreter.Number(42));
        Object result = Interpreter.eval_return(node, env);
        assertEquals(42, result);
    }

    // =========================================================================
    // evaluators dict
    // =========================================================================

    @Test
    void test_evaluators_contains_all_types() {
        Class<?>[] expectedTypes = {
            Interpreter.Number.class, Interpreter.StringNode.class, Interpreter.Array.class,
            Interpreter.Dictionary.class, Interpreter.Identifier.class, Interpreter.BinaryOperator.class,
            Interpreter.UnaryOperator.class, Interpreter.SubscriptOperator.class, Interpreter.Assignment.class,
            Interpreter.Condition.class, Interpreter.Match.class, Interpreter.WhileLoop.class,
            Interpreter.ForLoop.class, Interpreter.Function.class, Interpreter.Call.class, Interpreter.Return.class,
        };
        for (Class<?> tp : expectedTypes) {
            assertTrue(Interpreter.evaluators.containsKey(tp), tp.getSimpleName() + " not in evaluators");
        }
    }

    @Test
    void test_evaluators_number() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(42, Interpreter.evaluators.get(Interpreter.Number.class).apply(new Interpreter.Number(42), env));
    }

    @Test
    void test_evaluators_string() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals("hello", Interpreter.evaluators.get(Interpreter.StringNode.class).apply(new Interpreter.StringNode("hello"), env));
    }

    @Test
    void test_evaluators_count() {
        assertEquals(16, Interpreter.evaluators.size());
    }

    // =========================================================================
    // eval_node
    // =========================================================================

    @Test
    void test_eval_node_number() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertEquals(42, Interpreter.eval_node(new Interpreter.Number(42), env));
    }

    @Test
    void test_eval_node_string() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertEquals("hello", Interpreter.eval_node(new Interpreter.StringNode("hello"), env));
    }

    @Test
    void test_eval_node_unknown_type() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertThrows(RuntimeException.class, () -> Interpreter.eval_node("not_a_node", env));
    }

    // =========================================================================
    // eval_expression
    // =========================================================================

    @Test
    void test_eval_expression_delegates() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertEquals(7, Interpreter.eval_expression(new Interpreter.Number(7), env));
    }

    // =========================================================================
    // eval_statement
    // =========================================================================

    @Test
    void test_eval_statement_delegates() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertEquals(99, Interpreter.eval_statement(new Interpreter.Number(99), env));
    }

    // =========================================================================
    // eval_statements
    // =========================================================================

    @Test
    void test_eval_statements_returns_last() {
        Interpreter.Environment env = new Interpreter.Environment();
        assertEquals(3, Interpreter.eval_statements(
            Arrays.asList(new Interpreter.Number(1), new Interpreter.Number(2), new Interpreter.Number(3)), env));
    }

    @Test
    void test_eval_statements_sequence() {
        Interpreter.Environment env = new Interpreter.Environment();
        List<Interpreter.AstNode> stmts = Arrays.asList(
            new Interpreter.Assignment(new Interpreter.Identifier("x"), new Interpreter.Number(1)),
            new Interpreter.Assignment(new Interpreter.Identifier("y"), new Interpreter.Number(2)),
            new Interpreter.BinaryOperator("+", new Interpreter.Identifier("x"), new Interpreter.Identifier("y"))
        );
        assertEquals(3, Interpreter.eval_statements(stmts, env));
    }

    @Test
    void test_eval_statements_break_raises() {
        Interpreter.Environment env = Interpreter.create_global_env();
        List<Interpreter.AstNode> stmts = Arrays.asList(new Interpreter.Number(1), new Interpreter.Break());
        assertThrows(Interpreter.BreakException.class, () -> Interpreter.eval_statements(stmts, env));
    }

    @Test
    void test_eval_statements_continue_raises() {
        Interpreter.Environment env = Interpreter.create_global_env();
        List<Interpreter.AstNode> stmts = Arrays.asList(new Interpreter.Number(1), new Interpreter.Continue());
        assertThrows(Interpreter.ContinueException.class, () -> Interpreter.eval_statements(stmts, env));
    }

    @Test
    void test_eval_statements_return_raises() {
        Interpreter.Environment env = Interpreter.create_global_env();
        List<Interpreter.AstNode> stmts = Arrays.asList(new Interpreter.Return(new Interpreter.Number(42)));
        Interpreter.ReturnException ex = assertThrows(Interpreter.ReturnException.class, () -> Interpreter.eval_statements(stmts, env));
        assertEquals(42, ex.getReturnValue());
    }

    @Test
    void test_eval_statements_empty() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertNull(Interpreter.eval_statements(Collections.emptyList(), env));
    }

    // =========================================================================
    // add_builtins
    // =========================================================================

    @Test
    void test_add_builtins_all_present() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.add_builtins(env);
        for (String name : new String[]{"print", "len", "slice", "str", "int"}) {
            assertNotNull(env.get(name));
        }
    }

    @Test
    void test_add_builtins_are_builtin_functions() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.add_builtins(env);
        for (String name : new String[]{"print", "len", "slice", "str", "int"}) {
            assertTrue(env.get(name) instanceof Interpreter.BuiltinFunction);
        }
    }

    @Test
    void test_add_builtins_len_params() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.add_builtins(env);
        assertEquals(Arrays.asList("iter"), ((Interpreter.BuiltinFunction) env.get("len")).getParams());
    }

    @Test
    void test_add_builtins_slice_params() {
        Interpreter.Environment env = new Interpreter.Environment();
        Interpreter.add_builtins(env);
        assertEquals(Arrays.asList("iter", "start", "stop"), ((Interpreter.BuiltinFunction) env.get("slice")).getParams());
    }

    // =========================================================================
    // create_global_env
    // =========================================================================

    @Test
    void test_create_global_env_type() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertNotNull(env);
        assertTrue(env instanceof Interpreter.Environment);
    }

    @Test
    void test_create_global_env_has_builtins() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertNotNull(env.get("len"));
        assertNotNull(env.get("print"));
    }

    @Test
    void test_create_global_env_no_parent() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertNull(env.getParent());
    }

    // =========================================================================
    // evaluate_env
    // =========================================================================

    @Test
    void test_evaluate_env_basic() {
        Interpreter.Environment env = Interpreter.create_global_env();
        assertEquals(3, Interpreter.evaluate_env("1 + 2", env));
    }

    @Test
    void test_evaluate_env_persists_state() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.evaluate_env("x = 42", env);
        assertEquals(42, Interpreter.evaluate_env("x", env));
    }

    @Test
    void test_evaluate_env_syntax_error_returns_none() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Object result = Interpreter.evaluate_env("===invalid===", env);
        assertNull(result);
    }

    @Test
    void test_evaluate_env_shares_env() {
        Interpreter.Environment env = Interpreter.create_global_env();
        Interpreter.evaluate_env("x = 1", env);
        Interpreter.evaluate_env("y = 2", env);
        assertEquals(3, Interpreter.evaluate_env("x + y", env));
    }

    // =========================================================================
    // evaluate
    // =========================================================================

    @Test
    void test_evaluate_expression() {
        assertEquals(14, Interpreter.evaluate("2 + 3 * 4"));
    }

    @Test
    void test_evaluate_string() {
        assertEquals("hello", Interpreter.evaluate("\"hello\""));
    }

    @Test
    void test_evaluate_assignment_and_use() {
        assertEquals(20, Interpreter.evaluate("x = 10\nx * 2"));
    }

    @Test
    void test_evaluate_creates_fresh_env() {
        Interpreter.evaluate("x = 42");
        assertThrows(Interpreter.NameError.class, () -> Interpreter.evaluate("x"));
    }

    // =========================================================================
    // BuiltinFunction
    // =========================================================================

    @Test
    void test_BuiltinFunction_is_namedtuple() {
        Interpreter.BuiltinFunction bf = new Interpreter.BuiltinFunction(
            Arrays.asList("a"),
            (args, e) -> args.get("a")
        );
        assertEquals(Arrays.asList("a"), bf.getParams());
        assertNotNull(bf.getBody());
    }

    @Test
    void test_BuiltinFunction_params_and_body() {
        Interpreter.BuiltinFunction bf = new Interpreter.BuiltinFunction(
            Arrays.asList("x", "y"),
            (args, e) -> (Integer) args.get("x") + (Integer) args.get("y")
        );
        assertEquals(Arrays.asList("x", "y"), bf.getParams());
        Map<String, Object> testArgs = new LinkedHashMap<>();
        testArgs.put("x", 1);
        testArgs.put("y", 2);
        assertEquals(3, bf.getBody().apply(testArgs, null));
    }
}
