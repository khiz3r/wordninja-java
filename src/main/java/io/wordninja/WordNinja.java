package io.wordninja;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * <h2>WordNinja</h2>
 *
 * <p>A Java port of the Python <a href="https://github.com/keredson/wordninja">WordNinja</a>
 * library, extended with an {@code isWord} API.
 *
 * <h3>Quick start</h3>
 * <pre>{@code
 * // Use the singleton backed by the bundled 126 k-word list
 * WordNinja wn = WordNinja.getInstance();
 *
 * // Split concatenated text into words
 * List<String> words = wn.split("isThisgoodornot");
 * // → ["is", "This", "good", "or", "not"]
 *
 * // Named-parameter style  (mirrors the spec)
 * boolean ok = wn.isWord("word=isThisgoodornot,total-words=2,consider-word-length=4");
 *
 * // Or call the typed overload directly
 * boolean ok2 = wn.isWord("isThisgoodornot", 2, 4);
 * }</pre>
 *
 * <h3>isWord semantics</h3>
 * <ul>
 *   <li><b>word</b> – the string to analyse (camelCase and lower-case both work).</li>
 *   <li><b>total-words</b> – how many qualifying words must be found for the call to
 *       return {@code true}.</li>
 *   <li><b>consider-word-length</b> – a token is only counted if its length is ≥ this
 *       value.  Tokens shorter than the threshold (like {@code "the"}, {@code "has"},
 *       {@code "had"}) are silently ignored.</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>
 *  isThisgoodornot  → split: [is, This, good, or, not]
 *                     qualifying (len≥4): [This, good]  → count 2  ≥ 2  → true
 *
 *  isThenameisval   → split: [is, The, name, is, val]
 *                     qualifying (len≥4): [name]         → count 1  &lt; 2  → false
 * </pre>
 */
public class WordNinja {

    // ---------------------------------------------------------------- singleton

    private static volatile WordNinja defaultInstance;

    /** Returns the singleton backed by the bundled word list. */
    public static WordNinja getInstance() {
        if (defaultInstance == null) {
            synchronized (WordNinja.class) {
                if (defaultInstance == null) {
                    defaultInstance = createDefault();
                }
            }
        }
        return defaultInstance;
    }

    private static WordNinja createDefault() {
        try (InputStream is = WordNinja.class.getResourceAsStream("/wordninja_words.txt.gz")) {
            if (is == null) throw new IllegalStateException(
                "Bundled word list 'wordninja_words.txt.gz' not found on classpath");
            return new WordNinja(new LanguageModel(is));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load bundled word list", e);
        }
    }

    // ---------------------------------------------------------------- instance

    private final LanguageModel model;

    /** Create a WordNinja instance backed by a custom {@link LanguageModel}. */
    public WordNinja(LanguageModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    // ------------------------------------------------------------------ split

    /**
     * Splits a concatenated string into its constituent words.
     *
     * <pre>{@code
     * split("heshotwhointhewhat") → ["he", "shot", "who", "in", "the", "what"]
     * }</pre>
     */
    public List<String> split(String s) {
        return model.split(s);
    }

    // ----------------------------------------------------------------- isWord

    // Matches:  key=value  pairs separated by commas (or semicolons)
    private static final Pattern KV_PATTERN =
        Pattern.compile("([\\w-]+)=([^,;]+)");

    /**
     * Named-parameter API – accepts a string of {@code key=value} pairs.
     *
     * <p>Recognised keys (all required):
     * <ul>
     *   <li>{@code word} – the string to test</li>
     *   <li>{@code total-words} – minimum qualifying-word count (integer ≥ 1)</li>
     *   <li>{@code consider-word-length} – minimum token length to count (integer ≥ 1)</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * wn.isWord("word=isThisgoodornot,total-words=2,consider-word-length=4")
     * }</pre>
     *
     * @throws IllegalArgumentException if any required key is missing or values are invalid
     */
    public boolean isWord(String namedParams) {
        Map<String, String> params = parseParams(namedParams);

        String word = require(params, "word");
        int totalWords = requireInt(params, "total-words");
        int considerLen = requireInt(params, "consider-word-length");

        return isWord(word, totalWords, considerLen);
    }

    /**
     * Typed API – directly specify the parameters.
     *
     * @param word               the string to test
     * @param minWordCount       minimum number of qualifying words
     * @param minWordLength      minimum character length for a token to qualify
     * @return {@code true} iff at least {@code minWordCount} tokens of length
     *         ≥ {@code minWordLength} are recognised words
     * @throws IllegalArgumentException if {@code minWordCount} or {@code minWordLength} &lt; 1
     */
    public boolean isWord(String word, int minWordCount, int minWordLength) {
        if (minWordCount < 1) throw new IllegalArgumentException("total-words must be ≥ 1");
        if (minWordLength < 1) throw new IllegalArgumentException("consider-word-length must be ≥ 1");
        Objects.requireNonNull(word, "word");
        return model.isWordCandidate(word, minWordCount, minWordLength);
    }

    // ---------------------------------------------- single-word dictionary check

    /**
     * Returns {@code true} if {@code word} appears verbatim (case-insensitive)
     * in the word list.  This is a plain dictionary lookup – no splitting is done.
     *
     * <pre>{@code
     * wn.contains("javascript")  // true
     * wn.contains("xyzzy")       // false
     * }</pre>
     */
    public boolean contains(String word) {
        return model.containsWord(word);
    }

    // ----------------------------------------------------------------- helpers

    private static Map<String, String> parseParams(String input) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = KV_PATTERN.matcher(input);
        while (m.find()) {
            map.put(m.group(1).trim(), m.group(2).trim());
        }
        return map;
    }

    private static String require(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null || v.isEmpty())
            throw new IllegalArgumentException("Missing required parameter: " + key);
        return v;
    }

    private static int requireInt(Map<String, String> params, String key) {
        try {
            return Integer.parseInt(require(params, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + key + "' must be an integer");
        }
    }
}
