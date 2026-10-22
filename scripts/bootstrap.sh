#!/usr/bin/env bash
set -euo pipefail

echo "=========================================================="
echo "    Petal Browser (Chromium / Brave Core) Setup"
echo "=========================================================="

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Verify prerequisites
echo "[1/4] Checking environment..."
if ! command -v git &> /dev/null; then
    echo "Error: git is required."
    exit 1
fi

echo "[2/4] Setting up depot_tools..."
DEPOT_TOOLS_DIR="${HOME}/depot_tools"
if [ ! -d "${DEPOT_TOOLS_DIR}" ]; then
    echo "Cloning depot_tools..."
    git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git "${DEPOT_TOOLS_DIR}"
fi
export PATH="${DEPOT_TOOLS_DIR}:${PATH}"

echo "[3/4] Preparing brave-browser checkout..."
BRAVE_DIR="${REPO_ROOT}/brave-browser"
if [ ! -d "${BRAVE_DIR}" ]; then
    mkdir -p "${BRAVE_DIR}"
    cd "${BRAVE_DIR}"
    echo "Initializing brave-browser (depth 1)..."
    git clone --depth 1 https://github.com/brave/brave-browser.git src
    cd src
    npm install
fi

echo "[4/4] Applying Petal UI, M3 Expressive, and Liquid Glass patches..."
"${REPO_ROOT}/scripts/apply_patches.sh" "${BRAVE_DIR}/src"

echo "=========================================================="
echo "Setup complete! Ready to compile Petal Browser via CI / GN"
echo "=========================================================="
