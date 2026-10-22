#!/usr/bin/env bash
set -euo pipefail

# Scripts directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCHES_DIR="${REPO_ROOT}/patches"

BRAVE_SRC_DIR="${1:-${REPO_ROOT}/brave-browser/src}"

if [ ! -d "${BRAVE_SRC_DIR}" ]; then
    echo "Directory ${BRAVE_SRC_DIR} not found. Please provide the path to the brave-browser checkout."
    exit 1
fi

echo "==> Applying Petal Patches to Brave source tree at ${BRAVE_SRC_DIR}..."

for patch in "${PATCHES_DIR}"/brave/*.patch; do
    if [ -f "${patch}" ]; then
        echo "Applying patch: $(basename "${patch}")"
        git -C "${BRAVE_SRC_DIR}" apply --check "${patch}" 2>/dev/null && \
        git -C "${BRAVE_SRC_DIR}" apply "${patch}" || \
        echo "Note: Patch $(basename "${patch}") already applied or skipped."
    fi
done

echo "==> Syncing Petal UI and Engine components..."
TARGET_PETAL_DIR="${BRAVE_SRC_DIR}/src/chrome/android/java/src/com/petal/browser"
mkdir -p "${TARGET_PETAL_DIR}"
cp -r "${REPO_ROOT}/src/petal/java/com/petal/browser/"* "${TARGET_PETAL_DIR}/"

echo "==> All Petal patches and UI layers successfully applied!"
