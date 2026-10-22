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

# ─── Copy patches into brave-core patches directory ───────────────────────────
PATCHES_SRC="$REPO_ROOT/patches/petal"
PATCHES_DST="$BRAVE_CORE_DIR/patches/petal"
echo "[3/4] Copying patches → $PATCHES_DST"
mkdir -p "$PATCHES_DST"
cp -r "$PATCHES_SRC/." "$PATCHES_DST/"

# ─── Apply patches via brave's apply_patches.py ───────────────────────────────
echo "[4/4] Applying patches..."
APPLY_SCRIPT="$BRAVE_BROWSER_DIR/scripts/apply_patches.py"
if [[ -f "$APPLY_SCRIPT" ]]; then
  python3 "$APPLY_SCRIPT" \
    --patches_dir "$PATCHES_DST" \
    --repo_dir "$BRAVE_BROWSER_DIR" \
    --verbose || true
else
  # Fallback: apply each patch manually using git am
  echo "apply_patches.py not found; applying patches with git am..."
  cd "$BRAVE_BROWSER_DIR"
  for patch in "$PATCHES_DST"/*.patch; do
    echo "  Applying: $(basename "$patch")"
    git am --ignore-whitespace "$patch" 2>/dev/null || \
      git apply --ignore-whitespace "$patch" || \
      echo "  WARN: Patch $(basename "$patch") failed to apply cleanly — may need manual resolution"
  done
fi

echo ""
echo "=== Petal patches applied successfully ==="
echo "Now run the build:"
echo "  cd $BRAVE_BROWSER_DIR"
echo "  npm run build -- --target=chrome_public_apk --config=android"
