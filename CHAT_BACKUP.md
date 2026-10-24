# 🌸 Petal Browser — Chat State & Project Handoff Backup

**Date**: 2026-10-24  
**Repository**: [shreyagarwal72/petal-browser](https://github.com/shreyagarwal72/petal-browser)  
**Branch**: `main`  
**Latest Working Release**: [`latest`](https://github.com/shreyagarwal72/petal-browser/releases/tag/latest)  

---

## 1. Project Goal & Current Architecture

### Goal:
Transform the genuine, official **Brave Browser (arm64-v8a)** into **Petal Browser**, preserving 100% of Chromium speed, stability, Shields, and Sync v2, while customizing:
1. **App Identity**: App name ("Petal Browser"), authentic Petal launcher icons (both adaptive vector & legacy across all screen densities).
2. **Theme & Colors**: Replace signature Brave Orange (`#FB542B` / `#FF5000`) with Petal Material 3 Expressive Blue (`#1A73E8`) and Accent (`#3B82F6`).
3. **UI Elements**: Custom omnibox / URL bar styling and Petal toolbar shield button.
4. **Signing**: Signed with canonical Petal release keystore (`app/release.keystore`, alias `releasekey`, password `android`).

---

## 2. Key Files & Structure

| Path | Purpose |
|------|---------|
| `.github/workflows/build-petal-brave.yml` | GitHub Actions automated workflow. Downloads clean Brave APK, installs dependencies (`Pillow`, `zipalign`, `apksigner`), runs rebranding, and publishes to GitHub Releases under `latest`. |
| `scripts/rebrand_brave_to_petal.py` | Complete Python pipeline that extracts Brave base APK, replaces adaptive & legacy icons, rebrands string pool, injects Petal Blue palette, aligns (4-byte), and signs with release keystore. |
| `app/release.keystore` | Official Petal signing key (alias: `releasekey`, pass: `android`). |
| `app/src/main/res/mipmap-*/ic_launcher.png` | Authentic Petal Browser high-res source icons. |

---

## 3. Important Technical Discoveries & Rules

1. **Native Chromium Stability**:
   - The app runs Chromium's native engine (`libchrome.so`, 223MB) directly.
   - Do **NOT** compress `.so` libraries or asset `.pak` / `.bin` / `.dat` files in the zip archive (`ZIP_STORED` only) so Android's dynamic linker can `mmap` them at 4-byte boundaries.
   - Preserving internal ContentProvider authorities (`ChromeBrowserProvider`, `DropDataProvider`, `FileProvider`, etc.) is required for Chromium startup without crashing.

2. **Adaptive & Legacy Icons**:
   - Brave uses an `<adaptive-icon>` XML (`res/r2e.xml` and `res/BJw.xml`) with WebP mipmaps.
   - All 20 icon targets across densities (`mdpi` to `xxxhdpi`) are mapped and replaced with scaled WebP images:
     - Adaptive foregrounds: 108×108, 162×162, 216×216, 324×324, 432×432 (with 66% icon scale centered).
     - Legacy icons: 48×48, 72×72, 96×96, 144×144, 192×192.

3. **String Pool & Resource Table Integrity**:
   - In `resources.arsc`, only modify bytes inside the **table string pool** (`offset 12 to 28346876`). Modifying bytes outside this range damages type keys (e.g. `SearchWithBrave`), which causes instant resource inflation crashes.

4. **CI Workflow Runner Rules**:
   - Use dynamic relative paths (`os.path.dirname(...)`) in `rebrand_brave_to_petal.py` so it executes identically in local Termux (`/data/data/com.termux/...`) and on GitHub Actions Ubuntu runners (`/home/runner/work/...`).
   - `Pillow` must be installed (`pip install --no-cache-dir Pillow`) in the workflow step.

---

## 4. Current State & Immediate Next Steps

- **Latest commit on `main`**: `4e3077a` (Dynamic path handling for CI runner).
- **Latest signed APK artifact**: Generated locally at `/data/data/com.termux/files/home/brave_base/petal-browser-brave-rebranded.apk` (298 MB) and uploaded to GitHub Releases tag `latest`.
- **Next steps for next chat**:
  1. Confirm CI build completion for commit `4e3077a`.
  2. Implement Toolbar & Omnibox liquid glass styling and Petal shield button icon replacement (`APKTOOL_DUMMYVAL_0x7f090695` / `res/UWM.xml`).
