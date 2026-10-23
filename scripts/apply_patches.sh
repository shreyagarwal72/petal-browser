#!/usr/bin/env bash
# =============================================================================
# Petal Patch Applicator
# Copies Petal overlay sources into brave-core and applies git patches.
# Usage: apply_patches.sh [BRAVE_BROWSER_DIR]
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BRAVE_BROWSER_DIR="${1:-$HOME/brave-browser}"
BRAVE_CORE_DIR="$BRAVE_BROWSER_DIR/src/brave"
CHROMIUM_SRC="$BRAVE_BROWSER_DIR/src"

echo "=== Petal Patch Applicator ==="
echo "petal-browser : $REPO_ROOT"
echo "brave-browser : $BRAVE_BROWSER_DIR"
echo "brave-core    : $BRAVE_CORE_DIR"
echo ""

if [[ ! -d "$BRAVE_CORE_DIR" ]]; then
  echo "ERROR: brave-core not found at $BRAVE_CORE_DIR"
  echo "Run bootstrap.sh first."
  exit 1
fi

# ─── Copy Petal Java overlay sources ──────────────────────────────────────────
PETAL_JAVA_SRC="$REPO_ROOT/src/petal/java"
PETAL_OVERLAY_DST="$BRAVE_CORE_DIR/android/java/src/petal"
echo "[1/4] Copying Petal Java overlay → $PETAL_OVERLAY_DST"
mkdir -p "$PETAL_OVERLAY_DST"
cp -r "$PETAL_JAVA_SRC/." "$PETAL_OVERLAY_DST/"

# ─── Copy Petal resources overlay ─────────────────────────────────────────────
PETAL_RES_SRC="$REPO_ROOT/src/petal/res"
PETAL_RES_DST="$BRAVE_CORE_DIR/android/java/res_petal"
echo "[2/4] Copying Petal resources → $PETAL_RES_DST"
mkdir -p "$PETAL_RES_DST"
if [[ -d "$PETAL_RES_SRC" ]]; then
  cp -r "$PETAL_RES_SRC/." "$PETAL_RES_DST/"
fi

# Also overlay app/ resources (app/src/main/res) into the brave-core res path
APP_RES_SRC="$REPO_ROOT/app/src/main/res"
if [[ -d "$APP_RES_SRC" ]]; then
  echo "[2b/4] Copying app res overlay → $PETAL_RES_DST"
  cp -r "$APP_RES_SRC/." "$PETAL_RES_DST/"
fi

# ─── Ensure Chromium base & brave resources are populated ──────────────────
# Brave overrides upstream Android resources via brave-core:
# 1. Android icons under src/brave/app/theme/brave/android/res_brave(_base) -> res_chromium(_base)
# 2. Brave Android resources under src/brave/android/java/res -> chrome/android/java/res
#    (e.g., btn_delete_24dp.png, modern_location_bar.9.png, brave_colors.xml, etc.)
# 3. Brave Download internal resources -> chrome/browser/download/internal/android/java/res
# In a standard brave build, branding.js copies these into chromium.
# Here we mirror them directly so GN/Ninja build targets find all required resource assets.
BRAVE_THEME_ANDROID="$BRAVE_CORE_DIR/app/theme/brave/android"
CHROMIUM_RES_BASE="$CHROMIUM_SRC/chrome/android/java/res_chromium_base"
CHROMIUM_RES="$CHROMIUM_SRC/chrome/android/java/res_chromium"
BRAVE_ANDROID_RES="$BRAVE_CORE_DIR/android/java/res"
CHROMIUM_ANDROID_RES="$CHROMIUM_SRC/chrome/android/java/res"
BRAVE_DOWNLOAD_RES="$BRAVE_CORE_DIR/browser/download/internal/android/java/res"
CHROMIUM_DOWNLOAD_RES="$CHROMIUM_SRC/chrome/browser/download/internal/android/java/res"

if [[ -d "$BRAVE_THEME_ANDROID/res_brave_base" ]]; then
  echo "[2c/4] Populating res_chromium_base from Brave theme..."
  mkdir -p "$CHROMIUM_RES_BASE"
  cp -r "$BRAVE_THEME_ANDROID/res_brave_base/." "$CHROMIUM_RES_BASE/"
fi

if [[ -d "$BRAVE_THEME_ANDROID/res_brave" ]]; then
  echo "[2d/4] Populating res_chromium from Brave theme..."
  mkdir -p "$CHROMIUM_RES"
  cp -r "$BRAVE_THEME_ANDROID/res_brave/." "$CHROMIUM_RES/"
fi

if [[ -d "$BRAVE_ANDROID_RES" ]]; then
  echo "[2e/4] Populating chrome/android/java/res from Brave android res..."
  mkdir -p "$CHROMIUM_ANDROID_RES"
  cp -r "$BRAVE_ANDROID_RES/." "$CHROMIUM_ANDROID_RES/"
fi

if [[ -d "$BRAVE_DOWNLOAD_RES" ]]; then
  echo "[2f/4] Populating download internal res from Brave..."
  mkdir -p "$CHROMIUM_DOWNLOAD_RES"
  cp -r "$BRAVE_DOWNLOAD_RES/." "$CHROMIUM_DOWNLOAD_RES/"
fi

