#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="android/app/src/main/java/au/jigar/jvc"
mkdir -p "$PACKAGE_DIR"

cp apps/android/src/main/java/au/jigar/jvc/MainActivity.java "$PACKAGE_DIR/MainActivity.java"
cp apps/android/src/main/java/au/jigar/jvc/JvcWebChromeClient.java "$PACKAGE_DIR/JvcWebChromeClient.java"
cp apps/android/src/main/java/au/jigar/jvc/JvcWebViewClient.java "$PACKAGE_DIR/JvcWebViewClient.java"

python3 - <<'PY'
from pathlib import Path
import re

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

gradle = Path("android/app/build.gradle")
if not gradle.exists():
    raise SystemExit("Capacitor app/build.gradle was not generated")

build = gradle.read_text()
build, code_count = re.subn(r"(?m)^(\s*versionCode\s+)\d+", r"\g<1>20000", build, count=1)
build, name_count = re.subn(
    r"(?m)^(\s*versionName\s+)[\"'][^\"']+[\"']",
    r"\g<1>'2.0.0'",
    build,
    count=1,
)
if code_count != 1 or name_count != 1:
    raise SystemExit("Could not set Android versionCode/versionName in app/build.gradle")
gradle.write_text(build)
PY

echo "Applied JVC Android navigation, media permissions and version metadata."
