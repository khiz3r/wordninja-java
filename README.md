# WordNinja Java

A Java port of the Python [WordNinja](https://github.com/keredson/wordninja) library — splits concatenated text into words using dynamic programming and a Zipf-law cost model.

Extended with an **`isWord` API** that lets you detect whether a raw string (like a camelCase identifier or a URL slug) meaningfully resolves into real words, with control over minimum word count and minimum word length.

The bundled word list is a **394k-word merged corpus** built from:
- Google Trillion Word Corpus (top 10,000 — best frequency ordering)
- dolph/popular.txt (common English ∩ Wiktionary TV/movie corpus)
- ENABLE1 Scrabble dictionary (172k comprehensive words)
- dwyl/english-words (370k+ alpha words, catch-all coverage)
- Curated programming vocabulary (`javascript`, `typescript`, `async`, `await`, `webpack`, `eslint`, etc.)

---

## Requirements

- Java 11 or higher (JRE for running, JDK for building)

---

## Building

```bash
cd wordninja-java
bash build.sh
```

This produces `wordninja.jar` in the project root. The script auto-detects `javac` from your `PATH`, `JAVA_HOME`, or common install locations on Linux and macOS.

---

## Java API

### Get the singleton instance

```java
WordNinja wn = WordNinja.getInstance();
```

The word list is loaded once and reused. Thread-safe.

---

### `split(String s)` → `List<String>`

Splits a concatenated string into its constituent words. Preserves original casing.

```java
wn.split("thequickbrownfoxjumpsoverthelazydog");
// → [the, quick, brown, fox, jumps, over, the, lazy, dog]

wn.split("javascriptasyncawait");
// → [javascript, async, await]

wn.split("isThisgoodornot");
// → [is, This, good, or, not]

wn.split("derekanderson");
// → [derek, anderson]
```

---

### `isWord(String namedParams)` → `boolean`

Named-parameter style. Accepts a comma-separated `key=value` string.

**Parameters:**

| Key | Type | Description |
|---|---|---|
| `word` | String | The string to analyse |
| `total-words` | int ≥ 1 | Minimum number of qualifying words required to return `true` |
| `consider-word-length` | int ≥ 1 | A token must be at least this many characters long to count |

```java
// "isThisgoodornot" splits to [is, This, good, or, not]
// tokens with length >= 4: [This, good] → count 2 >= 2 → true
wn.isWord("word=isThisgoodornot,total-words=2,consider-word-length=4");
// → true

// "isThenameisval" splits to [is, The, name, is, val]
// tokens with length >= 4: [name] → count 1 < 2 → false
wn.isWord("word=isThenameisval,total-words=2,consider-word-length=4");
// → false
```

---

### `isWord(String word, int minWordCount, int minWordLength)` → `boolean`

Typed overload — same logic, direct parameters.

```java
wn.isWord("isThisgoodornot", 2, 4);  // → true
wn.isWord("isThenameisval",  2, 4);  // → false
wn.isWord("javascriptasyncawait", 2, 4); // → true  (javascript=10, async=5, await=5)
wn.isWord("isit", 1, 4);  // → false  (no token >= 4 chars)
wn.isWord("isit", 2, 2);  // → true   (is=2, it=2, both count)
```

**How qualifying works:**

A token qualifies if:
1. It is a known word in the dictionary, **and**
2. Its length is ≥ `minWordLength`

Short words like `the`, `has`, `is`, `or` are real words but will be ignored if they fall below the length threshold. This makes the check robust for identifier-style strings where short filler tokens are common.

---

### `contains(String word)` → `boolean`

Plain dictionary lookup — no splitting. Case-insensitive.

```java
wn.contains("javascript");   // → true
wn.contains("typescript");   // → true
wn.contains("webpack");      // → true
wn.contains("async");        // → true
wn.contains("xyzzyabc");     // → false
wn.contains("JavaScript");   // → true  (case-insensitive)
```

---

## CLI

The JAR includes a command-line interface. Useful for shell scripts and quick testing.

### `split`

```bash
java -jar wordninja.jar split "thequickbrownfox"
# → the quick brown fox

java -jar wordninja.jar split "javascriptasyncawait"
# → javascript async await
```

### `isWord` — named-param style

```bash
java -jar wordninja.jar isWord "word=isThisgoodornot,total-words=2,consider-word-length=4"
# stdout: true   exit code: 0

java -jar wordninja.jar isWord "word=isThenameisval,total-words=2,consider-word-length=4"
# stdout: false  exit code: 1
```

### `isWord` — positional style

```bash
java -jar wordninja.jar isWord <word> <total-words> <consider-word-length>

java -jar wordninja.jar isWord "isThisgoodornot" 2 4
# → true

java -jar wordninja.jar isWord "isThenameisval" 2 4
# → false
```

Exit code is `0` for `true` and `1` for `false`, so you can use it directly in shell conditionals:

```bash
if java -jar wordninja.jar isWord "someIdentifier" 2 4; then
  echo "Looks like real words"
fi
```

### `contains`

```bash
java -jar wordninja.jar contains "javascript"
# → true  (exit 0)

java -jar wordninja.jar contains "xyzzy"
# → false (exit 1)
```

---

## Project Structure

```
wordninja-java/
├── build.sh                                  ← compile & package script
├── pom.xml                                   ← Maven build (optional)
└── src/
    ├── main/
    │   ├── java/io/wordninja/
    │   │   ├── LanguageModel.java            ← DP splitting engine + cost model
    │   │   ├── WordNinja.java                ← public API (split, isWord, contains)
    │   │   └── WordNinjaCLI.java             ← CLI entry point
    │   └── resources/
    │       └── wordninja_words.txt.gz        ← bundled 394k word list
    └── test/
        └── java/io/wordninja/
            └── WordNinjaTest.java            ← JUnit 4 tests
```

---

## Word List Sources

| Source | Words | License |
|---|---|---|
| Google Trillion Word Corpus (via first20hours) | 10,000 | Public domain |
| dolph/dictionary popular.txt | ~25,000 | MIT |
| ENABLE1 Scrabble word list | ~172,000 | Public domain |
| dwyl/english-words | ~370,000 | MIT |
| Curated programming terms | ~40 | — |

Words are ordered by frequency (most frequent first), which is what the Zipf-law cost model depends on for accurate splitting.

---

## Credits

- Original Python WordNinja by [keredson](https://github.com/keredson/wordninja), based on the dynamic programming approach by [Generic Human on Stack Overflow](https://stackoverflow.com/a/11642687/2449774).
- Java port and `isWord` API added on top.
