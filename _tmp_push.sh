cd /d/Documents/deepseek-harness/default-workspace/cooking-timer
git add -A
git -c user.name=Levanll -c user.email=levanll@users.noreply.github.com commit -q -m "chore: 清理临时脚本"
git log --oneline -1
export GH_PASS=$(tr -d "\r\n" < /d/Documents/deepseek-harness/default-workspace/lanpulse/.gh_token)
export GIT_CONFIG_COUNT=1
export GIT_CONFIG_KEY_0=credential.helper
export GIT_CONFIG_VALUE_0='!f() { echo username=x-access-token; echo password=$GH_PASS; }; f'
export GIT_TERMINAL_PROMPT=0
git push origin main 2>&1 | tail -2
git status --porcelain | head -3
echo "--- tree ---"
find app/src/main/java -name "*.kt" | sed "s|app/src/main/java/cn/levanll/stoveside/||" | sort
