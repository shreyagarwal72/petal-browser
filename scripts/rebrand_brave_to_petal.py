import os
import sys
import zipfile
import shutil
import subprocess
import zlib
import hashlib

# Accept CLI arguments or use defaults
SRC_APK = sys.argv[1] if len(sys.argv) > 1 else "/data/data/com.termux/files/home/brave_base/Bravearm64Universal.apk"
OUT_SIGNED = sys.argv[2] if len(sys.argv) > 2 else "/data/data/com.termux/files/home/brave_base/petal-browser-brave-rebranded.apk"

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK_DIR = os.path.join(ROOT_DIR, "build_work_brave")
OUT_UNALIGNED = os.path.join(ROOT_DIR, "petal_unaligned.apk")
OUT_ALIGNED = os.path.join(ROOT_DIR, "petal_aligned.apk")
PETAL_RES = os.path.join(ROOT_DIR, "app/src/main/res")
KEYSTORE = os.path.join(ROOT_DIR, "app/release.keystore")

print("🌸 [1/6] Extracting clean base APK...")
shutil.rmtree(WORK_DIR, ignore_errors=True)
os.makedirs(WORK_DIR, exist_ok=True)

with zipfile.ZipFile(SRC_APK, 'r') as z:
    z.extractall(WORK_DIR)

shutil.rmtree(os.path.join(WORK_DIR, "META-INF"), ignore_errors=True)

print("🌸 [2/6] Replacing AndroidManifest.xml package identity, version name, and version code...")
manifest_path = os.path.join(WORK_DIR, "AndroidManifest.xml")
with open(manifest_path, "rb") as f:
    manifest_bytes = bytearray(f.read())

# 1. Package Name: "com.brave.browser" -> "com.petal.browser" (UTF-16LE, 34 bytes)
old_pkg_16 = "com.brave.browser".encode("utf-16le")
new_pkg_16 = "com.petal.browser".encode("utf-16le")
idx = 0
while True:
    idx = manifest_bytes.find(old_pkg_16, idx)
    if idx == -1: break
    manifest_bytes[idx:idx+len(old_pkg_16)] = new_pkg_16
    idx += len(new_pkg_16)

# 2. Version Name: "1.98.60" -> "4.4.000" (UTF-16LE, 14 bytes)
old_vname_16 = "1.98.60".encode("utf-16le")
new_vname_16 = "4.4.000".encode("utf-16le")
idx_vn = manifest_bytes.find(old_vname_16)
if idx_vn != -1:
    manifest_bytes[idx_vn:idx_vn+len(old_vname_16)] = new_vname_16

# 3. Version Code: 429806004 -> 440 (0x000001B8, 4 bytes)
old_vcode = (429806004).to_bytes(4, "little")
new_vcode = (440).to_bytes(4, "little")
idx_vc = manifest_bytes.find(old_vcode)
if idx_vc != -1:
    manifest_bytes[idx_vc:idx_vc+4] = new_vcode

with open(manifest_path, "wb") as f:
    f.write(manifest_bytes)

print("🌸 [3/6] Synchronizing classes.dex and native libchrome.so...")
# Update classes5.dex package references & recalculate Adler32 + SHA1
dex5_path = os.path.join(WORK_DIR, "classes5.dex")
if os.path.exists(dex5_path):
    with open(dex5_path, "rb") as f:
        dex5_data = bytearray(f.read())

    old_pkg = b"com.brave.browser"
    new_pkg = b"com.petal.browser"
    idx = 0
    while True:
        idx = dex5_data.find(old_pkg, idx)
        if idx == -1: break
        dex5_data[idx:idx+len(old_pkg)] = new_pkg
        idx += len(new_pkg)

    sha1 = hashlib.sha1(dex5_data[32:]).digest()
    dex5_data[12:32] = sha1
    adler = zlib.adler32(dex5_data[12:]) & 0xffffffff
    dex5_data[8:12] = adler.to_bytes(4, "little")

    with open(dex5_path, "wb") as f:
        f.write(dex5_data)

# Update native libchrome.so
libchrome_path = os.path.join(WORK_DIR, "lib/arm64-v8a/libchrome.so")
if os.path.exists(libchrome_path):
    with open(libchrome_path, "rb") as f:
        so_data = bytearray(f.read())

    old_pkg = b"com.brave.browser"
    new_pkg = b"com.petal.browser"
    idx = 0
    while True:
        idx = so_data.find(old_pkg, idx)
        if idx == -1: break
        so_data[idx:idx+len(old_pkg)] = new_pkg
        idx += len(new_pkg)

    with open(libchrome_path, "wb") as f:
        f.write(so_data)

print("🌸 [4/6] Rebranding App Launcher Icons & String Table...")
# Replace launcher icons
density_map = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"]
for d in density_map:
    petal_icon = os.path.join(PETAL_RES, d, "ic_launcher.png")
    if os.path.exists(petal_icon):
        for root, dirs, files in os.walk(os.path.join(WORK_DIR, "res")):
            if os.path.basename(root) == d or os.path.basename(root).startswith(d + "-"):
                for f in files:
                    if ("icon" in f.lower() or "launcher" in f.lower() or "brave" in f.lower()) and f.endswith(".png"):
                        shutil.copyfile(petal_icon, os.path.join(root, f))

# Rebrand resources.arsc safely (ONLY within table string pool range: 12 to 28346876)
arsc_path = os.path.join(WORK_DIR, "resources.arsc")
with open(arsc_path, "rb") as f:
    arsc_data = bytearray(f.read())

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

print("📦 [5/6] Packaging unaligned APK...")
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

print("✍️ [6/6] Aligning & Signing with authentic Petal Keystore...")
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
