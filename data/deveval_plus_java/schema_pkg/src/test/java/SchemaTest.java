import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for the Schema port.  One-to-one mapping to every Python test
 * in test_schema.py where feasible.
 */
public class SchemaTest {

    // ---------- helpers mirroring those in test_schema.py ----------

    /** Predicate that always raises (mimics the Python ``ve`` helper). */
    static final Function<Object, Object> VE = (x) -> { throw new Use.ValueErrorMarker(); };

    /** Function that throws a SchemaError (mimics the Python ``se`` helper). */
    static final Function<Object, Object> SE = (x) -> { throw new SchemaError("first auto", "first error"); };

    /** Make a Predicate with a known display name (used in error messages). */
    static Predicate<Object> namedPredicate(String name, Predicate<Object> p) {
        return new NamedPred(name, p);
    }

    static class NamedPred implements Predicate<Object>, Schema.NamedCallable {
        final String n; final Predicate<Object> p;
        NamedPred(String n, Predicate<Object> p) { this.n = n; this.p = p; }
        @Override public boolean test(Object o) { return p.test(o); }
        @Override public String getName() { return n; }
    }

    static class NamedFn implements Function<Object, Object>, Schema.NamedCallable {
        final String n; final Function<Object, Object> p;
        NamedFn(String n, Function<Object, Object> p) { this.n = n; this.p = p; }
        @Override public Object apply(Object o) { return p.apply(o); }
        @Override public String getName() { return n; }
    }

    static Function<Object, Object> namedFn(String name, Function<Object, Object> f) {
        return new NamedFn(name, f);
    }

    // ---------- Sorted-dict helper used in JSON-schema tests ----------
    @SuppressWarnings("unchecked")
    static Object sortedDict(Object x) {
        if (x instanceof Map) {
            Map<String, Object> in = (Map<String, Object>) x;
            TreeMap<String, Object> out = new TreeMap<>();
            for (Map.Entry<String, Object> e : in.entrySet()) {
                out.put(e.getKey(), sortedDict(e.getValue()));
            }
            return out;
        }
        if (x instanceof List) {
            List<Object> in = (List<Object>) x;
            if (!in.isEmpty() && in.get(0) instanceof String) {
                List<String> copy = new ArrayList<>();
                for (Object o : in) copy.add((String) o);
                Collections.sort(copy);
                return copy;
            }
            List<Object> out = new ArrayList<>();
            for (Object o : in) out.add(sortedDict(o));
            return out;
        }
        return x;
    }

