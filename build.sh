#!/bin/bash
# Build script for KcEffects plugin
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
SRC_DIR="$PROJECT_DIR/src/main/java"
RES_DIR="$PROJECT_DIR/src/main/resources"

LOPHINE_BASE="/home/xcgeek/lophine26.1.2"

# Build classpath from all library jars + server jar + CE jar
CP=""
for jar in $(find "$LOPHINE_BASE/libraries" -name "*.jar" 2>/dev/null); do
    CP="$CP:$jar"
done
CP="$CP:$LOPHINE_BASE/versions/26.1.2/lophine-26.1.2.jar"
CP="$CP:$LOPHINE_BASE/plugins/craft-engine-paper-plugin-26.7.4.jar"
# Remove leading colon
CP="${CP#:}"

# Find Java 25
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
JAVAC="$JAVA_HOME/bin/javac"
JAR="$JAVA_HOME/bin/jar"

if [ ! -x "$JAVAC" ]; then
    echo "ERROR: javac not found at $JAVAC"
    exit 1
fi

echo "=== Cleaning build ==="
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

echo "=== Compiling Java sources ==="
"$JAVAC" \
    --release 21 \
    -cp "$CP" \
    -d "$CLASSES_DIR" \
    $(find "$SRC_DIR" -name "*.java")

echo "=== Packing jar ==="
VERSION=$(grep '^version:' "$RES_DIR/plugin.yml" | awk '{print $2}')
JAR_NAME="KcEffects-${VERSION}.jar"

cd "$CLASSES_DIR"
"$JAR" cf "$BUILD_DIR/$JAR_NAME" .
cd "$PROJECT_DIR"

# Add plugin.yml to jar
"$JAR" uf "$BUILD_DIR/$JAR_NAME" -C "$RES_DIR" plugin.yml

# Also copy to jar/ directory
mkdir -p "$PROJECT_DIR/jar"
cp "$BUILD_DIR/$JAR_NAME" "$PROJECT_DIR/jar/"

echo "=== Build complete: $BUILD_DIR/$JAR_NAME ==="
ls -la "$BUILD_DIR/$JAR_NAME"
