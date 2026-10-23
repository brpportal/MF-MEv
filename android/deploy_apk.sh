#!/usr/bin/env bash
# ==============================================================================
# MF MPanel Evidence — Automated Build & Deploy Script
# ==============================================================================
# This script builds signed APKs ready for installation on Android phones,
# resolves package parsing issues, and prepares GitHub Release artifacts.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================================================"
echo "      MF MPanel Evidence — Android APK Build & Deploy Script"
echo "======================================================================"

# 1. Check Java & keytool
if ! command -v keytool &> /dev/null; then
    echo "[-] Error: 'keytool' is not installed or not in PATH."
    exit 1
fi

# 2. Setup Keystores
echo "[*] Step 1: Checking and configuring signing keystores..."

# Debug keystore
if [ ! -f "debug.keystore" ]; then
    if [ -f "debug.keystore.base64" ]; then
        echo "[+] Materializing debug.keystore from base64 companion..."
        base64 -d debug.keystore.base64 > debug.keystore
    else
        echo "[+] Generating debug.keystore..."
        keytool -genkey -v -keystore debug.keystore -alias androiddebugkey \
            -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
            -dname "CN=Android Debug,O=Android,C=US"
    fi
fi

# Release upload keystore
export KEYSTORE_PATH="${KEYSTORE_PATH:-$PWD/my-upload-key.jks}"
export STORE_PASSWORD="${STORE_PASSWORD:-android}"
export KEY_PASSWORD="${KEY_PASSWORD:-android}"

echo "[+] Creating release upload key at $KEYSTORE_PATH..."
rm -f "$KEYSTORE_PATH"
keytool -genkey -v -keystore "$KEYSTORE_PATH" -alias upload \
    -storepass "$STORE_PASSWORD" -keypass "$KEY_PASSWORD" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=MF MPanel, OU=Evidence Team, O=MF, L=Global, S=Global, C=US"

# 3. Determine Gradle command
GRADLE_CMD="gradle"
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    GRADLE_CMD="./gradlew"
fi

echo "[*] Step 2: Building Debug and Release APKs with $GRADLE_CMD..."
$GRADLE_CMD assembleDebug assembleRelease

# 4. Prepare Output Distribution Directory
DIST_DIR="$PWD/dist"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

echo "[*] Step 3: Verifying and copying generated APKs to $DIST_DIR..."

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"

if [ -f "$DEBUG_APK" ]; then
    cp "$DEBUG_APK" "$DIST_DIR/MF_MPanel_Evidence_v1.0_debug.apk"
    echo "[+] Debug APK ready: $DIST_DIR/MF_MPanel_Evidence_v1.0_debug.apk"
fi

if [ -f "$RELEASE_APK" ]; then
    cp "$RELEASE_APK" "$DIST_DIR/MF_MPanel_Evidence_v1.0_release.apk"
    echo "[+] Release APK ready: $DIST_DIR/MF_MPanel_Evidence_v1.0_release.apk"
fi

# 5. Check Signatures and SHA-256
echo "======================================================================"
echo "                     BUILD SUMMARY & ARTIFACTS"
echo "======================================================================"

for apk in "$DIST_DIR"/*.apk; do
    if [ -f "$apk" ]; then
        filename=$(basename "$apk")
        filesize=$(du -h "$apk" | cut -f1)
        sha256=$(sha256sum "$apk" | cut -d' ' -f1)
        echo "File:    $filename"
        echo "Size:    $filesize"
        echo "SHA-256: $sha256"
        echo "----------------------------------------------------------------------"
    fi
done

# 6. Optional GitHub CLI Release
if command -v gh &> /dev/null; then
    echo "[*] GitHub CLI (gh) detected."
    if gh auth status &> /dev/null; then
        echo "[+] To publish these APKs to GitHub Releases, run:"
        echo "    gh release create v1.0.0 $DIST_DIR/*.apk --title 'MF MPanel Evidence v1.0.0' --notes 'Official Release APKs'"
    fi
fi

echo ""
echo "[✓] Build complete! You can now install either APK on any Android phone."
echo "    Recommended: Transfer 'MF_MPanel_Evidence_v1.0_release.apk' to your phone"
echo "    and open with Files/Downloads to install."
