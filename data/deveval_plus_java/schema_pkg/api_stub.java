// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.
//
// This is a Java port of the Python ``schema`` library.  All classes live in
// the default package so that tests can use ``new Schema(...)`` directly.

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * SchemaError -- raised on validation failure.
 *
 * Mirrors the Python class -- carries an "autos" list (auto-generated messages)
 * and an "errors" list (user-provided error strings).  ``code`` joins the
 * unique non-null entries of either list.
 */
class SchemaError extends RuntimeException {
    public List<String> autos;
    public List<String> errors;

    /** Construct from a single auto + single error (either may be null). */
    public SchemaError(String auto, String error) { super(); /* ... */ }

    /** Construct from a list of autos + a single error string. */
    public SchemaError(List<String> autos, String error) { super(); /* ... */ }

    /** Construct from a single auto + a list of errors. */
    public SchemaError(String auto, List<String> errors) { super(); /* ... */ }

    /** Construct from lists of autos and errors. */
    public SchemaError(List<String> autos, List<String> errors) { super(); /* ... */ }

    /** Returns code -- newline-joined unique non-null errors (or autos if errors empty). */
    public String code() { /* ... */ return null; }

    /** First positional argument (matches Python ``e.args[0]``). */
    public String firstArg() { /* ... */ return null; }

    @Override
    public String getMessage() { /* ... */ return null; }
}

/** Thrown when a required key is missing from a dict-shaped value. */
class SchemaMissingKeyError extends SchemaError {
    public SchemaMissingKeyError(String auto, String error) { super(auto, error); }
    public SchemaMissingKeyError(List<String> autos, List<String> errors) { super(autos, errors); }
}

/** Thrown when an unexpected/extra key is found in a dict-shaped value. */
class SchemaWrongKeyError extends SchemaError {
    public SchemaWrongKeyError(String auto, String error) { super(auto, error); }
    public SchemaWrongKeyError(List<String> autos, List<String> errors) { super(autos, errors); }
}

/** Thrown when a forbidden key is present and matches the forbidden value type. */
class SchemaForbiddenKeyError extends SchemaError {
    public SchemaForbiddenKeyError(String auto, String error) { super(auto, error); }
    public SchemaForbiddenKeyError(List<String> autos, List<String> errors) { super(autos, errors); }
}

/** Thrown when an Or(only_one=True) matches multiple keys. */
class SchemaOnlyOneAllowedError extends SchemaError {
    public SchemaOnlyOneAllowedError(String auto, String error) { super(auto, error); }
    public SchemaOnlyOneAllowedError(List<String> autos, List<String> errors) { super(autos, errors); }
}

/** Thrown when a value is not an instance of the expected type. */
class SchemaUnexpectedTypeError extends SchemaError {
    public SchemaUnexpectedTypeError(String auto, String error) { super(auto, error); }
    public SchemaUnexpectedTypeError(List<String> autos, List<String> errors) { super(autos, errors); }
}

/**
 * The main Schema entry point.  Wraps a "schema" object (which may be a type
 * Class, a literal value, a Map, a List/Set, a callable Predicate, a
 * Function, another validator with a ``.validate`` method, etc.) and exposes
 * ``validate(data)`` to verify (and optionally transform) input data.
 */
class Schema {
    /** Construct a Schema from any object. */
    public Schema(Object schema) { /* ... */ }
    /** With ``error`` -- error message used when validation fails. */
    public Schema(Object schema, String error) { /* ... */ }
    /** With ``ignoreExtraKeys`` -- when true, extra keys in dict data are kept. */
    public Schema(Object schema, String error, boolean ignoreExtraKeys) { /* ... */ }
    /** Full constructor mirroring the Python kwargs. */
    public Schema(Object schema, String error, boolean ignoreExtraKeys,
                  String name, String description, boolean asReference) { /* ... */ }

    /** Returns the underlying schema object. */
    public Object schema() { /* ... */ return null; }
    /** Returns the schema description (for json_schema generation). */
    public String description() { /* ... */ return null; }
    /** Returns the schema name (used as title in json_schema). */
    public String name() { /* ... */ return null; }
    /** Returns ignoreExtraKeys flag. */
    public boolean ignoreExtraKeys() { /* ... */ return false; }
    /** Returns asReference flag. */
    public boolean asReference() { /* ... */ return false; }

    /** Returns true if ``data`` validates without throwing. */
    public boolean isValid(Object data) { /* ... */ return false; }
    /** isValid with kwargs (for inheritance/Optional callable defaults). */
    public boolean isValid(Object data, Map<String, Object> kwargs) { /* ... */ return false; }

    /** Validate ``data`` -- throws SchemaError on failure, returns the validated value. */
    public Object validate(Object data) { /* ... */ return null; }
    /** validate with kwargs -- supports inheritance / callable defaults. */
    public Object validate(Object data, Map<String, Object> kwargs) { /* ... */ return null; }