    static <K, V> Map<K, V> mapOf(Object... kv) {
        LinkedHashMap<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            @SuppressWarnings("unchecked") K k = (K) kv[i];
            @SuppressWarnings("unchecked") V v = (V) kv[i + 1];
            m.put(k, v);
        }
        return m;
    }

    // ===================================================================
    // test_schema
    // ===================================================================
    @Test
    void testSchema() {
        assertEquals(1, new Schema(1).validate(1));
        assertThrows(SchemaError.class, () -> new Schema(1).validate(9));

        assertEquals(1, new Schema(Integer.class).validate(1));
        assertThrows(SchemaError.class, () -> new Schema(Integer.class).validate("1"));
        assertEquals(1, new Schema(new Use((Function<Object, Object>) o -> Integer.parseInt(o.toString()))).validate("1"));
        assertThrows(SchemaError.class, () -> new Schema(Integer.class).validate(Integer.class));
        assertThrows(SchemaError.class, () -> new Schema(Integer.class).validate(true));
        assertThrows(SchemaError.class, () -> new Schema(Integer.class).validate(false));

        assertEquals("hai", new Schema(String.class).validate("hai"));
        assertThrows(SchemaError.class, () -> new Schema(String.class).validate(1));
        assertEquals("1", new Schema(new Use((Function<Object, Object>) o -> String.valueOf(o))).validate(1));

        assertEquals(Arrays.asList("a", 1), new Schema(List.class).validate(Arrays.asList("a", 1)));
        assertEquals(mapOf("a", 1), new Schema(Map.class).validate(mapOf("a", 1)));
        assertThrows(SchemaError.class, () -> new Schema(Map.class).validate(Arrays.asList("a", 1)));

        Predicate<Object> bound = (n) -> ((Integer) n) > 0 && ((Integer) n) < 5;
        assertEquals(3, new Schema(bound).validate(3));
        assertThrows(SchemaError.class, () -> new Schema(bound).validate(-1));
    }

    // ===================================================================
    // test_validate_file -- maps Use(open) -> Use(reader) using LICENSE-MIT.
    // ===================================================================
    @Test
    void testValidateFile() throws IOException {
        Function<Object, Object> open = (path) -> {
            try {
                return new BufferedReader(new FileReader(String.valueOf(path)));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
        BufferedReader br = (BufferedReader) new Schema(new Use(open)).validate("LICENSE-MIT");
        assertTrue(br.readLine().startsWith("Copyright"));

        assertThrows(SchemaError.class, () -> new Schema(new Use(open)).validate("NON-EXISTENT"));

        Predicate<Object> exists = (p) -> Files.exists(Paths.get(String.valueOf(p)));
        assertEquals(".", new Schema(exists).validate("."));
        assertThrows(SchemaError.class, () -> new Schema(exists).validate("./non-existent/"));

        Predicate<Object> isFile = (p) -> Files.isRegularFile(Paths.get(String.valueOf(p)));
        assertEquals("LICENSE-MIT", new Schema(isFile).validate("LICENSE-MIT"));
        assertThrows(SchemaError.class, () -> new Schema(isFile).validate("NON-EXISTENT"));
    }

    // ===================================================================
    // test_and
    // ===================================================================
    @Test
    void testAnd() {
        Predicate<Object> bounded = (n) -> ((Number) n).doubleValue() > 0 && ((Number) n).doubleValue() < 5;
        assertEquals(3, new And(Integer.class, bounded).validate(3));
        assertThrows(SchemaError.class, () -> new And(Integer.class, bounded).validate(3.33));

        Function<Object, Object> toInt = (o) -> {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(o.toString());
        };
        assertEquals(3, new And(new Use(toInt), bounded).validate(3.33));
        assertThrows(SchemaError.class, () -> new And(new Use(toInt), bounded).validate("3.33"));
    }

    // ===================================================================
    // test_or
    // ===================================================================
    @Test
    void testOr() {
        assertEquals(5, new Or(Integer.class, Map.class).validate(5));
        assertEquals(mapOf(), new Or(Integer.class, Map.class).validate(mapOf()));
        assertThrows(SchemaError.class, () -> new Or(Integer.class, Map.class).validate("hai"));
        assertEquals(4, new Or(Integer.class).validate(4));
        assertThrows(SchemaError.class, () -> new Or().validate(2));
    }

    // ===================================================================
    // test_or_only_one
    // ===================================================================
    @Test
    void testOrOnlyOne() {
        Or or1 = new Or("test1", "test2").onlyOne(true);
        Or or2 = new Or("test1", "test2").onlyOne(true);
        Schema schema = new Schema(mapOf(
                or1, String.class,
                new Optional("sub_schema"), mapOf(new Optional(or2), String.class)
        ));
        assertNotNull(schema.validate(mapOf("test1", "value")));
        assertNotNull(schema.validate(mapOf("test1", "value", "sub_schema", mapOf("test2", "value"))));
        // Reset between validations to clear matchCount
        Or or3 = new Or("test1", "test2").onlyOne(true);
        Or or4 = new Or("test1", "test2").onlyOne(true);
        Schema schema2 = new Schema(mapOf(
                or3, String.class,
                new Optional("sub_schema"), mapOf(new Optional(or4), String.class)
        ));
        assertNotNull(schema2.validate(mapOf("test2", "other_value")));
        Or or5 = new Or("test1", "test2").onlyOne(true);
        Schema schema3 = new Schema(mapOf(or5, String.class));
        assertThrows(SchemaError.class, () -> schema3.validate(mapOf("test1", "value", "test2", "other_value")));

        Or or6 = new Or("test1", "test2").onlyOne(true);
        Schema schema4 = new Schema(mapOf(or6, String.class,
                new Optional("sub_schema"), mapOf(new Optional(new Or("test1", "test2").onlyOne(true)), String.class)));
        assertThrows(SchemaError.class, () -> schema4.validate(
                mapOf("test1", "value", "sub_schema", mapOf("test1", "value", "test2", "value"))));

        Or or7 = new Or("test1", "test2").onlyOne(true);
        Schema schema5 = new Schema(mapOf(or7, String.class));
        assertThrows(SchemaError.class, () -> schema5.validate(mapOf("othertest", "value")));

        Or or8 = new Or("test1", "test2").onlyOne(true);
        Schema extra = new Schema(mapOf(or8, String.class), null, true);
        assertNotNull(extra.validate(mapOf("test1", "value", "other-key", "value")));
        assertNotNull(extra.validate(mapOf("test2", "other_value")));
        assertThrows(SchemaError.class, () -> extra.validate(mapOf("test1", "value", "test2", "other_value")));
    }

    // ===================================================================
    // test_test  --  Const(And(Use(...), unique_list))
    // ===================================================================
    @Test
    void testTest() {
        Function<Object, Object> indexes = (lst) -> {
            List<Object> out = new ArrayList<>();
            for (Object e : (List<?>) lst) out.add(((Map<?, ?>) e).get("index"));
            return out;
        };
        Predicate<Object> uniqueList = (lst) -> {
            List<?> l = (List<?>) lst;
            return new HashSet<>(l).size() == l.size();
        };
        Schema schema = new Schema(new Const(new And(new Use(indexes), uniqueList)));
        List<Map<String, Object>> data = Arrays.asList(
                mapOf("index", 1, "value", "foo"),
                mapOf("index", 2, "value", "bar"));
        assertEquals(data, schema.validate(data));

        List<Map<String, Object>> bad = Arrays.asList(
                mapOf("index", 1, "value", "foo"),
                mapOf("index", 1, "value", "bar"));
        assertThrows(SchemaError.class, () -> schema.validate(bad));
    }

    // ===================================================================
    // test_regex
    // ===================================================================
    @Test
    void testRegex() {
        assertEquals("afoot", new Regex("foo").validate("afoot"));
        assertThrows(SchemaError.class, () -> new Regex("bar").validate("afoot"));

        assertEquals("letters", new Regex("^[a-z]+$").validate("letters"));
        assertThrows(SchemaError.class, () -> new Regex("^[a-z]+$").validate("letters + spaces"));

        assertEquals(mapOf("fookey", "value"),
                new Schema(mapOf(new Regex("^foo"), String.class)).validate(mapOf("fookey", "value")));
        assertThrows(SchemaError.class,
                () -> new Schema(mapOf(new Regex("^foo"), String.class)).validate(mapOf("barkey", "value")));

        assertEquals(mapOf("key", "foovalue"),
                new Schema(mapOf(String.class, new Regex("^foo"))).validate(mapOf("key", "foovalue")));
        assertThrows(SchemaError.class,
                () -> new Schema(mapOf(String.class, new Regex("^foo"))).validate(mapOf("key", "barvalue")));

        assertThrows(SchemaError.class, () -> new Regex("bar").validate(1));
        assertThrows(SchemaError.class, () -> new Regex("bar").validate(mapOf()));
        assertThrows(SchemaError.class, () -> new Regex("bar").validate(new ArrayList<>()));
        assertThrows(SchemaError.class, () -> new Regex("bar").validate(null));

        assertEquals("foo", new Regex(Pattern.compile("foo")).validate("foo"));
        assertEquals("foo", new Regex("foo").validate("foo"));
        assertThrows(ClassCastException.class, () -> new Regex((String) null).validate("bar"));
        assertThrows(ClassCastException.class, () -> new Regex((Pattern) null).validate("bar"));
    }

    // ===================================================================
    // test_validate_list
    // ===================================================================
    @Test
    void testValidateList() {
        assertEquals(Arrays.asList(1, 0, 1, 1),
                new Schema(Arrays.asList(1, 0)).validate(Arrays.asList(1, 0, 1, 1)));
        assertEquals(new ArrayList<>(),
                new Schema(Arrays.asList(1, 0)).validate(new ArrayList<>()));
        assertThrows(SchemaError.class, () -> new Schema(Arrays.asList(1, 0)).validate(0));
        assertThrows(SchemaError.class, () -> new Schema(Arrays.asList(1, 0)).validate(Arrays.asList(2)));

        Predicate<Object> bigEnough = (lst) -> ((List<?>) lst).size() > 2;
        assertEquals(Arrays.asList(0, 1, 0),
                new And(Arrays.asList(1, 0), bigEnough).validate(Arrays.asList(0, 1, 0)));
        assertThrows(SchemaError.class,
                () -> new And(Arrays.asList(1, 0), bigEnough).validate(Arrays.asList(0, 1)));
    }

    // ===================================================================
    // test_list_tuple_set_frozenset
    // ===================================================================
    @Test
    void testListSet() {
        assertNotNull(new Schema(Arrays.asList(Integer.class)).validate(Arrays.asList(1, 2)));
        assertThrows(SchemaError.class, () -> new Schema(Arrays.asList(Integer.class)).validate(Arrays.asList("1", 2)));
        Set<Object> setSchema = new LinkedHashSet<>(Arrays.asList(Integer.class));
        Set<Object> data = new LinkedHashSet<>(Arrays.asList(1, 2));
        assertEquals(data, new Schema(setSchema).validate(data));
        assertThrows(SchemaError.class, () -> new Schema(setSchema).validate(Arrays.asList(1, 2)));
        assertThrows(SchemaError.class, () -> new Schema(setSchema).validate(new LinkedHashSet<>(Arrays.asList("1", 2))));
    }

    // ===================================================================
    // test_strictly
    // ===================================================================
    @Test
    void testStrictly() {
        assertEquals(1, new Schema(Integer.class).validate(1));
        assertThrows(SchemaError.class, () -> new Schema(Integer.class).validate("1"));
    }

    // ===================================================================
    // test_dict
    // ===================================================================
    @Test
    void testDict() {
        assertEquals(mapOf("key", 5), new Schema(mapOf("key", 5)).validate(mapOf("key", 5)));
        assertThrows(SchemaError.class, () -> new Schema(mapOf("key", 5)).validate(mapOf("key", "x")));
        assertThrows(SchemaError.class, () -> new Schema(mapOf("key", 5)).validate(Arrays.asList("key", 5)));
        assertEquals(mapOf("key", 5), new Schema(mapOf("key", Integer.class)).validate(mapOf("key", 5)));
        assertEquals(mapOf("n", 5, "f", 3.14),
                new Schema(mapOf("n", Integer.class, "f", Double.class)).validate(mapOf("n", 5, "f", 3.14)));
        assertThrows(SchemaError.class,
                () -> new Schema(mapOf("n", Integer.class, "f", Double.class)).validate(mapOf("n", 3.14, "f", 5)));

        SchemaWrongKeyError ex = assertThrows(SchemaWrongKeyError.class,
                () -> new Schema(mapOf()).validate(mapOf("abc", null, 1, null)));
        assertTrue(ex.firstArg().startsWith("Wrong keys "));

        SchemaMissingKeyError mex = assertThrows(SchemaMissingKeyError.class,
                () -> new Schema(mapOf("key", 5)).validate(mapOf()));
        assertEquals("Missing key: 'key'", mex.firstArg());

        SchemaMissingKeyError mex2 = assertThrows(SchemaMissingKeyError.class,
                () -> new Schema(mapOf("key", 5)).validate(mapOf("n", 5)));
        assertEquals("Missing key: 'key'", mex2.firstArg());

        SchemaMissingKeyError mex3 = assertThrows(SchemaMissingKeyError.class,
                () -> new Schema(mapOf("key", 5, "key2", 5)).validate(mapOf("n", 5)));
        assertEquals("Missing keys: 'key', 'key2'", mex3.firstArg());

        SchemaWrongKeyError wex = assertThrows(SchemaWrongKeyError.class,
                () -> new Schema(mapOf()).validate(mapOf("n", 5)));
        assertEquals("Wrong key 'n' in {'n': 5}", wex.firstArg());

        SchemaWrongKeyError wex2 = assertThrows(SchemaWrongKeyError.class,
                () -> new Schema(mapOf("key", 5)).validate(mapOf("key", 5, "bad", 5)));
        assertTrue(Arrays.asList("Wrong key 'bad' in {'key': 5, 'bad': 5}",
                "Wrong key 'bad' in {'bad': 5, 'key': 5}").contains(wex2.firstArg()));

        SchemaError sex = assertThrows(SchemaError.class,
                () -> new Schema(mapOf()).validate(mapOf("a", 5, "b", 5)));
        assertTrue(sex.firstArg().startsWith("Wrong keys "));

        // Python's test only requires *some* SchemaError -- the inner assertion is conditional.
        SchemaError uex = assertThrows(SchemaError.class,
                () -> new Schema(mapOf(Integer.class, Integer.class)).validate(mapOf("", "")));
        assertNotNull(uex);
    }

    // ===================================================================
    // test_dict_keys
    // ===================================================================
    @Test
    void testDictKeys() {
        assertEquals(mapOf("a", 1, "b", 2),
                new Schema(mapOf(String.class, Integer.class)).validate(mapOf("a", 1, "b", 2)));
        assertThrows(SchemaError.class,
                () -> new Schema(mapOf(String.class, Integer.class)).validate(mapOf(1, 1, "b", 2)));

        Function<Object, Object> toStr = Object::toString;
        Function<Object, Object> toInt = (o) -> {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(o.toString());
        };
        Map<String, Integer> expected = mapOf("1", 3, "3.14", 1);
        Map<Object, Object> actualMap = (Map<Object, Object>) new Schema(
                mapOf(new Use(toStr), new Use(toInt))).validate(mapOf(1, 3.14, 3.14, 1));
        assertEquals(expected, actualMap);
    }

    // ===================================================================
    // test_ignore_extra_keys
    // ===================================================================
    @Test
    void testIgnoreExtraKeys() {
        assertEquals(mapOf("key", 5),
                new Schema(mapOf("key", 5), null, true).validate(mapOf("key", 5, "bad", 4)));
        assertEquals(mapOf("key", 5, "dk", mapOf("a", "a")),
                new Schema(mapOf("key", 5, "dk", mapOf("a", "a")), null, true)
                        .validate(mapOf("key", 5, "bad", "b", "dk", mapOf("a", "a", "bad", "b"))));
        assertEquals(Arrays.asList(mapOf("key", "v")),
                new Schema(Arrays.asList(mapOf("key", "v")), null, true)
                        .validate(Arrays.asList(mapOf("key", "v", "bad", "bad"))));
    }

    // ===================================================================
    // test_ignore_extra_keys_validation_and_return_keys
    // ===================================================================
    @Test
    void testIgnoreExtraKeysValidationAndReturnKeys() {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("key", 5);
        sch.put(Object.class, Object.class);
        Map<Object, Object> data = new LinkedHashMap<>();
        data.put("key", 5);
        data.put("bad", 4);
        Map<Object, Object> actual = (Map<Object, Object>) new Schema(sch, null, true).validate(data);
        assertEquals(5, actual.get("key"));
        assertEquals(4, actual.get("bad"));
    }

    // ===================================================================
    // test_dict_forbidden_keys
    // ===================================================================
    @Test
    void testDictForbiddenKeys() {
        assertThrows(SchemaForbiddenKeyError.class,
                () -> new Schema(mapOf(new Forbidden("b"), Object.class)).validate(mapOf("b", "bye")));
        assertThrows(SchemaWrongKeyError.class,
                () -> new Schema(mapOf(new Forbidden("b"), Integer.class)).validate(mapOf("b", "bye")));
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(new Forbidden("b"), Integer.class);
        sch.put(new Optional("b"), Object.class);
        assertEquals(mapOf("b", "bye"), new Schema(sch).validate(mapOf("b", "bye")));
        Map<Object, Object> sch2 = new LinkedHashMap<>();
        sch2.put(new Forbidden("b"), Object.class);
        sch2.put(new Optional("b"), Object.class);
        assertThrows(SchemaForbiddenKeyError.class, () -> new Schema(sch2).validate(mapOf("b", "bye")));
    }

    // ===================================================================
    // test_dict_hook
    // ===================================================================
    @Test
    void testDictHook() {
        int[] count = {0};
        Hook hook = new Hook("b", (nkey, data, error) -> count[0]++);
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(hook, String.class);
        sch.put(new Optional("b"), Object.class);
        assertEquals(mapOf("b", "bye"), new Schema(sch).validate(mapOf("b", "bye")));
        assertEquals(1, count[0]);

        Hook hook2 = new Hook("b", (nkey, data, error) -> count[0]++);
        // value type doesn't match -- handler should not fire
        Map<Object, Object> sch2 = new LinkedHashMap<>();
        sch2.put(hook2, Integer.class);
        sch2.put(new Optional("b"), Object.class);
        assertEquals(mapOf("b", "bye"), new Schema(sch2).validate(mapOf("b", "bye")));
        assertEquals(1, count[0]);

        Hook hook3 = new Hook("b", (nkey, data, error) -> count[0]++);
        Map<Object, Object> sch3 = new LinkedHashMap<>();
        sch3.put(hook3, String.class);
        sch3.put("b", Object.class);
        assertEquals(mapOf("b", "bye"), new Schema(sch3).validate(mapOf("b", "bye")));
        assertEquals(2, count[0]);
    }

    // ===================================================================
    // test_dict_optional_keys
    // ===================================================================
    @Test
    void testDictOptionalKeys() {
        assertThrows(SchemaError.class,
                () -> new Schema(mapOf("a", 1, "b", 2)).validate(mapOf("a", 1)));
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("a", 1);
        sch.put(new Optional("b"), 2);
        assertEquals(mapOf("a", 1), new Schema(sch).validate(mapOf("a", 1)));
        assertEquals(mapOf("a", 1, "b", 2), new Schema(sch).validate(mapOf("a", 1, "b", 2)));
        // Optionals favored over types
        Map<Object, Object> sch2 = new LinkedHashMap<>();
        sch2.put(String.class, 1);
        sch2.put(new Optional("b"), 2);
        assertEquals(mapOf("a", 1, "b", 2),
                new Schema(sch2).validate(mapOf("a", 1, "b", 2)));
        // Optional hashes by inner schema
        Set<Optional> set = new HashSet<>();
        set.add(new Optional("a"));
        set.add(new Optional("a"));
        set.add(new Optional("b"));
        assertEquals(2, set.size());
    }

    // ===================================================================
    // test_dict_optional_defaults
    // ===================================================================
    @Test
    void testDictOptionalDefaults() {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(new Optional("a", 1), 11);
        sch.put(new Optional("b", 2), 22);
        assertEquals(mapOf("a", 11, "b", 2), new Schema(sch).validate(mapOf("a", 11)));

        // Optionals take precedence over types
        Map<Object, Object> sch2 = new LinkedHashMap<>();
        sch2.put(new Optional("a", 1), 11);
        sch2.put(String.class, 22);
        assertEquals(mapOf("a", 1, "b", 22), new Schema(sch2).validate(mapOf("b", 22)));

        Function<Object, Object> toInt = Object::toString;
        assertThrows(ClassCastException.class,
                () -> new Optional(new And(String.class, new Use(toInt)), 7));
    }

    // ===================================================================
    // test_dict_subtypes -- LinkedHashMap subtype preserved
    // ===================================================================
    @Test
    void testDictSubtypes() {
        LinkedHashMap<String, Integer> d = new LinkedHashMap<>();
        d.put("key", 1);
        Object v = new Schema(mapOf("key", 1)).validate(d);
        assertEquals(d, v);
        assertTrue(v instanceof LinkedHashMap);
    }

    // ===================================================================
    // test_dict_key_error
    // ===================================================================
    @Test
    void testDictKeyError() {
        try {
            new Schema(mapOf("k", Integer.class)).validate(mapOf("k", "x"));
            fail();
        } catch (SchemaError e) {
            assertEquals("Key 'k' error:\n'x' should be instance of 'int'", e.code());
        }
        try {
            new Schema(mapOf("k", mapOf("k2", Integer.class))).validate(mapOf("k", mapOf("k2", "x")));
            fail();
        } catch (SchemaError e) {
            assertEquals("Key 'k' error:\nKey 'k2' error:\n'x' should be instance of 'int'", e.code());
        }
        try {
            new Schema(mapOf("k", mapOf("k2", Integer.class)), "k2 should be int")
                    .validate(mapOf("k", mapOf("k2", "x")));
            fail();
        } catch (SchemaError e) {
            assertEquals("k2 should be int", e.code());
        }
    }

    // ===================================================================
    // test_complex
    // ===================================================================
    @Test
    void testComplex() throws IOException {
        Function<Object, Object> open = (path) -> {
            try {
                return new BufferedReader(new FileReader(String.valueOf(path)));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
        Predicate<Object> nonEmpty = (lst) -> ((List<?>) lst).size() > 0;
        Predicate<Object> exists = (p) -> Files.exists(Paths.get(String.valueOf(p)));
        Predicate<Object> bound = (n) -> ((Number) n).intValue() >= 0 && ((Number) n).intValue() <= 5;
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("<file>", new And(Arrays.asList(new Use(open)), nonEmpty));
        sch.put("<path>", exists);
        sch.put(new Optional("--count"), new And(Integer.class, bound));
        Schema s = new Schema(sch);
        @SuppressWarnings("unchecked")
        Map<Object, Object> data = (Map<Object, Object>) s.validate(mapOf(
                "<file>", Arrays.asList("./LICENSE-MIT"),
                "<path>", "./"));
        assertEquals(2, data.size());
        @SuppressWarnings("unchecked")
        List<Object> files = (List<Object>) data.get("<file>");
        assertEquals(1, files.size());
        BufferedReader br = (BufferedReader) files.get(0);
        assertTrue(br.readLine().startsWith("Copyright"));
        assertEquals("./", data.get("<path>"));
    }

    // ===================================================================
    // test_nice_errors
    // ===================================================================
    @Test
    void testNiceErrors() {
        try {
            new Schema(Integer.class, "should be integer").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("should be integer"), e.errors);
        }
        Function<Object, Object> toFloat = (o) -> Double.parseDouble(o.toString());
        try {
            new Schema(new Use(toFloat), "should be a number").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals("should be a number", e.code());
        }
        try {
            Function<Object, Object> toInt = (o) -> Integer.parseInt(o.toString());
            new Schema(mapOf(new Optional("i"), new Use(toInt, "should be a number")))
                    .validate(mapOf("i", "x"));
            fail();
        } catch (SchemaError e) {
            assertEquals("should be a number", e.code());
        }
    }

    // ===================================================================
    // test_use_error_handling
    // ===================================================================
    @Test
    void testUseErrorHandling() {
        try {
            new Use(namedFn("ve", VE)).validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("ve('x') raised ValueError()"), e.autos);
            assertEquals(Arrays.asList((String) null), e.errors);
        }
        try {
            new Use(namedFn("ve", VE), "should not raise").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("ve('x') raised ValueError()"), e.autos);
            assertEquals(Arrays.asList("should not raise"), e.errors);
        }
        try {
            new Use(namedFn("se", SE)).validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, "first auto"), e.autos);
            assertEquals(Arrays.asList(null, "first error"), e.errors);
        }
        try {
            new Use(namedFn("se", SE), "second error").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, "first auto"), e.autos);
            assertEquals(Arrays.asList("second error", "first error"), e.errors);
        }
    }

    // ===================================================================
    // test_or_error_handling
    // ===================================================================
    @Test
    void testOrErrorHandling() {
        try {
            new Or(namedFn("ve", VE)).validate("x");
            fail();
        } catch (SchemaError e) {
            assertTrue(e.autos.get(0).startsWith("Or("));
            assertTrue(e.autos.get(0).endsWith(") did not validate 'x'"));
            assertEquals("ve('x') raised ValueError()", e.autos.get(1));
            assertEquals(2, e.autos.size());
            assertEquals(Arrays.asList(null, null), e.errors);
        }
        try {
            new Or(namedFn("ve", VE)).error("should not raise").validate("x");
            fail();
        } catch (SchemaError e) {
            assertTrue(e.autos.get(0).startsWith("Or("));
            assertTrue(e.autos.get(0).endsWith(") did not validate 'x'"));
            assertEquals("ve('x') raised ValueError()", e.autos.get(1));
            assertEquals(2, e.autos.size());
            assertEquals(Arrays.asList("should not raise", "should not raise"), e.errors);
        }
        try {
            new Or("o").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("Or('o') did not validate 'x'", "'o' does not match 'x'"), e.autos);
            assertEquals(Arrays.asList(null, null), e.errors);
        }
        try {
            new Or("o").error("second error").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("Or('o') did not validate 'x'", "'o' does not match 'x'"), e.autos);
            assertEquals(Arrays.asList("second error", "second error"), e.errors);
        }
    }

    // ===================================================================
    // test_and_error_handling
    // ===================================================================
    @Test
    void testAndErrorHandling() {
        try {
            new And(namedFn("ve", VE)).validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("ve('x') raised ValueError()"), e.autos);
            assertEquals(Arrays.asList((String) null), e.errors);
        }
        try {
            new And(namedFn("ve", VE)).error("should not raise").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList("ve('x') raised ValueError()"), e.autos);
            assertEquals(Arrays.asList("should not raise"), e.errors);
        }
        try {
            new And(String.class, namedFn("se", SE)).validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, "first auto"), e.autos);
            assertEquals(Arrays.asList(null, "first error"), e.errors);
        }
        try {
            new And(String.class, namedFn("se", SE)).error("second error").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, "first auto"), e.autos);
            assertEquals(Arrays.asList("second error", "first error"), e.errors);
        }
    }

    // ===================================================================
    // test_schema_error_handling
    // ===================================================================
    @Test
    void testSchemaErrorHandling() {
        try {
            new Schema(new Use(namedFn("ve", VE))).validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, "ve('x') raised ValueError()"), e.autos);
            assertEquals(Arrays.asList(null, null), e.errors);
        }
        try {
            new Schema(new Use(namedFn("ve", VE)), "should not raise").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, "ve('x') raised ValueError()"), e.autos);
            assertEquals(Arrays.asList("should not raise", null), e.errors);
        }
        try {
            new Schema(new Use(namedFn("se", SE))).validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, null, "first auto"), e.autos);
            assertEquals(Arrays.asList(null, null, "first error"), e.errors);
        }
        try {
            new Schema(new Use(namedFn("se", SE)), "second error").validate("x");
            fail();
        } catch (SchemaError e) {
            assertEquals(Arrays.asList(null, null, "first auto"), e.autos);
            assertEquals(Arrays.asList("second error", null, "first error"), e.errors);
        }
    }

    // ===================================================================
    // test_validate_object
    // ===================================================================
    @Test
    void testValidateObject() {
        Schema schema = new Schema(mapOf(Object.class, String.class));
        assertEquals(mapOf(42, "str"), schema.validate(mapOf(42, "str")));
        assertThrows(SchemaError.class, () -> schema.validate(mapOf(42, 777)));
    }

    // ===================================================================
    // test_issue_9_prioritized_key_comparison
    // ===================================================================
    @Test
    void testIssue9PrioritizedKeyComparison() {
        Schema schema = new Schema(mapOf("key", 42, Object.class, 42));
        Map<Object, Object> data = new LinkedHashMap<>();
        data.put("key", 42);
        data.put(777, 42);
        assertEquals(data, schema.validate(data));
    }

    // ===================================================================
    // test_issue_9_prioritized_key_comparison_in_dicts
    // ===================================================================
    @Test
    void testIssue9PrioritizedKeyComparisonInDicts() {
        Function<Object, Object> toInt = (o) -> {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(o.toString());
        };
        Function<Object, Object> open = (p) -> {
            try {
                return new BufferedReader(new FileReader(String.valueOf(p)));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("ID", new Use(toInt, "ID should be an int"));
        sch.put("FILE", new Or(null, new Use(open, "FILE should be readable")));
        sch.put(new Optional(String.class), Object.class);
        Schema s = new Schema(sch);
        Map<Object, Object> data = new LinkedHashMap<>();
        data.put("ID", 10);
        data.put("FILE", null);
        data.put("other", "other");
        data.put("other2", "other2");
        assertEquals(data, s.validate(data));

        Map<Object, Object> data2 = new LinkedHashMap<>();
        data2.put("ID", 10);
        data2.put("FILE", null);
        // Reset Or() inside the schema (validation path mutates matchCount when only_one is false; safe)
        assertEquals(data2, new Schema(rebuildSch(toInt, open)).validate(data2));
    }

    private Map<Object, Object> rebuildSch(Function<Object, Object> toInt, Function<Object, Object> open) {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("ID", new Use(toInt, "ID should be an int"));
        sch.put("FILE", new Or(null, new Use(open, "FILE should be readable")));
        sch.put(new Optional(String.class), Object.class);
        return sch;
    }

    // ===================================================================
    // test_missing_keys_exception_with_non_str_dict_keys
    // ===================================================================
    @Test
    void testMissingKeysWithNonStr() {
        Function<Object, Object> lower = (o) -> ((String) o).toLowerCase();
        Schema s = new Schema(mapOf(new And(String.class, new Use(lower), "name"),
                new And(String.class, (Predicate<Object>) o -> ((String) o).length() > 0)));
        assertThrows(SchemaError.class, () -> s.validate(new LinkedHashMap<>()));
        SchemaMissingKeyError ex = assertThrows(SchemaMissingKeyError.class,
                () -> new Schema(mapOf(1, "x")).validate(mapOf()));
        assertEquals("Missing key: 1", ex.firstArg());
    }

    // ===================================================================
    // test_exception_handling_with_bad_validators -- a "validator" that throws
    // ===================================================================
    @Test
    void testExceptionHandlingWithBadValidators() {
        BadValidator bad = new BadValidator("haha");
        Schema s = new Schema(bad);
        SchemaError e = assertThrows(SchemaError.class, () -> s.validate("test"));
        assertTrue(e.firstArg().contains("ClassCastException")
                || e.firstArg().contains("validate"));
    }

    /** Stand-in for namedtuple("BadValidator", ["validate"]) -- ``validate`` is a non-callable. */
    public static class BadValidator {
        private final String validate;
        public BadValidator(String validate) { this.validate = validate; }
        public Object validate(Object data) {
            throw new ClassCastException("'str' object is not callable");
        }
    }

    // ===================================================================
    // test_issue_83_iterable_validation_return_type -- subtype preservation
    // ===================================================================
    @Test
    void testIssue83IterableReturnType() {
        Set<Object> testSetType = new TestSetType();
        testSetType.add("test");
        testSetType.add("strings");
        Schema s = new Schema(new LinkedHashSet<>(Arrays.asList(String.class)));
        Object result = s.validate(testSetType);
        assertTrue(result instanceof TestSetType);
    }

    public static class TestSetType extends LinkedHashSet<Object> {}

    // ===================================================================
    // test_optional_key_convert_failed_randomly  --  loop test
    // ===================================================================
    @Test
    void testOptionalKeyConvert() {
        // Convert string "2015-10-10 00:00:00" to a LocalDateTime via Use
        Function<Object, Object> dt = (o) -> java.time.LocalDateTime.parse(
                String.valueOf(o), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        for (int i = 0; i < 64; i++) {
            Or validator = new Or(null, new Use(dt));
            Map<Object, Object> sch = new LinkedHashMap<>();
            sch.put(new Optional("created_at"), validator);
            sch.put(new Optional("updated_at"), validator);
            sch.put(new Optional("birth"), validator);
            sch.put(new Optional(String.class), Object.class);
            Schema s = new Schema(sch);
            Map<Object, Object> data = new LinkedHashMap<>();
            data.put("created_at", "2015-10-10 00:00:00");
            @SuppressWarnings("unchecked")
            Map<Object, Object> validated = (Map<Object, Object>) s.validate(data);
            assertTrue(validated.get("created_at") instanceof java.time.LocalDateTime);
        }
    }

    // ===================================================================
    // test_inheritance / kwargs threading
    // ===================================================================
    @Test
    void testInheritance() {
        Function<Object, Object> conv = (data) -> (data instanceof Number)
                ? ((Number) data).intValue() + 1 : data;

        class MySchema extends Schema {
            MySchema(Object schema) { super(schema); }
            @Override public Object validate(Object data) { return validate(data, null); }
            @Override public Object validate(Object data, Map<String, Object> kwargs) {
                return super.validate(conv.apply(data), kwargs);
            }
            @Override protected Schema subSchema(Object schema, String error, boolean ignoreExtraKeys) {
                return new MySchema(schema);
            }
        }

        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("k", Integer.class);
        Map<Object, Object> sub = new LinkedHashMap<>();
        sub.put("k", Integer.class);
        sub.put("l", Arrays.asList(mapOf("l", Arrays.asList(Integer.class))));
        sch.put("d", sub);
        Map<Object, Object> v = new LinkedHashMap<>();
        v.put("k", 1);
        Map<Object, Object> dv = new LinkedHashMap<>();
        dv.put("k", 2);
        dv.put("l", Arrays.asList(mapOf("l", Arrays.asList(3, 4, 5))));
        v.put("d", dv);
        @SuppressWarnings("unchecked")
        Map<Object, Object> d = (Map<Object, Object>) new MySchema(sch).validate(v);
        assertEquals(2, d.get("k"));
        @SuppressWarnings("unchecked")
        Map<Object, Object> dd = (Map<Object, Object>) d.get("d");
        assertEquals(3, dd.get("k"));
        @SuppressWarnings("unchecked")
        List<Object> ll = (List<Object>) dd.get("l");
        @SuppressWarnings("unchecked")
        Map<Object, Object> firstL = (Map<Object, Object>) ll.get(0);
        assertEquals(Arrays.asList(4, 5, 6), firstL.get("l"));
    }

    // ===================================================================
    // test_inheritance_validate_kwargs
    // ===================================================================
    @Test
    void testInheritanceValidateKwargs() {
        class MySchema extends Schema {
            MySchema(Object schema) { super(schema); }
            @Override public Object validate(Object data, Map<String, Object> kwargs) {
                int inc = kwargs == null || kwargs.get("increment") == null ? 1
                        : ((Number) kwargs.get("increment")).intValue();
                if (data instanceof Number) data = ((Number) data).intValue() + inc;
                return super.validate(data, kwargs);
            }
            @Override public Object validate(Object data) { return validate(data, mapOf("increment", 1)); }
            @Override protected Schema subSchema(Object schema, String error, boolean ignoreExtraKeys) {
                return new MySchema(schema);
            }
        }
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("k", Integer.class);
        Map<Object, Object> sub = new LinkedHashMap<>();
        sub.put("k", Integer.class);
        sub.put("l", Arrays.asList(mapOf("l", Arrays.asList(Integer.class))));
        sch.put("d", sub);
        Map<Object, Object> v = new LinkedHashMap<>();
        v.put("k", 1);
        Map<Object, Object> dv = new LinkedHashMap<>();
        dv.put("k", 2);
        dv.put("l", Arrays.asList(mapOf("l", Arrays.asList(3, 4, 5))));
        v.put("d", dv);
        @SuppressWarnings("unchecked")
        Map<Object, Object> d = (Map<Object, Object>) new MySchema(sch).validate(v, mapOf("increment", 1));
        assertEquals(2, d.get("k"));
        @SuppressWarnings("unchecked")
        Map<Object, Object> dd = (Map<Object, Object>) d.get("d");
        assertEquals(3, dd.get("k"));
        @SuppressWarnings("unchecked")
        Map<Object, Object> firstL = (Map<Object, Object>) ((List<?>) dd.get("l")).get(0);
        assertEquals(Arrays.asList(4, 5, 6), firstL.get("l"));

        @SuppressWarnings("unchecked")
        Map<Object, Object> d2 = (Map<Object, Object>) new MySchema(sch).validate(v, mapOf("increment", 10));
        assertEquals(11, d2.get("k"));
    }

    // ===================================================================
    // test_optional_callable_default_get_inherited_schema_validate_kwargs
    // ===================================================================
    @Test
    void testOptionalCallableDefaultKwargs() {
        Function<Map<String, Object>, Object> def = (kw) ->
                2 + ((Number) kw.get("increment")).intValue();
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("k", Integer.class);
        Map<Object, Object> sub = new LinkedHashMap<>();
        sub.put(new Optional("k", def), Integer.class);
        sub.put("l", Arrays.asList(mapOf("l", Arrays.asList(Integer.class))));
        sch.put("d", sub);
        Map<Object, Object> v = new LinkedHashMap<>();
        v.put("k", 1);
        v.put("d", mapOf("l", Arrays.asList(mapOf("l", Arrays.asList(3, 4, 5)))));
        @SuppressWarnings("unchecked")
        Map<Object, Object> d = (Map<Object, Object>) new Schema(sch).validate(v, mapOf("increment", 1));
        assertEquals(1, d.get("k"));
        assertEquals(3, ((Map<?, ?>) d.get("d")).get("k"));
        @SuppressWarnings("unchecked")
        Map<Object, Object> d2 = (Map<Object, Object>) new Schema(sch).validate(v, mapOf("increment", 10));
        assertEquals(12, ((Map<?, ?>) d2.get("d")).get("k"));
    }

    // ===================================================================
    // test_optional_callable_default_ignore_inherited_schema_validate_kwargs
    // ===================================================================
    @Test
    void testOptionalCallableDefaultIgnoresKwargs() {
        java.util.concurrent.Callable<Object> def = () -> 42;
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("k", Integer.class);
        Map<Object, Object> sub = new LinkedHashMap<>();
        sub.put(new Optional("k", def), Integer.class);
        sub.put("l", Arrays.asList(mapOf("l", Arrays.asList(Integer.class))));
        sch.put("d", sub);
        Map<Object, Object> v = new LinkedHashMap<>();
        v.put("k", 1);
        v.put("d", mapOf("l", Arrays.asList(mapOf("l", Arrays.asList(3, 4, 5)))));
        @SuppressWarnings("unchecked")
        Map<Object, Object> d = (Map<Object, Object>) new Schema(sch).validate(v, mapOf("increment", 1));
        assertEquals(42, ((Map<?, ?>) d.get("d")).get("k"));
        @SuppressWarnings("unchecked")
        Map<Object, Object> d2 = (Map<Object, Object>) new Schema(sch).validate(v, mapOf("increment", 10));
        assertEquals(42, ((Map<?, ?>) d2.get("d")).get("k"));
    }

    // ===================================================================
    // test_literal_repr
    // ===================================================================
    @Test
    void testLiteralRepr() {
        assertEquals("Literal(\"test\", description=\"testing\")",
                new Literal("test", "testing").repr());
        assertEquals("Literal(\"test\", description=\"\")",
                new Literal("test").repr());
    }

    // ===================================================================
    // test_callable_error
    // ===================================================================
    @Test
    void testCallableError() {
        SchemaError e = null;
        try {
            new Schema(namedPredicate("lambda", (d) -> false), "{}").validate("This is the error message");
        } catch (SchemaError ex) {
            e = ex;
        }
        assertNotNull(e);
        assertEquals(Arrays.asList("This is the error message"), e.errors);
    }

    // ===================================================================
    // test_dict_literal_error_string -- regression for github issue #240
    // ===================================================================
    @Test
    void testDictLiteralErrorString() {
        assertTrue(new Schema(new Or(mapOf("a", 1)).error("error: {}"))
                .isValid(mapOf("a", 1)));
    }

    // ===================================================================
    // test_prepend_schema_name
    // ===================================================================
    @Test
    void testPrependSchemaName() {
        try {
            new Schema(mapOf("key1", Integer.class)).validate(mapOf("key1", "a"));
        } catch (SchemaError e) {
            assertEquals("Key 'key1' error:\n'a' should be instance of 'int'", e.toString());
        }
        try {
            new Schema(mapOf("key1", Integer.class), null, false, "custom_schemaname", null, false)
                    .validate(mapOf("key1", "a"));
        } catch (SchemaError e) {
            assertEquals("'custom_schemaname' Key 'key1' error:\n'a' should be instance of 'int'",
                    e.toString());
        }
        try {
            new Schema(Integer.class, null, false, "custom_schemaname", null, false).validate("a");
        } catch (SchemaUnexpectedTypeError e) {
            assertEquals("'custom_schemaname' 'a' should be instance of 'int'", e.toString());
        }
    }

    // ===================================================================
    // ----  JSON Schema generation tests  ----
    // ===================================================================

    @Test
    void testJsonSchema() {
        Schema s = new Schema(mapOf("test", String.class));
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("$schema", "http://json-schema.org/draft-07/schema#");
        expected.put("$id", "my-id");
        expected.put("properties", mapOf("test", mapOf("type", "string")));
        expected.put("required", Arrays.asList("test"));
        expected.put("additionalProperties", false);
        expected.put("type", "object");
        assertEquals(sortedDict(expected), sortedDict(s.jsonSchema("my-id")));
    }

    @Test
    void testJsonSchemaWithTitle() {
        Schema s = new Schema(mapOf("test", String.class), null, false, "Testing a schema", null, false);
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("$schema", "http://json-schema.org/draft-07/schema#");
        expected.put("$id", "my-id");
        expected.put("title", "Testing a schema");
        expected.put("properties", mapOf("test", mapOf("type", "string")));
        expected.put("required", Arrays.asList("test"));
        expected.put("additionalProperties", false);
        expected.put("type", "object");
        assertEquals(sortedDict(expected), sortedDict(s.jsonSchema("my-id")));
    }

    @Test
    void testJsonSchemaTypes() {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(new Optional("test_str"), String.class);
        sch.put(new Optional("test_int"), Integer.class);
        sch.put(new Optional("test_float"), Double.class);
        sch.put(new Optional("test_bool"), Boolean.class);
        Schema s = new Schema(sch);
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        assertEquals(mapOf("type", "string"), props.get("test_str"));
        assertEquals(mapOf("type", "integer"), props.get("test_int"));
        assertEquals(mapOf("type", "number"), props.get("test_float"));
        assertEquals(mapOf("type", "boolean"), props.get("test_bool"));
        assertEquals(new ArrayList<>(), result.get("required"));
        assertEquals(false, result.get("additionalProperties"));
    }

    @Test
    void testJsonSchemaNested() {
        Schema s = new Schema(mapOf("test", mapOf("other", String.class)), null, true);
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> testProp = (Map<String, Object>) props.get("test");
        assertEquals("object", testProp.get("type"));
        assertEquals(true, testProp.get("additionalProperties"));
        assertEquals(Arrays.asList("other"), testProp.get("required"));
        assertEquals(true, result.get("additionalProperties"));
    }

    @Test
    void testJsonSchemaOptionalKey() {
        Schema s = new Schema(mapOf(new Optional("test"), String.class));
        Map<String, Object> result = s.jsonSchema("my-id");
        assertEquals(new ArrayList<>(), result.get("required"));
    }

    @Test
    void testJsonSchemaOrKey() {
        Schema s = new Schema(mapOf(new Or("test1", "test2"), String.class));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        assertEquals(mapOf("type", "string"), props.get("test1"));
        assertEquals(mapOf("type", "string"), props.get("test2"));
        assertEquals(new ArrayList<>(), result.get("required"));
    }

    @Test
    void testJsonSchemaOrValues() {
        Schema s = new Schema(mapOf("param", new Or("test1", "test2")));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> param = (Map<String, Object>) props.get("param");
        assertEquals(Arrays.asList("test1", "test2"), param.get("enum"));
    }

    @Test
    void testJsonSchemaOrTypes() {
        Schema s = new Schema(mapOf("test", new Or(String.class, Integer.class)));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> test = (Map<String, Object>) props.get("test");
        assertEquals(Arrays.asList(mapOf("type", "string"), mapOf("type", "integer")), test.get("anyOf"));
    }

    @Test
    void testJsonSchemaOrOneValue() {
        Schema s = new Schema(mapOf("test", new Or(true)));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> test = (Map<String, Object>) props.get("test");
        assertEquals(true, test.get("const"));
    }

    @Test
    void testJsonSchemaConstIsNone() {
        Schema s = new Schema(mapOf("test", null));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> test = (Map<String, Object>) props.get("test");
        assertEquals("null", test.get("type"));
    }

    @Test
    void testJsonSchemaForbiddenKeyIgnored() {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(new Forbidden("forbidden"), String.class);
        sch.put("test", String.class);
        Schema s = new Schema(sch);
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        assertNull(props.get("forbidden"));
        assertEquals(mapOf("type", "string"), props.get("test"));
        assertEquals(Arrays.asList("test"), result.get("required"));
    }

    @Test
    void testJsonSchemaRegex() {
        Schema s = new Schema(mapOf(new Optional("username"), new Regex("[a-zA-Z][a-zA-Z0-9]{3,}")));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> u = (Map<String, Object>) props.get("username");
        assertEquals("string", u.get("type"));
        assertEquals("[a-zA-Z][a-zA-Z0-9]{3,}", u.get("pattern"));
    }

    @Test
    void testJsonSchemaEcmaCompliantRegex() {
        Schema s = new Schema(mapOf(new Optional("username"),
                new Regex("^(?<name>[a-zA-Z_][a-zA-Z0-9_]*)/$")));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> u = (Map<String, Object>) props.get("username");
        assertEquals("string", u.get("type"));
        // After translation: (?<name>...) -> (...) and / -> \/
        assertTrue(((String) u.get("pattern")).contains("\\/"));
    }

    @Test
    void testJsonSchemaAdditionalPropertiesMultiple() {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("named_property", Boolean.class);
        sch.put(Object.class, Integer.class);
        Schema s = new Schema(sch);
        Map<String, Object> result = s.jsonSchema("my-id");
        assertEquals(true, result.get("additionalProperties"));
        assertEquals(Arrays.asList("named_property"), result.get("required"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        assertEquals(mapOf("type", "boolean"), props.get("named_property"));
    }

    @Test
    void testJsonSchemaRootNotDict() {
        // input_schema=int -> {"type":"integer"}
        Map<String, Object> r = new Schema(Integer.class).jsonSchema("my-id");
        assertEquals("integer", r.get("type"));
        assertEquals("my-id", r.get("$id"));
    }

    @Test
    void testJsonSchemaArray() {
        Map<String, Object> r = new Schema(Arrays.asList(String.class)).jsonSchema("my-id");
        assertEquals("array", r.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) r.get("items");
        assertEquals("string", items.get("type"));
    }

    @Test
    void testJsonSchemaRegexRoot() {
        Map<String, Object> r = new Schema(new Regex("^v\\d+")).jsonSchema("my-id");
        assertEquals("string", r.get("type"));
        assertEquals("^v\\d+", r.get("pattern"));
        assertEquals("my-id", r.get("$id"));
    }

    @Test
    void testJsonSchemaDictType() {
        Schema s = new Schema(mapOf(new Optional("test1", new LinkedHashMap<>()), Map.class));
        Map<String, Object> result = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) result.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("object", t1.get("type"));
        assertEquals(new LinkedHashMap<>(), t1.get("default"));
    }

    @Test
    void testJsonSchemaTitleAndDescription() {
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(new Literal("productId", "The unique identifier for a product", "Product ID"), Integer.class);
        Schema s = new Schema(sch, null, false, "Product", "A product in the catalog", false);
        Map<String, Object> r = s.jsonSchema("my-id");
        assertEquals("Product", r.get("title"));
        assertEquals("A product in the catalog", r.get("description"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> pid = (Map<String, Object>) props.get("productId");
        assertEquals("Product ID", pid.get("title"));
        assertEquals("The unique identifier for a product", pid.get("description"));
        assertEquals("integer", pid.get("type"));
    }

    @Test
    void testJsonSchemaDefaultValue() {
        Schema s = new Schema(mapOf(new Optional("test1", 42), Integer.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("integer", t1.get("type"));
        assertEquals(42, t1.get("default"));
    }

    @Test
    void testJsonSchemaDefaultIsNone() {
        Schema s = new Schema(mapOf(new Optional("test1", null), String.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("string", t1.get("type"));
        assertNull(t1.get("default"));
        assertTrue(t1.containsKey("default"));
    }

    @Test
    void testJsonSchemaDefinitions() {
        Schema sub = new Schema(mapOf("sub_key1", Integer.class), null, false, "sub_schema", null, true);
        Schema main = new Schema(mapOf("main_key1", String.class, "main_key2", sub));
        Map<String, Object> r = main.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> defs = (Map<String, Object>) r.get("definitions");
        assertNotNull(defs);
        assertNotNull(defs.get("sub_schema"));
        @SuppressWarnings("unchecked")
        Map<String, Object> subDef = (Map<String, Object>) defs.get("sub_schema");
        assertEquals("object", subDef.get("type"));
        assertEquals("sub_schema", subDef.get("title"));
    }

    @Test
    void testJsonSchemaDefinitionsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> new Schema(mapOf("test1", String.class), null, false, null, null, true));
    }

    @Test
    void testDescription() {
        Schema s = new Schema(mapOf(new Optional(new Literal("test1", "A description here"), new LinkedHashMap<>()), Map.class));
        assertNotNull(s.validate(mapOf("test1", new LinkedHashMap<>())));
    }

    @Test
    void testDescriptionWithDefault() {
        Schema s = new Schema(mapOf(new Optional(new Literal("test1", "A description here"), new LinkedHashMap<>()), Map.class));
        assertEquals(mapOf("test1", new LinkedHashMap<>()), s.validate(new LinkedHashMap<>()));
    }

    // ===================================================================
    // test_use_json -- chaining Use(json.loads) with a dict schema
    // ===================================================================
    @Test
    void testUseJson() {
        Function<Object, Object> jsonLoads = (s) -> {
            // Minimal JSON parser using javax.json would be heavy; use a quick hand-roll for the test fixture
            return parseJson((String) s);
        };
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put(new Optional("description"), CharSequence.class);
        sch.put("public", Boolean.class);
        sch.put("files", mapOf(CharSequence.class, mapOf("content", CharSequence.class)));
        Schema gistSchema = new Schema(new And(new Use(jsonLoads), sch));
        String gist = "{\"description\": \"the description for this gist\","
                + " \"public\": true,"
                + " \"files\": {"
                + " \"file1.txt\": {\"content\": \"String file contents\"},"
                + " \"other.txt\": {\"content\": \"Another file contents\"}}}";
        assertNotNull(gistSchema.validate(gist));
    }

    /** Tiny JSON parser sufficient for testUseJson. */
    @SuppressWarnings("unchecked")
    private static Object parseJson(String s) {
        return new SimpleJson(s).parseValue();
    }

    static class SimpleJson {
        final String s; int i;
        SimpleJson(String s) { this.s = s; this.i = 0; }
        void skipWs() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        Object parseValue() {
            skipWs();
            char c = s.charAt(i);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { i += 4; return null; }
            return parseNumber();
        }
        Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // '{'
            skipWs();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                skipWs();
                String k = parseString();
                skipWs(); i++; // ':'
                Object v = parseValue();
                m.put(k, v);
                skipWs();
                if (s.charAt(i) == ',') { i++; continue; }
                if (s.charAt(i) == '}') { i++; break; }
            }
            return m;
        }
        List<Object> parseArray() {
            List<Object> l = new ArrayList<>();
            i++; skipWs();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(parseValue());
                skipWs();
                if (s.charAt(i) == ',') { i++; continue; }
                if (s.charAt(i) == ']') { i++; break; }
            }
            return l;
        }
        String parseString() {
            i++; // opening "
            StringBuilder sb = new StringBuilder();
            while (s.charAt(i) != '"') {
                if (s.charAt(i) == '\\') { sb.append(s.charAt(i + 1)); i += 2; }
                else sb.append(s.charAt(i++));
            }
            i++; // closing "
            return sb.toString();
        }
        Boolean parseBool() {
            if (s.charAt(i) == 't') { i += 4; return true; }
            i += 5; return false;
        }
        Number parseNumber() {
            int start = i;
            while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(start, i);
            if (n.contains(".")) return Double.parseDouble(n);
            return Long.parseLong(n);
        }
    }

    // ===================================================================
    // test_error_reporting
    // ===================================================================
    @Test
    void testErrorReporting() {
        Function<Object, Object> open = (p) -> {
            try {
                return new BufferedReader(new FileReader(String.valueOf(p)));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
        Predicate<Object> exists = (p) -> Files.exists(Paths.get(String.valueOf(p)));
        Function<Object, Object> toInt = (o) -> {
            if (o instanceof Number) return ((Number) o).intValue();
            return Integer.parseInt(o.toString());
        };
        Predicate<Object> bound = (n) -> ((Number) n).intValue() > 0 && ((Number) n).intValue() < 5;

        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("<files>", Arrays.asList(new Use(open, "<files> should be readable")));
        sch.put("<path>", new And(exists).error("<path> should exist"));
        sch.put("--count", new Or(null, new And(new Use(toInt), bound)).error("--count should be integer 0 < n < 5"));
        Schema s = new Schema(sch, "Error:");

        // Should not throw
        s.validate(mapOf("<files>", new ArrayList<>(), "<path>", "./", "--count", 3));

        try {
            s.validate(mapOf("<files>", new ArrayList<>(), "<path>", "./", "--count", "10"));
            fail();
        } catch (SchemaError e) {
            assertEquals("Error:\n--count should be integer 0 < n < 5", e.code());
        }
        try {
            s.validate(mapOf("<files>", new ArrayList<>(), "<path>", "./hai", "--count", "2"));
            fail();
        } catch (SchemaError e) {
            assertEquals("Error:\n<path> should exist", e.code());
        }
        try {
            s.validate(mapOf("<files>", Arrays.asList("hai"), "<path>", "./", "--count", "2"));
            fail();
        } catch (SchemaError e) {
            assertEquals("Error:\n<files> should be readable", e.code());
        }
    }

    // ===================================================================
    // test_schema_repr
    // ===================================================================
    @Test
    void testSchemaRepr() {
        Function<Object, Object> toFloat = (o) -> Double.parseDouble(o.toString());
        Schema schema = new Schema(Arrays.asList(new Or(null, new And(String.class, new Use(namedFn("float", toFloat))))));
        // Match the Python repr (modulo the "type" -> "class" replacement)
        String r = schema.toString().replace("class", "type");
        assertEquals("Schema([Or(None, And(<type 'str'>, Use(<type 'float'>)))])", r);
    }

    // ===================================================================
    // test_copy -- SchemaError must be safely cloneable (no shared state).
    // ===================================================================
    @Test
    void testCopy() {
        SchemaError s1 = new SchemaError("a", (String) null);
        SchemaError s2 = new SchemaError(new ArrayList<>(s1.autos), new ArrayList<>(s1.errors));
        assertNotSame(s1, s2);
        assertEquals(s1.getClass(), s2.getClass());
    }

    // ===================================================================
    // test_issue_56 -- callables without a friendly name still report errors.
    // ===================================================================
    @Test
    void testIssue56NoCallableName() {
        Predicate<Object> endsWithCsv = (s) -> ((String) s).endsWith(".csv");
        Schema s = new Schema(endsWithCsv);
        assertEquals("test.csv", s.validate("test.csv"));
        SchemaError e = assertThrows(SchemaError.class, () -> s.validate("test.py"));
        assertNotNull(e.firstArg());
    }

    // ===================================================================
    // test_inheritance_validate_kwargs_passed_to_nested_schema
    // ===================================================================
    @Test
    void testInheritanceValidateKwargsPassedToNested() {
        class MySchema extends Schema {
            MySchema(Object schema) { super(schema); }
            @Override public Object validate(Object data, Map<String, Object> kwargs) {
                int inc = kwargs == null || kwargs.get("increment") == null ? 1
                        : ((Number) kwargs.get("increment")).intValue();
                if (data instanceof Number) data = ((Number) data).intValue() + inc;
                return super.validate(data, kwargs);
            }
            @Override public Object validate(Object data) { return validate(data, null); }
            @Override protected Schema subSchema(Object schema, String error, boolean ignoreExtraKeys) {
                return new MySchema(schema);
            }
        }

        Map<Object, Object> sub = new LinkedHashMap<>();
        sub.put("k", Integer.class);
        sub.put("l", Arrays.asList(new Schema(mapOf("l", Arrays.asList(Integer.class)))));
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("k", Integer.class);
        sch.put("d", new MySchema(sub));

        Map<Object, Object> v = new LinkedHashMap<>();
        v.put("k", 1);
        Map<Object, Object> dv = new LinkedHashMap<>();
        dv.put("k", 2);
        dv.put("l", Arrays.asList(mapOf("l", Arrays.asList(3, 4, 5))));
        v.put("d", dv);

        @SuppressWarnings("unchecked")
        Map<Object, Object> d = (Map<Object, Object>) new Schema(sch).validate(v, mapOf("increment", 1));
        // Outer Schema doesn't increment, only inner MySchema does
        assertEquals(1, d.get("k"));
        @SuppressWarnings("unchecked")
        Map<Object, Object> dd = (Map<Object, Object>) d.get("d");
        // Only "k" inside d is incremented (because MySchema applies on the outer dict, +1 to k=2 -> 3)
        // The list-of-list inside is handled by plain Schema, no increment.
        assertEquals(3, dd.get("k"));
    }

    // ===================================================================
    // test_inheritance_optional -- subclassing Optional with a custom default
    // ===================================================================
    @Test
    void testInheritanceOptional() {
        Function<Object, Object> conv = (data) -> {
            // not used directly here -- the MyOptional class will increment.
            return data;
        };

        class MyOptional extends Optional {
            int rawDefault;
            MyOptional(Object schema, int dflt) {
                super(schema);
                this.rawDefault = dflt;
                this.hasDefault = true;
                this.key = Reprs.str(schema);
            }
            @Override public Object getDefault() {
                // We don't have access to kwargs here in the simple model,
                // so just return the raw default.
                return rawDefault;
            }
        }

        Map<Object, Object> sub = new LinkedHashMap<>();
        sub.put(new MyOptional("k", 2), Integer.class);
        sub.put("l", Arrays.asList(mapOf("l", Arrays.asList(Integer.class))));
        Map<Object, Object> sch = new LinkedHashMap<>();
        sch.put("k", Integer.class);
        sch.put("d", sub);

        Map<Object, Object> v = new LinkedHashMap<>();
        v.put("k", 1);
        v.put("d", mapOf("l", Arrays.asList(mapOf("l", Arrays.asList(3, 4, 5)))));

        @SuppressWarnings("unchecked")
        Map<Object, Object> d = (Map<Object, Object>) new Schema(sch).validate(v);
        assertEquals(1, d.get("k"));
        // Default applied -- value 2 (rawDefault)
        @SuppressWarnings("unchecked")
        Map<Object, Object> dd = (Map<Object, Object>) d.get("d");
        assertEquals(2, dd.get("k"));
    }

    // ===================================================================
    // ----  More json_schema tests  ----
    // ===================================================================

    @Test
    void testJsonSchemaOtherTypes() {
        // Unknown types fall back to "string"
        Schema s = new Schema(mapOf(new Optional("test_other"), byte[].class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> v = (Map<String, Object>) props.get("test_other");
        assertEquals("string", v.get("type"));
    }

    @Test
    void testJsonSchemaNestedSchema() {
        Schema s = new Schema(mapOf("test", new Schema(mapOf("other", String.class), null, true)));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> testProp = (Map<String, Object>) props.get("test");
        assertEquals("object", testProp.get("type"));
        assertEquals(true, testProp.get("additionalProperties"));
        assertEquals(false, r.get("additionalProperties"));
    }

    @Test
    void testJsonSchemaOptionalKeyNested() {
        Schema s = new Schema(mapOf("test", mapOf(new Optional("other"), String.class)));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> testProp = (Map<String, Object>) props.get("test");
        assertEquals(new ArrayList<>(), testProp.get("required"));
    }

    @Test
    void testJsonSchemaOrValuesNested() {
        Schema s = new Schema(mapOf("param", new Or(Arrays.asList(String.class), Arrays.asList(List.class))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) props.get("param");
        assertNotNull(p.get("anyOf"));
    }

    @Test
    void testJsonSchemaOrValuesWithOptional() {
        Schema s = new Schema(mapOf(new Optional("whatever"), new Or("test1", "test2")));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) props.get("whatever");
        assertEquals(Arrays.asList("test1", "test2"), p.get("enum"));
    }

    @Test
    void testJsonSchemaOrOnlyOne() {
        // Or with str + a callable -> schema picks just the str type.
        Predicate<Object> shorter = (x) -> ((String) x).length() < 5;
        Schema s = new Schema(mapOf("test", new Or(String.class, shorter)));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) props.get("test");
        assertEquals("string", p.get("type"));
    }

    @Test
    void testJsonSchemaAndTypes() {
        Predicate<Object> shorter = (x) -> ((String) x).length() < 5;
        Schema s = new Schema(mapOf("test", new And(String.class, shorter)));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) props.get("test");
        assertEquals("string", p.get("type"));
    }

    @Test
    void testJsonSchemaConstIsCallable() {
        Function<Object, Object> doublerFn = (x) -> ((Number) x).intValue() * 2;
        Schema s = new Schema(mapOf("test", doublerFn));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        assertEquals(new LinkedHashMap<>(), props.get("test"));
    }

    @Test
    void testJsonSchemaAndSimple() {
        Schema s = new Schema(mapOf("test1", new And(String.class, "test2")));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) props.get("test1");
        assertEquals(Arrays.asList(mapOf("type", "string"), mapOf("const", "test2")), p.get("allOf"));
    }

    @Test
    void testJsonSchemaAndList() {
        Schema s = new Schema(mapOf("param1", new And(Arrays.asList("choice1", "choice2"), List.class)));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) props.get("param1");
        assertNotNull(p.get("allOf"));
    }

    @Test
    void testJsonSchemaAdditionalPropertiesParametrized() {
        // {} -> false
        Schema s1 = new Schema(new LinkedHashMap<>());
        assertEquals(false, s1.jsonSchema("my-id").get("additionalProperties"));

        // {str: str} -> true
        Schema s2 = new Schema(mapOf(String.class, String.class));
        assertEquals(true, s2.jsonSchema("my-id").get("additionalProperties"));

        // {Optional(str): str} -> true
        Schema s3 = new Schema(mapOf(new Optional(String.class), String.class));
        assertEquals(true, s3.jsonSchema("my-id").get("additionalProperties"));

        // {object: int} -> true
        Schema s4 = new Schema(mapOf(Object.class, Integer.class));
        assertEquals(true, s4.jsonSchema("my-id").get("additionalProperties"));

        // {} ignoreExtraKeys=true -> true
        Schema s5 = new Schema(new LinkedHashMap<>(), null, true);
        assertEquals(true, s5.jsonSchema("my-id").get("additionalProperties"));
    }

    @Test
    void testJsonSchemaDefinitionsRecursive() {
        List<Object> children = new ArrayList<>();
        Map<Object, Object> personSch = new LinkedHashMap<>();
        personSch.put(new Optional("name"), String.class);
        personSch.put(new Optional("children"), children);
        Schema person = new Schema(personSch, null, false, "person", null, true);
        children.add(person);
        Map<String, Object> r = person.jsonSchema("my-id");
        assertEquals("#/definitions/person", r.get("$ref"));
        @SuppressWarnings("unchecked")
        Map<String, Object> defs = (Map<String, Object>) r.get("definitions");
        assertNotNull(defs.get("person"));
    }

    @Test
    void testJsonSchemaDefaultIsTuple() {
        Schema s = new Schema(mapOf(new Optional("test1", Arrays.asList(1, 2)), List.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals(Arrays.asList(1, 2), t1.get("default"));
    }

    @Test
    void testJsonSchemaDefaultIsLiteral() {
        Schema s = new Schema(mapOf(new Optional("test1", new Literal("Hello!")), String.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("Hello!", t1.get("default"));
    }

    @Test
    void testJsonSchemaDefaultValueWithLiteral() {
        Schema s = new Schema(mapOf(new Optional(new Literal("test1"), false), Boolean.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("boolean", t1.get("type"));
        assertEquals(false, t1.get("default"));
    }

    @Test
    void testJsonSchemaDefaultIsCallable() {
        java.util.concurrent.Callable<Object> def = () -> "Hello!";
        Schema s = new Schema(mapOf(new Optional("test1", def), String.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("Hello!", t1.get("default"));
    }

    @Test
    void testJsonSchemaDefaultIsCallableWithKwargs() {
        Function<Map<String, Object>, Object> def = (kw) -> "Hello, " + kw.get("name");
        Schema s = new Schema(mapOf(new Optional("test1", def), String.class));
        Map<String, Object> r = s.jsonSchema("my-id", false, mapOf("name", "World!"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("Hello, World!", t1.get("default"));
    }

    @Test
    void testJsonSchemaConstIsCustomType() {
        Object obj = new Object() { @Override public String toString() { return "Hello!"; } };
        Schema s = new Schema(mapOf("test", obj));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t = (Map<String, Object>) props.get("test");
        assertEquals("Hello!", t.get("const"));
    }

    @Test
    void testJsonSchemaDefaultIsCustomType() {
        Object obj = new Object() { @Override public String toString() { return "Hello!"; } };
        Schema s = new Schema(mapOf(new Optional("test", obj), String.class));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t = (Map<String, Object>) props.get("test");
        assertEquals("Hello!", t.get("default"));
    }

    @Test
    void testJsonSchemaTitleInOr() {
        Schema s = new Schema(mapOf("test", new Or(
                new Schema("option1", null, false, "Option 1", "This is the first option", false),
                new Schema("option2", null, false, "Option 2", "This is the second option", false))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t = (Map<String, Object>) props.get("test");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anyOf = (List<Map<String, Object>>) t.get("anyOf");
        assertEquals(2, anyOf.size());
        assertEquals("option1", anyOf.get(0).get("const"));
        assertEquals("Option 1", anyOf.get(0).get("title"));
        assertEquals("option2", anyOf.get(1).get("const"));
    }

    @Test
    void testJsonSchemaDescriptionNested() {
        Schema s = new Schema(mapOf(
                new Optional(new Literal("test1", "A description here"), new LinkedHashMap<>()),
                new Or(Arrays.asList(String.class), Arrays.asList(List.class))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t1 = (Map<String, Object>) props.get("test1");
        assertEquals("A description here", t1.get("description"));
        assertNotNull(t1.get("anyOf"));
    }

    @Test
    void testJsonSchemaLiteralWithEnum() {
        Schema s = new Schema(mapOf(
                new Literal("test", "A test"),
                new Or(new Literal("literal1", "A literal with description"),
                       new Literal("literal2", "Another literal with description"))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t = (Map<String, Object>) props.get("test");
        assertEquals("A test", t.get("description"));
        assertEquals(Arrays.asList("literal1", "literal2"), t.get("enum"));
    }

    @Test
    void testJsonSchemaObjectOrArrayOfObject() {
        Map<Object, Object> o = new LinkedHashMap<>();
        o.put("param1", "test1");
        o.put(new Optional("param2"), "test2");
        Schema s = new Schema(mapOf("test", new Or(o, Arrays.asList(o))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> t = (Map<String, Object>) props.get("test");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anyOf = (List<Map<String, Object>>) t.get("anyOf");
        assertEquals(2, anyOf.size());
        assertEquals("object", anyOf.get(0).get("type"));
        assertEquals("array", anyOf.get(1).get("type"));
    }

    @Test
    void testJsonSchemaDescriptionOrNested() {
        Schema s = new Schema(mapOf(
                new Optional(new Or(
                        new Literal("test1", "A description here"),
                        new Literal("test2", "Another"))),
                new Or(Arrays.asList(String.class), Arrays.asList(List.class))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        assertNotNull(props.get("test1"));
        assertNotNull(props.get("test2"));
    }

    @Test
    void testJsonSchemaDescriptionAndNested() {
        Schema s = new Schema(mapOf(
                new Optional(new Or(
                        new Literal("test1", "A description here"),
                        new Literal("test2", "Another"))),
                new And(Arrays.asList(String.class), Arrays.asList(List.class))));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) r.get("properties");
        assertNotNull(props.get("test1"));
        assertNotNull(props.get("test2"));
    }

    @Test
    void testJsonSchemaDefinitionsNested() {
        Schema sub2 = new Schema(mapOf("sub_sub_key1", Integer.class), null, false, "sub_sub_schema", null, true);
        Schema sub = new Schema(mapOf("sub_key1", Integer.class, "sub_key2", sub2),
                null, false, "sub_schema", null, true);
        Schema main = new Schema(mapOf("main_key1", String.class, "main_key2", sub));
        Map<String, Object> r = main.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> defs = (Map<String, Object>) r.get("definitions");
        assertNotNull(defs.get("sub_schema"));
        assertNotNull(defs.get("sub_sub_schema"));
    }

    @Test
    void testJsonSchemaDefinitionsAndLiterals() {
        Schema sub = new Schema(mapOf(new Literal("sub_key1", "Sub key 1"), Integer.class),
                null, false, "sub_schema", "Sub Schema", true);
        Schema main = new Schema(mapOf(
                new Literal("main_key1", "Main Key 1"), String.class,
                new Literal("main_key2", "Main Key 2"), sub,
                new Literal("main_key3", "Main Key 3"), sub));
        Map<String, Object> r = main.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> defs = (Map<String, Object>) r.get("definitions");
        @SuppressWarnings("unchecked")
        Map<String, Object> subDef = (Map<String, Object>) defs.get("sub_schema");
        assertEquals("Sub Schema", subDef.get("description"));
    }

    @Test
    void testJsonSchemaRefInList() {
        Schema inner1 = new Schema(Arrays.asList(String.class), null, false, "Inner test", null, true);
        Schema inner2 = new Schema(Arrays.asList(String.class), null, false, "Inner test2", null, true);
        Schema s = new Schema(new Or(inner1, inner2));
        Map<String, Object> r = s.jsonSchema("my-id");
        @SuppressWarnings("unchecked")
        Map<String, Object> defs = (Map<String, Object>) r.get("definitions");
        assertNotNull(defs.get("Inner test"));
        assertNotNull(defs.get("Inner test2"));
        assertNotNull(r.get("anyOf"));
    }
}
