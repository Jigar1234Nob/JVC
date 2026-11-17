#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="android/app/src/main/java/au/jigar/jvc"
mkdir -p "$PACKAGE_DIR"

cp apps/android/src/main/java/au/jigar/jvc/MainActivity.java "$PACKAGE_DIR/MainActivity.java"
cp apps/android/src/main/java/au/jigar/jvc/JvcWebChromeClient.java "$PACKAGE_DIR/JvcWebChromeClient.java"
cp apps/android/src/main/java/au/jigar/jvc/JvcWebViewClient.java "$PACKAGE_DIR/JvcWebViewClient.java"

python3 - <<'PY'
from pathlib import Path

manifest = Path("android/app/src/main/AndroidManifest.xml")
if not manifest.exists():
    raise SystemExit("Capacitor AndroidManifest.xml was not generated")

content = manifest.read_text()
permissions = [
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.MODIFY_AUDIO_SETTINGS",
]
missing = [p for p in permissions if f'name="{p}"' not in content]
if missing:
    marker = "<application"
    if marker not in content:
        raise SystemExit("Could not find <application> in AndroidManifest.xml")
    additions = "".join(
        f'    <uses-permission android:name="{permission}" />\n'
        for permission in missing
    )
    content = content.replace(marker, additions + marker, 1)
    manifest.write_text(content)
PY

echo "Applied JVC Android navigation and media-permission overlay."
