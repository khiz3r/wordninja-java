package io.wordninja;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Port of the Python WordNinja LanguageModel.
 *
 * Uses a Zipf-law cost dictionary and dynamic programming
 * to split concatenated words with no spaces.
 */
public class LanguageModel {

    private final Map<String, Double> wordCost;
    private final int maxWordLen;

    /** Build from a gzipped word-list file (one word per line, sorted by frequency). */
    public LanguageModel(InputStream wordFileStream) throws IOException {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(wordFileStream), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) words.add(line);
            }
        }

        int n = words.size();
        double logN = Math.log(n);
        wordCost = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            wordCost.put(words.get(i), Math.log((i + 1) * logN));
        }

        maxWordLen = words.stream().mapToInt(String::length).max().orElse(24);
    }

    // ------------------------------------------------------------------ split

    /**
     * Split a string that has no spaces into its constituent words.
     * Preserves original casing; honours apostrophes and digit sequences
     * the same way the Python reference does.
     */
    public List<String> split(String s) {
        // Split on whitespace first (keep delimiters), process each chunk
        String[] parts = s.split("((?<=\\s)|(?=\\s))");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                result.add(part);
            } else {
                result.addAll(splitChunk(part));
            }
        }
        return result;
    }

    private List<String> splitChunk(String s) {
        int len = s.length();
        double[] cost = new double[len + 1];
        cost[0] = 0.0;

        for (int i = 1; i <= len; i++) {
            double[] bm = bestMatch(s, cost, i);
            cost[i] = bm[0];
        }

        // Backtrack
        Deque<String> out = new ArrayDeque<>();
        int i = len;
        while (i > 0) {
            double[] bm = bestMatch(s, cost, i);
            int k = (int) bm[1];
            String token = s.substring(i - k, i);

            boolean newToken = true;
            if (!token.equals("'")) {
                if (!out.isEmpty()) {
                    String last = out.peekFirst();
                    if (last.equals("'s") ||
                        (s.charAt(i - 1) >= '0' && s.charAt(i - 1) <= '9'
                            && !last.isEmpty() && last.charAt(0) >= '0' && last.charAt(0) <= '9')) {
                        out.pollFirst();
                        out.addFirst(token + last);
                        newToken = false;
                    }
                }
            }
            if (newToken) out.addFirst(token);
            i -= k;
        }

        return new ArrayList<>(out);
    }

    private double[] bestMatch(String s, double[] cost, int i) {
        int start = Math.max(0, i - maxWordLen);
        double minCost = Double.MAX_VALUE;
        int bestK = 1;

        for (int k = 0; k < i - start; k++) {
            String candidate = s.substring(i - k - 1, i).toLowerCase(Locale.ROOT);
            double c = cost[i - k - 1] + wordCost.getOrDefault(candidate, 9e18);
            if (c < minCost) {
                minCost = c;
                bestK = k + 1;
            }
        }
        return new double[]{minCost, bestK};
    }

    // ---------------------------------------------------------------- isWord

    /**
     * Returns {@code true} if {@code word} exists verbatim (case-insensitive)
     * in the word-cost dictionary.
     */
    public boolean containsWord(String word) {
        return wordCost.containsKey(word.toLowerCase(Locale.ROOT));
    }

    /**
     * Core logic for {@link WordNinja#isWord(String, int, int)}.
     *
     * <p>Splits the input string, then counts how many of the resulting tokens
     * satisfy <em>both</em> conditions:
     * <ul>
     *   <li>the token is recognised as a known word, and</li>
     *   <li>the token length is ≥ {@code minWordLength}.</li>
     * </ul>
     * Returns {@code true} iff that count is ≥ {@code minWordCount}.
     *
     * @param s             concatenated string to test
     * @param minWordCount  minimum number of qualifying words required
     * @param minWordLength minimum character length a token must have to count
     */
    public boolean isWordCandidate(String s, int minWordCount, int minWordLength) {
        List<String> tokens = split(s);
        int qualifying = 0;
        for (String token : tokens) {
            if (token.length() >= minWordLength && containsWord(token)) {
                qualifying++;
                if (qualifying >= minWordCount) return true;
            }
        }
        return false;
    }

    /** Package-private: exposed for tests. */
    Map<String, Double> getWordCost() {
        return Collections.unmodifiableMap(wordCost);
    }
}
