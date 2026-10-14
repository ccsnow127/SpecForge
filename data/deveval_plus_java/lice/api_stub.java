// API Stub -- class/method signatures only, no implementation.
// You MUST use these exact class names, field names, and method signatures.

import java.io.*;
import java.util.*;

public class Lice {

    // ---------------------------------------------------------------
    // Public constants
    // ---------------------------------------------------------------

    /** Sorted list of available license names (parsed at class init from packaged templates). */
    public static final List<String> LICENSES = null;

    /** Default license (matches Python: "bsd3"). */
    public static final String DEFAULT_LICENSE = "bsd3";

    /** Map of file-extension suffix -> comment-style key (mirrors Python LANGS dict). */
    public static final Map<String, String> LANGS = null;

    /** Map of comment-style key -> [open, line, close] strings (mirrors Python LANG_CMT dict). */
    public static final Map<String, String[]> LANG_CMT = null;

    // ---------------------------------------------------------------
    // Helper type: parsed CLI arguments
    // ---------------------------------------------------------------
    public static class Args {
        public String license;
        public boolean header = false;
        public String organization;
        public String project;
        public String templatePath;
        public String year;
        public String language;
        public String ofile = "stdout";
        public boolean listVars = false;
        public boolean listLicenses = false;
        public boolean listLanguages = false;
    }

    /** Thrown when --year argument is not a four-digit year. */
    public static class ArgumentTypeException extends RuntimeException {
        public ArgumentTypeException(String msg) { super(msg); }
    }

    // ---------------------------------------------------------------
    // clean_path
    // ---------------------------------------------------------------
    /**
     * Clean a path by expanding user home (~), environment variables ($VAR),
     * and ensuring the path is absolute.
     */
    public static String cleanPath(String p) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // get_context
    // ---------------------------------------------------------------
    /**
     * Build the substitution context map from parsed CLI args.
     * Returns a map with keys: year, organization, project.
     */
    public static Map<String, String> getContext(Args args) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // guess_organization
    // ---------------------------------------------------------------
    /**
     * Guess the organization from `git config --get user.name`. If that fails,
     * fall back to the value of the USER environment variable.
     */
    public static String guessOrganization() { /* ... */ return null; }

    // ---------------------------------------------------------------
    // load_file_template
    // ---------------------------------------------------------------
    /**
     * Load a template from the given filesystem path. Path is cleaned first.
     *
     * @throws IllegalArgumentException if the path does not exist
     *         (message format: "path does not exist: <path>")
     */
    public static StringBuilder loadFileTemplate(String path) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // load_template
    // ---------------------------------------------------------------
    /**
     * Load the packaged license template by short name. If {@code header} is
     * true, load the matching header template instead.
     *
     * @throws IOException if the template resource cannot be found
     */
    public static StringBuilder loadTemplate(String license, boolean header) throws IOException { /* ... */ return null; }

    /** Convenience overload, header=false. */
    public static StringBuilder loadTemplate(String license) throws IOException { /* ... */ return null; }

    // ---------------------------------------------------------------
    // extract_vars
    // ---------------------------------------------------------------
    /**
     * Extract template variables wrapped in <code>{{ name }}</code> from the
     * given template buffer.  Returns a sorted list of unique variable names.
     */
    public static List<String> extractVars(StringBuilder template) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // generate_license
    // ---------------------------------------------------------------
    /**
     * Replace each <code>{{ key }}</code> in the template with its value from
     * {@code context}.  After this call, the template buffer is cleared.
     *
     * @throws IllegalArgumentException if a variable is missing from the context
     *         (message format: "<key> is missing from the template context")
     */
    public static StringBuilder generateLicense(StringBuilder template, Map<String, String> context) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // format_license
    // ---------------------------------------------------------------
    /**
     * Wrap the rendered license text in source-code comments for the given
     * language extension. If {@code lang} is null or empty, "txt" is used.
     */
    public static StringBuilder formatLicense(StringBuilder template, String lang) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // get_suffix
    // ---------------------------------------------------------------
    /**
     * Return the recognised file-extension suffix for the given filename, or
     * {@code null} if the filename has no extension or an unknown extension.
     * (Mirrors Python's <code>get_suffix</code>, which returns False on miss.)
     */
    public static String getSuffix(String name) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // valid_year
    // ---------------------------------------------------------------
    /**
     * Validate a four-digit year string.
     *
     * @throws ArgumentTypeException if the string is not exactly four digits
     */
    public static String validYear(String s) { /* ... */ return null; }

    // ---------------------------------------------------------------
    // main
    // ---------------------------------------------------------------
    /**
     * Java entry point mirroring Python {@code core.main()}.
     * Throws {@link SystemExitException} when the equivalent Python program
     * would call {@code sys.exit(code)}.
     */
    public static void runMain(String[] argv) { /* ... */ }

    public static void main(String[] argv) { /* ... */ }

    /** Carries an exit code so tests can assert exit semantics like Python's SystemExit. */
    public static class SystemExitException extends RuntimeException {
        public final int code;
        public SystemExitException(int code) { super("exit " + code); this.code = code; }
    }
}
