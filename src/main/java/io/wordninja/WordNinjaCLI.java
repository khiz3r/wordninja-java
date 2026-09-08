package io.wordninja;

import java.util.List;

/**
 * Command-line interface for WordNinja.
 *
 * <h3>Usage</h3>
 * <pre>
 *   # Split mode (default)
 *   java -jar wordninja.jar split "isThisgoodornot"
 *   → is This good or not
 *
 *   # isWord named-param style
 *   java -jar wordninja.jar isWord "word=isThisgoodornot,total-words=2,consider-word-length=4"
 *   → true
 *
 *   # isWord positional style
 *   java -jar wordninja.jar isWord "isThisgoodornot" 2 4
 *   → true
 *
 *   # Dictionary contains check
 *   java -jar wordninja.jar contains "javascript"
 *   → true
 * </pre>
 *
 * Exit code: 0 = success / true, 1 = false / error.
 */
public class WordNinjaCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }

        WordNinja wn = WordNinja.getInstance();
        String command = args[0].toLowerCase();

        try {
            switch (command) {

                case "split": {
                    if (args.length < 2) { System.err.println("Usage: split <input>"); System.exit(1); }
                    List<String> parts = wn.split(args[1]);
                    System.out.println(String.join(" ", parts));
                    break;
                }

                case "isword": {
                    boolean result;
                    if (args.length == 2) {
                        // Named-param string:  "word=...,total-words=2,consider-word-length=4"
                        result = wn.isWord(args[1]);
                    } else if (args.length == 4) {
                        // Positional:  <word> <total-words> <consider-word-length>
                        int totalWords = Integer.parseInt(args[2]);
                        int minLen     = Integer.parseInt(args[3]);
                        result = wn.isWord(args[1], totalWords, minLen);
                    } else {
                        System.err.println("Usage: isWord \"word=X,total-words=N,consider-word-length=M\"");
                        System.err.println("   or: isWord <word> <total-words> <consider-word-length>");
                        System.exit(1);
                        return;
                    }
                    System.out.println(result);
                    System.exit(result ? 0 : 1);
                    break;
                }

                case "contains": {
                    if (args.length < 2) { System.err.println("Usage: contains <word>"); System.exit(1); }
                    boolean result = wn.contains(args[1]);
                    System.out.println(result);
                    System.exit(result ? 0 : 1);
                    break;
                }

                default:
                    System.err.println("Unknown command: " + args[0]);
                    printHelp();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("WordNinja CLI");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  split    <input>");
        System.out.println("      Split concatenated text into words.");
        System.out.println();
        System.out.println("  isWord   \"word=<input>,total-words=<N>,consider-word-length=<M>\"");
        System.out.println("  isWord   <input> <N> <M>");
        System.out.println("      Returns true if at least N words of length >= M are found.");
        System.out.println();
        System.out.println("  contains <word>");
        System.out.println("      Returns true if word is in the dictionary (plain lookup).");
    }
}
