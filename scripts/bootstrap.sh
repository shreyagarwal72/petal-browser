#!/usr/bin/env bash
# =============================================================================
# Petal Browser Bootstrap Script
# Sets up the full Brave + Petal build environment from scratch.
# Run this once on a fresh Ubuntu 22.04 / Debian machine.
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BRAVE_BROWSER_DIR="${BRAVE_BROWSER_DIR:-$HOME/brave-browser}"
PETAL_CORE_REPO="https://github.com/shreyagarwal72/petal-core.git"
BRAVE_BROWSER_REPO="https://github.com/shreyagarwal72/brave-browser.git"

echo "=== Petal Browser Bootstrap ==="
echo "Repo root : $REPO_ROOT"
echo "Build dir : $BRAVE_BROWSER_DIR"
echo ""

# ─── System packages ───────────────────────────────────────────────────────────
echo "[1/7] Installing system dependencies..."
sudo apt-get update -y
sudo apt-get install -y \
  build-essential git curl wget python3 python3-pip \
  openjdk-17-jdk-headless pkg-config libglib2.0-dev \
  libxml2-dev libssl-dev lsb-release zip unzip \
  libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 libdrm2 \
  libxcomposite1 libxdamage1 libxrandr2 libgbm1 libxkbcommon0

# ─── Node 20 ──────────────────────────────────────────────────────────────────
echo "[2/7] Installing Node 20..."
if ! node --version 2>/dev/null | grep -q "^v20"; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
fi
node --version && npm --version

# ─── depot_tools ──────────────────────────────────────────────────────────────
echo "[3/7] Setting up depot_tools..."
DEPOT_TOOLS_DIR="$HOME/depot_tools"
if [[ ! -d "$DEPOT_TOOLS_DIR" ]]; then
  git clone --depth=1 https://chromium.googlesource.com/chromium/tools/depot_tools.git "$DEPOT_TOOLS_DIR"
fi
export PATH="$DEPOT_TOOLS_DIR:$PATH"
echo "export PATH=\"$DEPOT_TOOLS_DIR:\$PATH\"" >> ~/.bashrc

# ─── Android SDK / NDK ────────────────────────────────────────────────────────
echo "[4/7] Setting up Android SDK..."
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS="$ANDROID_SDK_ROOT/cmdline-tools/latest"
if [[ ! -d "$CMDLINE_TOOLS" ]]; then
  mkdir -p "$ANDROID_SDK_ROOT"
  TMP_ZIP=$(mktemp /tmp/cmdline-tools.XXXXXX.zip)
  wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O "$TMP_ZIP"
  unzip -q "$TMP_ZIP" -d /tmp/cmdtools
  mkdir -p "$CMDLINE_TOOLS"
  mv /tmp/cmdtools/cmdline-tools/* "$CMDLINE_TOOLS/"
  rm -rf /tmp/cmdtools "$TMP_ZIP"
fi
export ANDROID_SDK_ROOT
export PATH="$CMDLINE_TOOLS/bin:$PATH"
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platforms;android-36" "build-tools;35.0.0" "ndk;27.2.12479018" "platform-tools"

# ─── Clone brave-browser fork ─────────────────────────────────────────────────
echo "[5/7] Cloning brave-browser fork..."
if [[ ! -d "$BRAVE_BROWSER_DIR" ]]; then
  git clone --depth=1 "$BRAVE_BROWSER_REPO" "$BRAVE_BROWSER_DIR"
fi

# Patch package.json to point brave-core at petal-core
python3 - <<PYEOF
import json, sys
with open("$BRAVE_BROWSER_DIR/package.json") as f:
    pkg = json.load(f)
pkg['config']['projects']['brave-core']['repository']['url'] = "$PETAL_CORE_REPO"
with open("$BRAVE_BROWSER_DIR/package.json", "w") as f:
    json.dump(pkg, f, indent=2)
print("package.json updated: brave-core -> petal-core")
PYEOF

# ─── npm install + init (syncs petal-core + Chromium) ─────────────────────────
echo "[6/7] Running npm install and init (this downloads Chromium — ~30 min)..."
cd "$BRAVE_BROWSER_DIR"
npm install
# Initialize without updating (faster for CI; use --init_network_timeout for slow connections)
npm run init -- --no_chromium_history

# ─── Apply Petal patches ──────────────────────────────────────────────────────
echo "[7/7] Applying Petal patches..."
"$REPO_ROOT/scripts/apply_patches.sh" "$BRAVE_BROWSER_DIR"

echo ""
echo "=== Bootstrap complete! ==="
echo "Next step:"
echo "  cd $BRAVE_BROWSER_DIR"
echo "  npm run build -- --target=chrome_public_apk --config=android"
