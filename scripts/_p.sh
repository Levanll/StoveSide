cd /d/Documents/deepseek-harness/default-workspace/cooking-timer
rm -f scripts/rename_release_assets.py
git add -A
git -c user.name=Levanll -c user.email=levanll@users.noreply.github.com commit -q -m "fix(release): APK 附件改名 StoveSide-<版本>.apk，正文与 SHA256SUMS 同步"
git log --oneline -2
export GH_PASS=$(tr -d "\r\n" < /d/Documents/deepseek-harness/default-workspace/lanpulse/.gh_token)
export GIT_CONFIG_COUNT=1
export GIT_CONFIG_KEY_0=credential.helper
export GIT_CONFIG_VALUE_0='!f() { echo username=x-access-token; echo password=$GH_PASS; }; f'
export GIT_TERMINAL_PROMPT=0
git push origin main 2>&1 | tail -2
echo "--- unpushed ---"
git log origin/main..HEAD --oneline
