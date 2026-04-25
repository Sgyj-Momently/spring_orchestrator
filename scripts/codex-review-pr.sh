#!/usr/bin/env bash
set -euo pipefail

skill_script="${CODEX_GH_REVIEW_PR_SCRIPT:-$HOME/.codex/skills/gh-review-pr/scripts/review_pr.py}"

python3 "$skill_script" "$@"
