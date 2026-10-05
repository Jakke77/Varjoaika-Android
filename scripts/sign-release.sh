#!/usr/bin/env bash
# Keep the same private key for every published update.
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "$0")/.." && pwd)
INPUT=${1:?Usage: bash scripts/sign-release.sh unsigned.apk apksigner.jar [output.apk]}
SIGNER=${2:?Official Android SDK apksigner.jar is required}
OUTPUT=${3:-"$ROOT/dist/Varjoaika-1.0.1.apk"}
KEYDIR="$ROOT/.signing"
umask 077
mkdir -p "$KEYDIR" "$(dirname -- "$OUTPUT")"
if [ ! -f "$KEYDIR/varjoaika.p12" ]; then
  if [ -f "$KEYDIR/password" ]; then
    echo 'An existing password without a keystore needs manual recovery.' >&2
    exit 1
  fi
  python3 -c 'import secrets,sys; open(sys.argv[1],"x").write(secrets.token_urlsafe(48)+"\n")' "$KEYDIR/password"
  keytool -genkeypair -keystore "$KEYDIR/varjoaika.p12" -storetype PKCS12 \
    -alias varjoaika -keyalg RSA -keysize 3072 -validity 10000 \
    -dname 'CN=Varjoaika Android, OU=Varjoaika, O=Jakke77' \
    -storepass:file "$KEYDIR/password" -keypass:file "$KEYDIR/password"
fi
java -jar "$SIGNER" sign --ks "$KEYDIR/varjoaika.p12" --ks-key-alias varjoaika \
  --ks-pass "file:$KEYDIR/password" --out "$OUTPUT" "$INPUT"
java -jar "$SIGNER" verify --verbose --print-certs "$OUTPUT"
(cd -- "$(dirname -- "$OUTPUT")" && sha256sum "$(basename -- "$OUTPUT")") > "$OUTPUT.sha256"
echo "Signed APK: $OUTPUT"
