#!/usr/bin/env bash
set -euo pipefail

skill_script="${CODEX_GH_CREATE_PR_SCRIPT:-$HOME/.codex/skills/gh-create-pr/scripts/create_pr.py}"

python3 "$skill_script" "$@"
