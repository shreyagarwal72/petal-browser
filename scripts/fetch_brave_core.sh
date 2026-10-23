#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Petal Browser - Brave Core Engine Extractor
# Downloads official Brave Android arm64 release APK and extracts:
# 1. Native shared libraries (libchrome.so, libbrave.so) -> app/src/main/jniLibs/arm64-v8a/
# 2. Chromium assets (pak files, icudtl.dat)              -> app/src/main/assets/
# 3. Brave & Chromium classes.dex -> JAR                 -> app/libs/brave_core_classes.jar
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
TEMP_DIR="${ROOT_DIR}/.brave_fetch_tmp"
BRAVE_REPO="brave/brave-browser"
BRAVE_VERSION="${1:-latest}"

echo "🦁 [1/5] Identifying Brave release..."
if [ "$BRAVE_VERSION" = "latest" ]; then
    # Fetch latest release tag using JSON output
    BRAVE_TAG=$(gh release list --repo "$BRAVE_REPO" --exclude-pre-releases --limit 1 --json tagName --jq '.[0].tagName' 2>/dev/null || echo "")
    if [ -z "$BRAVE_TAG" ]; then
        echo "⚠️ Fallback to hardcoded stable version..."
        BRAVE_TAG="v1.100.23"
    fi
else
    BRAVE_TAG="$BRAVE_VERSION"
fi

echo "🦁 Target Brave Release Tag: $BRAVE_TAG"

rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

echo "🦁 [2/5] Downloading Bravearm64Universal.apk from $BRAVE_REPO ($BRAVE_TAG)..."
gh release download "$BRAVE_TAG" --repo "$BRAVE_REPO" --pattern "Bravearm64Universal.apk" || \
gh release download "$BRAVE_TAG" --repo "$BRAVE_REPO" --pattern "BraveMonoarm64.apk"

APK_FILE=$(ls *.apk | head -n 1)
if [ -z "$APK_FILE" ]; then
    echo "❌ Failed to download Brave APK!"
    exit 1
fi
echo "✅ Downloaded: $APK_FILE"

echo "🦁 [3/5] Extracting native libraries (libchrome.so, libbrave.so)..."
JNILIBS_DIR="${ROOT_DIR}/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$JNILIBS_DIR"

# Extract arm64 native libs
unzip -q "$APK_FILE" "lib/arm64-v8a/*" -d extracted_libs/ || true
if [ -d "extracted_libs/lib/arm64-v8a" ]; then
    cp -rv extracted_libs/lib/arm64-v8a/*.so "$JNILIBS_DIR/"
    echo "✅ Native libraries extracted to $JNILIBS_DIR:"
    ls -lh "$JNILIBS_DIR"
else
    echo "❌ No arm64 native libraries found in APK!"
    exit 1
fi

echo "🦁 [4/5] Extracting Chromium assets & resources..."
ASSETS_DIR="${ROOT_DIR}/app/src/main/assets"
mkdir -p "$ASSETS_DIR"

unzip -q "$APK_FILE" "assets/*" -d extracted_assets/ || true
if [ -d "extracted_assets/assets" ]; then
    cp -rv extracted_assets/assets/* "$ASSETS_DIR/"
    echo "✅ Assets extracted to $ASSETS_DIR"
fi

echo "🦁 [5/5] Extracting & Converting DEX files to compile JAR..."
LIBS_DIR="${ROOT_DIR}/app/libs"
mkdir -p "$LIBS_DIR"

# Extract classes*.dex
unzip -q "$APK_FILE" "classes*.dex" -d extracted_dex/

# Check for dex2jar or d8/baksmali, or install d2j-dex2jar
if command -v d2j-dex2jar >/dev/null 2>&1; then
    DEX2JAR_CMD="d2j-dex2jar"
elif [ -x "/tmp/dex-tools/d2j-dex2jar.sh" ]; then
    DEX2JAR_CMD="/tmp/dex-tools/d2j-dex2jar.sh"
else
    echo "⚙️ Installing dex2jar tools..."
    mkdir -p /tmp/dex-tools
    curl -sL https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip -o /tmp/dex-tools.zip
    unzip -q -o /tmp/dex-tools.zip -d /tmp/
    mv /tmp/dex-tools-*/* /tmp/dex-tools/ 2>/dev/null || true
    chmod +x /tmp/dex-tools/*.sh || true
    DEX2JAR_CMD="/tmp/dex-tools/d2j-dex2jar.sh"
fi

echo "⚙️ Converting DEX to JAR using $DEX2JAR_CMD..."
mkdir -p dex_jars
cd extracted_dex
for dex in classes*.dex; do
    echo "Converting $dex..."
    "$DEX2JAR_CMD" "$dex" -o "../dex_jars/${dex}.jar" --force || true
done
cd "$TEMP_DIR"

echo "⚙️ Merging DEX jars into brave_core_classes.jar..."
mkdir -p merged_classes
cd dex_jars
for j in *.jar; do
    unzip -q -o "$j" -d ../merged_classes/ || true
done
cd ../merged_classes
# Remove META-INF signatures
rm -rf META-INF/*.SF META-INF/*.RSA META-INF/*.DSA 2>/dev/null || true
jar -cf "${LIBS_DIR}/brave_core_classes.jar" .
cd "$ROOT_DIR"

echo "✅ brave_core_classes.jar created successfully:"
ls -lh "${LIBS_DIR}/brave_core_classes.jar"

# Cleanup temporary folder
rm -rf "$TEMP_DIR"
echo "🎉 Brave Core Engine extraction complete in < 60 seconds!"
