#!/usr/bin/env bash
set -euo pipefail

if ! command -v rsvg-convert >/dev/null 2>&1; then
  echo "rsvg-convert is required (install librsvg2-bin)." >&2
  exit 1
fi

RES="android/app/src/main/res"
FULL_LOGO="apps/web/public/jvc-logo.svg"
FOREGROUND="apps/android/jvc-launcher-foreground.svg"

for spec in "mdpi 48 108" "hdpi 72 162" "xhdpi 96 216" "xxhdpi 144 324" "xxxhdpi 192 432"; do
  read -r density legacy_size foreground_size <<< "$spec"
  dir="$RES/mipmap-$density"
  mkdir -p "$dir"
  rsvg-convert -w "$legacy_size" -h "$legacy_size" "$FULL_LOGO" > "$dir/ic_launcher.png"
  cp "$dir/ic_launcher.png" "$dir/ic_launcher_round.png"
  rsvg-convert -w "$foreground_size" -h "$foreground_size" "$FOREGROUND" > "$dir/ic_launcher_foreground.png"
done

mkdir -p "$RES/mipmap-anydpi-v26" "$RES/values"
cat > "$RES/mipmap-anydpi-v26/ic_launcher.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
XML
cp "$RES/mipmap-anydpi-v26/ic_launcher.xml" "$RES/mipmap-anydpi-v26/ic_launcher_round.xml"

cat > "$RES/values/ic_launcher_background.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#111113</color>
</resources>
XML

echo "Generated JVC launcher icons from the supplied J. logo."
