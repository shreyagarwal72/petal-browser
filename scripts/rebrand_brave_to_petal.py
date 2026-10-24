import os
import sys
import zipfile
import shutil
import subprocess
import io
from PIL import Image

SRC_APK = sys.argv[1] if len(sys.argv) > 1 else "/data/data/com.termux/files/home/brave_base/Bravearm64Universal.apk"
OUT_SIGNED = sys.argv[2] if len(sys.argv) > 2 else "/data/data/com.termux/files/home/brave_base/petal-browser-brave-rebranded.apk"

ROOT_DIR = "/data/data/com.termux/files/home/petal-browser"
WORK_DIR = "/data/data/com.termux/files/home/brave_base/work"
OUT_UNALIGNED = "/data/data/com.termux/files/home/brave_base/petal_unaligned.apk"
OUT_ALIGNED = "/data/data/com.termux/files/home/brave_base/petal_aligned.apk"
PETAL_RES = os.path.join(ROOT_DIR, "app/src/main/res")
KEYSTORE = os.path.join(ROOT_DIR, "app/release.keystore")

print("🌸 [1/5] Extracting clean base APK...")
shutil.rmtree(WORK_DIR, ignore_errors=True)
os.makedirs(WORK_DIR, exist_ok=True)

with zipfile.ZipFile(SRC_APK, 'r') as z:
    z.extractall(WORK_DIR)

shutil.rmtree(os.path.join(WORK_DIR, "META-INF"), ignore_errors=True)

print("🌸 [2/5] Replacing all launcher & adaptive icons with authentic Petal Browser icons...")
# Authentic Petal icon source
petal_icon_source = os.path.join(PETAL_RES, "mipmap-xxxhdpi/ic_launcher.png")
base_petal_img = Image.open(petal_icon_source).convert("RGBA")

# Exact mapping of all launcher and adaptive mipmaps identified in Brave APK
icon_targets = {
    # mdpi (48x48 icon, 108x108 adaptive foreground)
    "res/80y": (48, 48, False),
    "res/Nkp": (48, 48, False),
    "res/QtC": (108, 108, True),
    "res/lMS": (108, 108, True),
    # hdpi (72x72 icon, 162x162 adaptive foreground)
    "res/k1k": (72, 72, False),
    "res/oKz": (72, 72, False),
    "res/rps": (162, 162, True),
    "res/Zyh": (162, 162, True),
    # xhdpi (96x96 icon, 216x216 adaptive foreground)
    "res/0dH": (96, 96, False),
    "res/YGc": (96, 96, False),
    "res/PQv": (216, 216, True),
    "res/6Pj": (216, 216, True),
    # xxhdpi (144x144 icon, 324x324 adaptive foreground)
    "res/R4U": (144, 144, False),
    "res/gG8": (144, 144, False),
    "res/EFE": (324, 324, True),
    "res/BLH": (324, 324, True),
    # xxxhdpi (192x192 icon, 432x432 adaptive foreground)
    "res/tT1": (192, 192, False),
    "res/tOW": (192, 192, False),
    "res/ima": (432, 432, True),
    "res/PCI": (432, 432, True),
}

for path_rel, (w, h, is_adaptive) in icon_targets.items():
    dest_path = os.path.join(WORK_DIR, path_rel)
    if is_adaptive:
        # Standard Android adaptive icon foreground: icon occupies inner ~66% with transparent margin
        canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        icon_dim = int(w * 0.66)
        scaled_icon = base_petal_img.resize((icon_dim, icon_dim), Image.Resampling.LANCZOS)
        offset = (w - icon_dim) // 2
        canvas.paste(scaled_icon, (offset, offset), scaled_icon)
        canvas.save(dest_path, "WEBP")
    else:
        scaled = base_petal_img.resize((w, h), Image.Resampling.LANCZOS)
        scaled.save(dest_path, "WEBP")

print(f"✅ Generated and injected all {len(icon_targets)} launcher & adaptive icons!")

print("🌸 [3/5] Updating Strings & Theme Palettes in resources.arsc...")
arsc_path = os.path.join(WORK_DIR, "resources.arsc")
with open(arsc_path, "rb") as f:
    arsc_data = bytearray(f.read())

# Rebrand resources.arsc safely (ONLY within table string pool range: 12 to 28346876)
POOL_END = 28346876
pool_data = arsc_data[12:POOL_END]
pool_data = pool_data.replace(b"Brave Browser", b"Petal Browser")
pool_data = pool_data.replace(b"Brave", b"Petal")
arsc_data[12:POOL_END] = pool_data

# Theme Colors (Brave Orange -> Petal Material 3 Expressive Blue)
orange_bytes = bytes([0x2B, 0x54, 0xFB, 0xFF])
petal_blue = bytes([0xE8, 0x73, 0x1A, 0xFF])
c_idx = 0
while True:
    c_idx = arsc_data.find(orange_bytes, c_idx)
    if c_idx == -1: break
    arsc_data[c_idx:c_idx+4] = petal_blue
    c_idx += 4

with open(arsc_path, "wb") as f:
    f.write(arsc_data)

print("📦 [4/5] Packaging unaligned APK...")
do_not_compress = [
    "resources.arsc",
    "assets/brave_resources.pak",
    "assets/chrome_100_percent.pak+40064988+",
    "assets/chrome_200_percent.pak+40064988+",
    "assets/dev_ui_resources.pak",
    "assets/icudtl.dat+40064988+",
    "assets/resources.pak+40064988+",
    "assets/snapshot_blob_64.bin+40064988+",
    "assets/v8_context_snapshot_64.bin+40064988+",
    "assets/webapk_dex_version.txt"
]

if os.path.exists(OUT_UNALIGNED):
    os.remove(OUT_UNALIGNED)

with zipfile.ZipFile(OUT_UNALIGNED, 'w', compression=zipfile.ZIP_DEFLATED) as z_out:
    for root, dirs, files in os.walk(WORK_DIR):
        for f in files:
            full_path = os.path.join(root, f)
            rel_path = os.path.relpath(full_path, WORK_DIR)
            if rel_path.endswith(".so") or rel_path in do_not_compress:
                z_out.write(full_path, rel_path, compress_type=zipfile.ZIP_STORED)
            else:
                z_out.write(full_path, rel_path, compress_type=zipfile.ZIP_DEFLATED)

print("✍️ [5/5] Aligning & Signing with authentic Petal Keystore...")
if os.path.exists(OUT_ALIGNED):
    os.remove(OUT_ALIGNED)

zipalign_bin = shutil.which("zipalign")
subprocess.check_call([zipalign_bin, "-v", "-p", "4", OUT_UNALIGNED, OUT_ALIGNED])

apksigner_bin = shutil.which("apksigner")
subprocess.check_call([
    apksigner_bin, "sign",
    "--ks", KEYSTORE,
    "--ks-key-alias", "releasekey",
    "--ks-pass", "pass:android",
    "--key-pass", "pass:android",
    "--v1-signing-enabled", "true",
    "--v2-signing-enabled", "true",
    "--v3-signing-enabled", "true",
    "--out", OUT_SIGNED,
    OUT_ALIGNED
])

print("🔍 Validating final package...")
subprocess.check_call([apksigner_bin, "verify", "--verbose", OUT_SIGNED])
print(f"🎉 Complete! Final package: {OUT_SIGNED}")
