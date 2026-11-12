cd /d/Documents/deepseek-harness/default-workspace/cooking-timer
git add -A
git -c user.name=Levanll -c user.email=levanll@users.noreply.github.com commit -q -m "fix(ci): 重新修正工作流模板表达式；发布前自动同步版本号到站点与 README"
git log --oneline -3
echo "--- from HEAD ---"
git show HEAD:.github/workflows/build-release.yml | sed -n "49p"
echo "expr_count:"
git show HEAD:.github/workflows/build-release.yml | grep -c "${{"
echo "--- unpushed ---"
git log origin/main..HEAD --oneline
