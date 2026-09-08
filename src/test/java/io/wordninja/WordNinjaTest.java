package io.wordninja;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class WordNinjaTest {

    private static WordNinja wn;

    @BeforeClass
    public static void setup() {
        wn = WordNinja.getInstance();
    }

    // ------------------------------------------------------------------ split

    @Test
    public void testSplitBasic() {
        List<String> parts = wn.split("derekanderson");
        Assert.assertTrue("should contain derek", parts.contains("derek"));
        Assert.assertTrue("should contain anderson", parts.contains("anderson"));
    }

    @Test
    public void testSplitFox() {
        List<String> parts = wn.split("thequickbrownfoxjumpsoverthelazydog");
        Assert.assertTrue(parts.contains("fox"));
        Assert.assertTrue(parts.contains("lazy"));
    }

    @Test
    public void testSplitCamel() {
        List<String> parts = wn.split("isThisgoodornot");
        System.out.println("split(isThisgoodornot) = " + parts);
        Assert.assertTrue(parts.size() >= 2);
    }

    // ----------------------------------------------------------------- isWord

    @Test
    public void testIsWordTrueExample() {
        // "isThisgoodornot" → tokens include "this" and "good" (len≥4), count=2 → true
        boolean result = wn.isWord("isThisgoodornot", 2, 4);
        System.out.println("isWord(isThisgoodornot, 2, 4) = " + result);
        Assert.assertTrue(result);
    }

    @Test
    public void testIsWordFalseExample() {
        // "isThenameisval" → only "name" qualifies (len≥4), count=1 < 2 → false
        boolean result = wn.isWord("isThenameisval", 2, 4);
        System.out.println("isWord(isThenameisval, 2, 4) = " + result);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsWordNamedParams() {
        boolean result = wn.isWord("word=isThisgoodornot,total-words=2,consider-word-length=4");
        Assert.assertTrue(result);
    }

    @Test
    public void testIsWordNamedParamsFalse() {
        boolean result = wn.isWord("word=isThenameisval,total-words=2,consider-word-length=4");
        Assert.assertFalse(result);
    }

    @Test
    public void testIsWordShortWordsIgnored() {
        // "isit" → tokens: ["is", "it"] both length < 4, so 0 qualify → false (with minLen=4)
        boolean result = wn.isWord("isit", 1, 4);
        Assert.assertFalse(result);
    }

    @Test
    public void testIsWordLowMinLen() {
        // With minLen=2, "is" and "it" both count
        boolean result = wn.isWord("isit", 2, 2);
        Assert.assertTrue(result);
    }

    @Test
    public void testIsWordTotalWordsOne() {
        // Just needs 1 word of len≥4 → "good" qualifies
        boolean result = wn.isWord("isgood", 1, 4);
        Assert.assertTrue(result);
    }

    // --------------------------------------------------------------- contains

    @Test
    public void testContainsJavascript() {
        Assert.assertTrue(wn.contains("javascript"));
    }

    @Test
    public void testContainsFunction() {
        Assert.assertTrue(wn.contains("function"));
    }

    @Test
    public void testContainsNonsense() {
        Assert.assertFalse(wn.contains("xyzzyabc"));
    }

    @Test
    public void testContainsCaseInsensitive() {
        Assert.assertTrue(wn.contains("JavaScript"));
        Assert.assertTrue(wn.contains("FUNCTION"));
    }
}
