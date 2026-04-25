#!/usr/bin/env bash
set -euo pipefail

kind="${1:-done}"
message="${2:-Codex 작업 상태가 업데이트되었습니다.}"
cursor_bundle_id="com.todesktop.230313mzl4w4u92"

case "$kind" in
  done)
    title="Codex: 작업 완료"
    ;;
  approval)
    title="Codex: 승인 필요"
    ;;
  *)
    title="Codex"
    ;;
esac

if command -v terminal-notifier >/dev/null 2>&1; then
  terminal-notifier \
    -title "$title" \
    -message "$message" \
    -activate "$cursor_bundle_id"
  exit 0
fi

osascript \
  -e "display notification \"$message\" with title \"$title\""

if [[ "${CODEX_NOTIFY_FOCUS_CURSOR:-0}" == "1" ]]; then
  osascript \
    -e 'tell application "Cursor" to activate'
fi
