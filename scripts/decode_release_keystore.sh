#!/usr/bin/env bash
set -euo pipefail

# Prints only the keystore type. The decoded key is written to the caller's path.
output_file="${1:?keystore output path required}"
: "${KEYSTORE_B64:?ANDROID_KEYSTORE_BASE64 is missing}"
umask 077
temp_dir=$(mktemp -d)
trap 'rm -f "$temp_dir/payload" "$temp_dir/next"; rmdir "$temp_dir"' EXIT

printf '%s' "$KEYSTORE_B64" | base64 --decode > "$temp_dir/payload"
for layer in 0 1 2; do
  magic=$(od -An -tx1 -N4 "$temp_dir/payload" | tr -d ' \n')
  case "$magic" in
    feedfeed)
      cp "$temp_dir/payload" "$output_file"
      echo JKS
      exit 0
      ;;
    30*)
      cp "$temp_dir/payload" "$output_file"
      echo PKCS12
      exit 0
      ;;
    1f8b*)
      if ! gzip -dc "$temp_dir/payload" > "$temp_dir/next"; then
        echo 'Signing secret contains invalid gzip data.' >&2
        exit 1
      fi
      ;;
    *)
      if ! base64 --decode < "$temp_dir/payload" > "$temp_dir/next" 2>/dev/null ||
        [ ! -s "$temp_dir/next" ]; then
        echo 'Signing secret does not contain a binary JKS or PKCS12 keystore.' >&2
        exit 1
      fi
      ;;
  esac
  mv "$temp_dir/next" "$temp_dir/payload"
done

echo 'Signing secret has too many encoding layers or is not a keystore.' >&2
exit 1