    /** Generate a draft-07 JSON schema dict. */
    public Map<String, Object> jsonSchema(String schemaId) { /* ... */ return null; }
    /** json_schema with use_refs flag. */
    public Map<String, Object> jsonSchema(String schemaId, boolean useRefs) { /* ... */ return null; }
    /** json_schema with use_refs and kwargs (passed to callable defaults). */
    public Map<String, Object> jsonSchema(String schemaId, boolean useRefs, Map<String, Object> kwargs) { /* ... */ return null; }

    @Override public String toString() { /* ... */ return null; }
}

/** AND-combinator -- all sub-schemas must validate; data is threaded through. */
class And {
    /** Construct from arbitrary args. */
    public And(Object... args) { /* ... */ }
    /** Configure with a custom error message. */
    public And error(String error) { /* ... */ return null; }
    /** Configure with ignoreExtraKeys. */
    public And ignoreExtraKeys(boolean v) { /* ... */ return null; }
    /** Returns the underlying args list. */
    public Object[] args() { /* ... */ return null; }
    /** Validate. */
    public Object validate(Object data) { /* ... */ return null; }
    public Object validate(Object data, Map<String, Object> kwargs) { /* ... */ return null; }
    @Override public String toString() { /* ... */ return null; }
}

/** OR-combinator -- at least one sub-schema must validate. */
class Or extends And {
    public Or(Object... args) { super(args); }
    /** Configure as XOR (only_one=True). */
    public Or onlyOne(boolean v) { /* ... */ return null; }
    /** Reset match counter (called between dict-key validations). */
    public void reset() { /* ... */ }
    @Override public Object validate(Object data) { /* ... */ return null; }
    @Override public Object validate(Object data, Map<String, Object> kwargs) { /* ... */ return null; }
}

/** Use -- transforms data via a callable while validating. */
class Use {
    /** Construct from a transform Function. */
    public Use(Function<Object, Object> callable) { /* ... */ }
    /** With a custom error message. */
    public Use(Function<Object, Object> callable, String error) { /* ... */ }
    public Object validate(Object data) { /* ... */ return null; }
    public Object validate(Object data, Map<String, Object> kwargs) { /* ... */ return null; }
    @Override public String toString() { /* ... */ return null; }
}

/** Optional dict-key marker, with optional default value. */
class Optional extends Schema {
    public static final Object MARKER = new Object();
    public Object defaultValue;
    public boolean hasDefault;
    public String key;

    public Optional(Object schema) { super(schema); }
    public Optional(Object schema, Object defaultValue) { super(schema); /* ... */ }

    @Override public int hashCode() { /* ... */ return 0; }
    @Override public boolean equals(Object other) { /* ... */ return false; }
    public void reset() { /* ... */ }
}

/** Hook -- user-provided handler invoked when key+value match. */
class Hook extends Schema {
    public Object key;
    /** Handler called with (matched-key, full-data, error) when value matches. */
    public TriConsumer handler;

    /** Functional interface for the hook handler. */
    @FunctionalInterface
    public interface TriConsumer {
        void accept(Object nkey, Object data, String error);
    }

    public Hook(Object schema) { super(schema); }
    public Hook(Object schema, TriConsumer handler) { super(schema); /* ... */ }
}

/** Forbidden -- a hook that raises SchemaForbiddenKeyError on match. */
class Forbidden extends Hook {
    public Forbidden(Object schema) { super(schema); }
}

/** Literal -- carries a value plus optional title/description. */
class Literal {
    public Literal(Object value) { /* ... */ }
    public Literal(Object value, String description) { /* ... */ }
    public Literal(Object value, String description, String title) { /* ... */ }
    public Object schema() { /* ... */ return null; }
    public String title() { /* ... */ return null; }
    public String description() { /* ... */ return null; }
    @Override public String toString() { /* ... */ return null; }
}

/** Regex validator -- string must match the pattern via search semantics. */
class Regex {
    public Regex(String pattern) { /* ... */ }
    public Regex(String pattern, int flags) { /* ... */ }
    public Regex(String pattern, int flags, String error) { /* ... */ }
    /** Construct from a pre-compiled Pattern. */
    public Regex(Pattern pattern) { /* ... */ }
    public String patternStr() { /* ... */ return null; }
    public Object validate(Object data) { /* ... */ return null; }
    public Object validate(Object data, Map<String, Object> kwargs) { /* ... */ return null; }
    @Override public String toString() { /* ... */ return null; }
}

/** Const -- like Schema, but always returns the original (untransformed) data. */
class Const extends Schema {
    public Const(Object schema) { super(schema); }
    @Override public Object validate(Object data) { /* ... */ return null; }
    @Override public Object validate(Object data, Map<String, Object> kwargs) { /* ... */ return null; }
}
