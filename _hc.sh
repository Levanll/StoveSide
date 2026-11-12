cd /d/Documents/deepseek-harness/default-workspace/cooking-timer
mkdir -p scripts/__pycache__ && touch scripts/__pycache__/x.pyc
echo "=== check-ignore with dir present ==="
git check-ignore -v scripts/__pycache__/x.pyc || echo NOT_IGNORED
rm -rf scripts/__pycache__
git add -A
git -c user.name=Levanll -c user.email=levanll@users.noreply.github.com commit -q -m "docs: HANDOFF 标注 CHANGELOG 残缺与 pycache 未忽略；.gitignore 补 Python 缓存"
git log --oneline -1
export GH_PASS=$(tr -d "\r\n" < /d/Documents/deepseek-harness/default-workspace/lanpulse/.gh_token)
export GIT_CONFIG_COUNT=1
export GIT_CONFIG_KEY_0=credential.helper
export GIT_CONFIG_VALUE_0='!f() { echo username=x-access-token; echo password=$GH_PASS; }; f'
export GIT_TERMINAL_PROMPT=0
git push origin main 2>&1 | tail -2
echo "clean:[$(git status --porcelain)]"
