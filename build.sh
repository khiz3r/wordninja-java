#!/usr/bin/env bash
set -e

# ── WordNinja build script ─────────────────────────────────────────
# Finds javac/jar automatically, compiles sources, bundles resources,
# and produces wordninja.jar in the project root.

echo "=== WordNinja Build ==="

# ── locate javac ──────────────────────────────────────────────────
if command -v javac &>/dev/null; then
    JAVAC=$(command -v javac)
    JAR_TOOL=$(dirname "$JAVAC")/jar
elif [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAR_TOOL="$JAVA_HOME/bin/jar"
else
    # Common JDK paths (Linux / macOS)
    for candidate in \
        /usr/lib/jvm/java-21-openjdk-amd64/bin/javac \
        /usr/lib/jvm/java-17-openjdk-amd64/bin/javac \
        /usr/lib/jvm/java-11-openjdk-amd64/bin/javac \
        /usr/local/opt/openjdk/bin/javac \
        /usr/local/opt/openjdk@21/bin/javac \
        /usr/local/opt/openjdk@17/bin/javac; do
        if [ -f "$candidate" ]; then
            JAVAC="$candidate"
            JAR_TOOL="$(dirname "$candidate")/jar"
            break
        fi
    done
fi

if [ -z "$JAVAC" ] || [ ! -f "$JAVAC" ]; then
    echo "ERROR: javac not found. Install a JDK (11+) or set JAVA_HOME."
    exit 1
fi

echo "  javac : $JAVAC"
echo "  jar   : $JAR_TOOL"
echo ""

# ── clean & compile ────────────────────────────────────────────────
rm -rf out
mkdir -p out/classes

echo "[1/4] Compiling sources..."
$JAVAC \
    src/main/java/io/wordninja/LanguageModel.java \
    src/main/java/io/wordninja/WordNinja.java \
    src/main/java/io/wordninja/WordNinjaCLI.java \
    -d out/classes

echo "[2/4] Copying resources..."
cp src/main/resources/wordninja_words.txt.gz out/classes/

echo "[3/4] Writing manifest..."
printf "Main-Class: io.wordninja.WordNinjaCLI\n\n" > out/MANIFEST.MF

echo "[4/4] Packaging JAR..."
$JAR_TOOL --create \
           --file wordninja.jar \
           --manifest out/MANIFEST.MF \
           -C out/classes .

echo ""
echo "✓ Build complete → wordninja.jar ($(du -h wordninja.jar | cut -f1))"
echo ""
echo "Quick test:"
echo "  java -jar wordninja.jar split \"thequickbrownfox\""
java -jar wordninja.jar split "thequickbrownfox" 2>/dev/null && true || \
    echo "  (run the above manually — java not on PATH used during build)"