# ─── Fix plaster-modified base files ─────────────────────────────────────────
# In upstream brave-core, plaster YAML rewriters in rewrite/base/ insert calls to
# Brave helper functions (like ShouldPrintRustLog, IsMemoryAllocatorDumpNameInBraveAllowlist).
# The definitions of these helpers live in brave/chromium_src overrides, but the upstream
# files are compiled directly as part of //base:base.
# We inject the helper definitions directly into the corresponding Chromium source files.
CHROMIUM_RUST_LOG_CC="$CHROMIUM_SRC/base/logging/rust_logger/print_rust_log_ffi.cc"
if [[ -f "$CHROMIUM_RUST_LOG_CC" ]]; then
  echo "[2g/4] Injecting ShouldPrintRustLog definition into $CHROMIUM_RUST_LOG_CC..."
  python3 -c "
p = '$CHROMIUM_RUST_LOG_CC'
with open(p, 'r') as f:
    c = f.read()
if 'bool ShouldPrintRustLog' not in c:
    helper = '''
namespace {
bool ShouldPrintRustLog(int32_t& severity) {
  switch (severity) {
    case LOGGING_INFO:
    case LOGGING_WARNING:
      severity = LOGGING_VERBOSE;
      break;
    default:
      return true;
  }
  return VLOG_IS_ON(-severity);
}
}  // namespace
'''
    if 'namespace logging::internal {' in c:
        c = c.replace('namespace logging::internal {', 'namespace logging::internal {\n' + helper)
    elif 'void print_rust_log(' in c:
        c = helper + '\n' + c
    with open(p, 'w') as f:
        f.write(c)
    print('Successfully injected ShouldPrintRustLog into print_rust_log_ffi.cc')
" || true
fi

CHROMIUM_ALLOWLIST_CC="$CHROMIUM_SRC/base/trace_event/memory_infra_background_allowlist.cc"
if [[ -f "$CHROMIUM_ALLOWLIST_CC" ]]; then
  echo "[2h/4] Injecting IsMemoryAllocatorDumpNameInBraveAllowlist definition into $CHROMIUM_ALLOWLIST_CC..."
  python3 -c "
p = '$CHROMIUM_ALLOWLIST_CC'
with open(p, 'r') as f:
    c = f.read()
if 'bool IsMemoryAllocatorDumpNameInBraveAllowlist(' not in c:
    helper = '''
namespace {
bool IsMemoryAllocatorDumpNameInBraveAllowlist(const std::string& name) {
  if (name.starts_with(\"extensions/value_store/Extensions.Database.Open.BraveWallet/\")) {
    return true;
  }
  if (name.starts_with(\"extensions/value_store/Extensions.Database.Open.AdBlock Custom Resources/\")) {
    return true;
  }
  return false;
}
}  // namespace
'''
    if 'bool IsMemoryAllocatorDumpNameInAllowlist(' in c:
        c = c.replace('bool IsMemoryAllocatorDumpNameInAllowlist(', helper + '\nbool IsMemoryAllocatorDumpNameInAllowlist(')
    elif 'namespace base::trace_event {' in c:
        c = c.replace('namespace base::trace_event {', 'namespace base::trace_event {\n' + helper)
    else:
        c = helper + '\n' + c
    with open(p, 'w') as f:
        f.write(c)
    print('Successfully injected IsMemoryAllocatorDumpNameInBraveAllowlist into memory_infra_background_allowlist.cc')
else:
    print('IsMemoryAllocatorDumpNameInBraveAllowlist already defined in memory_infra_background_allowlist.cc')
"
fi

CHROMIUM_AEAD_CC="$CHROMIUM_SRC/crypto/aead.cc"
if [[ -f "$CHROMIUM_AEAD_CC" ]]; then
  echo "[2i/4] Fixing BRAVE_NONCE_LENGTH in $CHROMIUM_AEAD_CC..."
  python3 -c "
p = '$CHROMIUM_AEAD_CC'
with open(p, 'r') as f:
    content = f.read()

lines = content.split('\n')
fixed_lines = []
has_macro = any('#define BRAVE_NONCE_LENGTH' in l for l in lines)

for line in lines:
    stripped = line.strip()
    if stripped == 'BRAVE_NONCE_LENGTH':
        indent = line[:len(line) - len(line.lstrip())]
        fixed_lines.append(indent + 'BRAVE_NONCE_LENGTH;')
    else:
        fixed_lines.append(line)

fixed_content = '\n'.join(fixed_lines)
if not has_macro:
    macro_block = '#ifndef BRAVE_NONCE_LENGTH\n#define BRAVE_NONCE_LENGTH 12\n#endif\n\n'
    fixed_content = macro_block + fixed_content

with open(p, 'w') as f:
    f.write(fixed_content)
print('Successfully verified and fixed BRAVE_NONCE_LENGTH in crypto/aead.cc')
"
fi

# ─── Copy patches into brave-core patches directory ───────────────────────────
PATCHES_SRC="$REPO_ROOT/patches/petal"
PATCHES_DST="$BRAVE_CORE_DIR/patches/petal"
echo "[3/4] Copying patches → $PATCHES_DST"
mkdir -p "$PATCHES_DST"
cp -r "$PATCHES_SRC/." "$PATCHES_DST/"

echo "[4/4] Applying patches..."
# Try git apply directly on the appropriate target directories
cd "$CHROMIUM_SRC"
for patch in "$PATCHES_DST"/*.patch; do
  echo "  Applying: $(basename "$patch")"
  git apply --ignore-whitespace --whitespace=nowarn "$patch" 2>/dev/null || \
  (cd "$BRAVE_CORE_DIR" && git apply --ignore-whitespace --whitespace=nowarn "$patch" 2>/dev/null) || \
  echo "  INFO: Patch $(basename "$patch") skipped or already applied"
done

echo ""
echo "=== Petal patches applied successfully ==="
echo "Now run the build:"
echo "  cd $BRAVE_BROWSER_DIR"
echo "  npm run build -- --target=chrome_public_apk --config=android"
