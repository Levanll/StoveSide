# HANDOFF — 项目交接

> 给接手这个项目的下一个 AI 会话 / 开发者。读完这一份就能继续干活，不必翻历史对话。
> 最后更新：2026-11-12，对应 v1.0.2（versionCode 3）。

---

## 一、项目是什么

**灶暖 / StoveSide** — Android 原生烹饪计时 App。

- 仓库：https://github.com/Levanll/StoveSide （公开，MIT）
- 站点：https://levanll.github.io/StoveSide/ （中文）/ `/en/` （英文）
- 包名：`cn.levanll.stoveside`
- 一句话：它记的不是菜谱，是「现在这一刻哪口锅走到哪一步」。

核心设计是**模板与实例分离**：`Recipe` 是食谱模板，每次真正下厨开一个 `Cook`（烹饪实例），
实例的步骤状态与备注写在 `CookStep` 上，永不回写模板。同一道菜今晚做三份就开三个实例，各自独立推进。

用户明确同意的功能边界（P0 + A1–A4），**没有做**的：内置示例食谱（A5）、桌面小组件（A6）、云同步、账号。

---

## 二、当前状态

| 项 | 状态 |
|---|---|
| 版本 | 1.0.2（versionCode 3） |
| main 分支 | 干净，与 origin 同步 |
| Releases | v1.0.0 / v1.0.1 / v1.0.2，各含 `StoveSide-<版本>.apk` + `SHA256SUMS` |
| Pages | 已部署（gh-pages 分支），中英双页 |
| About | 中英双语，中文在前，185 字符 |
| Topics | android / kotlin / jetpack-compose / cooking / timer / room / bilingual |
| 单测 | 8 个用例，全过 |

### 已验证可用（模拟器实测）

- 3 个实例并行计时，各自倒数，互不干扰
- 粘贴文本解析：`1. blanch beef 8 mins / 2. fry tomato 10 mins / 3. simmer 30 mins` → 3 步 480s/600s/1800s
- 步骤备注：点备注行 → 弹窗 → 保存 → 落库到该实例的 `CookStep.note`
- 中英切换：切换后杀进程重启仍保持
- 前台服务：冷启动 0 个 ServiceRecord；点开始 = 1；全部暂停 7 秒后归 0
- 到期通知：channel=due 实际触发
- 签名：`CN=StoveSide, O=Levanll`，三版一致，可覆盖安装

### 未验证 / 已知风险

- **CI workflow 从未在 GitHub 上真实跑过**。表达式语法与 bash 已静态校验，但没触发过 `workflow_dispatch`。
- **导入语言恢复** 没有单测覆盖（需要 Android Context），只做了代码路径接入。
- 未在真机（非模拟器）上跑过，只在 x86_64 API 34 模拟器验证。
- **CHANGELOG 当前是残缺的（待修）**：`9c69615` 合并重复的 1.0.2 标题时把文件从 56 行砍到 28 行，
  丢掉了 `## [Unreleased]`、`## [1.0.0]` 整节（8 条双语 Added + Known limitations）、文件头说明段与底部 compare/tag 链接；
  且 H1 `# 更新日志 · Changelog` 排在正文末尾，顺序颠倒。完整内容可从 `git show 8458ef0:CHANGELOG.md` 取回。
- **`scripts/__pycache__/` 未被 `.gitignore` 覆盖**：跑过 `py_compile` 后 `git add -A` 会把它带进公开仓库。

---

## 三、环境（这台机器上的实际路径）

| 项 | 路径 |
|---|---|
| JDK | `C:\Program Files\Java\jdk-17` |
| Gradle 发行版 | `F:\android-dev\gradle-dist\gradle-8.9` |
| GRADLE_USER_HOME | `F:\android-dev\gradle-home`（1.8G，已含全部依赖） |
| Android SDK | `F:\android-dev\sdk`（platform 34 / build-tools 34.0.0） |
| AVD | `F:\android-dev\avd`（`ANDROID_AVD_HOME`，名 `stoveside34`，x86_64 API 34） |
| 项目根 | `D:\Documents\deepseek-harness\default-workspace\cooking-timer` |
| GitHub Token | `D:\Documents\deepseek-harness\default-workspace\lanpulse\.gh_token`（93 字节） |
| 快照归档 | `F:\deepseek-harness\_archive\<时间戳>-cooktimer\` |

**所有缓存、构建产物、下载物一律不落 C 盘。**

### 构建命令

```bash
cd D:/Documents/deepseek-harness/default-workspace/cooking-timer
export GRADLE_USER_HOME=F:/android-dev/gradle-home
F:/android-dev/gradle-dist/gradle-8.9/bin/gradle --offline \
  -Pkotlin.compiler.execution.strategy=in-process assembleRelease
```

> **必须加 `-Pkotlin.compiler.execution.strategy=in-process`**。
> 本机 Kotlin 编译 worker 的 JVM agent 初始化会失败（`agent library failed to init: instrument`），
> 这是环境问题不是项目问题。不加这个参数只会得到一句毫无信息量的 "A failure occurred while executing GradleCompilerRunner"。

跑单测需要联网拉 junit（首次），之后可 `--offline`：

```bash
gradle -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=8686 \
  -Pkotlin.compiler.execution.strategy=in-process testDebugUnitTest
```

### 签名

- `keystore/release.jks`（别名 `stoveside`）+ `keystore/keystore.properties`
- **两者都在 `.gitignore`，绝不入库、绝不上传**
- 口令在 `keystore.properties` 里；文件不存在时 Gradle 自动降级产出 `app-release-unsigned.apk`（见第四节）

### 模拟器

```bash
export ANDROID_AVD_HOME=F:/android-dev/avd
export ANDROID_SDK_ROOT=F:/android-dev/sdk ANDROID_HOME=F:/android-dev/sdk
/f/android-dev/sdk/emulator/emulator.exe -avd stoveside34 -no-snapshot -no-audio \
  -no-window -gpu swiftshader_indirect -no-boot-anim -port 5554
```

`-no-window` 下用 `adb shell input tap x y` + `adb exec-out screencap -p` 操作与截图。
屏幕 1080×2340，底部导航三个 tab 的 y 约 2090，x 分别 180 / 540 / 907。

---

## 四、架构（改代码前必读）

### 数据模型

```
Recipe ──< Step                (模板：这道菜应当怎么做)
Recipe ──< Cook ──< CookStep   (实例：这次实际怎么走的)
```

- 开火时把模板步骤**整份拷贝**成 `CookStep`，之后两条线各自演化
- 备注两层：模板 `Step.note` 是经验沉淀，实例 `CookStep.note` 是临时记录，实例改动不回写
- 删 Recipe 级联删 Step / Cook / CookStep（外键 CASCADE）

### 计时调度的三条硬规则

1. **墙钟为准**：`dueAt = startedAt + durationSec * 1000` 存数据库。UI 每次刷新用 `dueAt - System.currentTimeMillis()` 算剩余。杀进程、重启、息屏都不影响准确性。
2. **单一 tick 循环**：`StoveViewModel` 里一个 `while(true) { refresh(); delay(...) }`，有运行中计时 500ms 一次，否则 2s。**不要为每个计时器开协程**。
3. **数据库是唯一真相**：UI 不持有独立的计时状态，只有派生的 `remainingMs`。

### 前台服务的进出条件（曾经出过 bug）

- `MainActivity` **先查有没有 running 步骤**，有才 `TimerService.start()`
- `TimerService.tickLoop` 连续 3 次查不到 running 就 `stopSelf()` 自行退出
- 到点**只提醒、不改状态**。曾经提醒时把状态打回 `pending`，导致「+1 分钟」无处可点、用户也看不出这锅还在跑

### 文本解析（`domain/RecipeParser.kt`）

- 正则顺序：`halfHourPattern`（「一个半小时」）**必须排在** `timePattern` 之前，否则 `个` 会截断数字段，「一个半小时」被算成 30 分钟
- 支持：`5分钟`、`十五分钟`、`1.5 小时`、`一个半小时`、`两小时半`、`90 seconds`、`2 mins`、`1h`
- 单步超过 12 小时判定误判，丢弃时长
- 解析不出的行不丢弃，整行作为标题保留

### 国际化

- 语言存 SharedPreferences（`stoveside_prefs` / `lang`），**不跟随系统 Locale**
- 文案用 `I18n.t("中文", "English")` 就地双语，**不用两套 strings.xml**——避免资源错位与漏翻
- 需求：切换后下次冷启动必须还原

### 备份与导入

- 导出单文件 JSON：`{schemaVersion, app, exportedAt, recipes[], cooks[], settings}`
- `schemaVersion` 是唯一兼容性判断依据，高版本直接拒绝
- **导入前自动快照**到 `files/backups/auto-<时间戳>.json`，保留最近 20 份；写库失败自动还原
- 导入与回滚都会恢复 `settings.lang`

### 新克隆能构建（曾经不能）

`app/build.gradle.kts` 里：

```kotlin
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseSigning = keystorePropsFile.exists() && keystoreProps.getProperty("storeFile") != null
```

没有 `keystore/` 时 `signingConfig = null`，产出未签名包而不是抛 `FileNotFoundException`。**别把这段改回去。**

---

## 五、发布流程

```powershell
cd D:\Documents\deepseek-harness\default-workspace\cooking-timer
.\scripts\snapshot.ps1                              # 高危修改前先快照
D:/python/python.exe scripts/publish.py --public --yes
```

`publish.py` 的 8 个步骤，**每步执行前都先跑 `scan_secrets.py`，命中即中止**：

1. 检查仓库是否存在
2. 不存在则创建（`/user/repos` 或 `/orgs/{owner}/repos`）
3. **同步版本号到站点与文档**（`sync_version.py`，先改盘，再与源码一起提交）
4. `git add/commit/push`
5. 打 tag 并推送
6. 建 Release，正文优先读 `notes/<tag>.md`，上传 `StoveSide-<版本>.apk` + `SHA256SUMS`
7. **更新仓库 About**（读 `notes/about.txt`）
8. 部署 `docs/` 到 `gh-pages`

支持 `--dry-run`（只打印动作，不发网络请求）与 `--skip-pages`。

### 版本号只有一个真相来源

`app/build.gradle.kts` 的 `versionName` / `versionCode`。`scripts/sync_version.py` 会把它同步到：

- `docs/index.html` 的「版本 x.y.z」
- `docs/en/index.html` 的「Version x.y.z」
- README / README_EN 的版本表述

**这个脚本是补上一个真实事故的**：1.0.1、1.0.2 两版发出去后，站点首页还写着「版本 1.0.0」，
因为那句是手写死在 HTML 里的。现在发布流程第 3 步强制同步，同步结果随源码一起提交。

### Release 附件的命名（有坑）

要求是 `StoveSide-<版本>.apk`（连字符），因为 **GitHub 上传附件时会把文件名里的空格替换成点**：

| 尝试 | 结果 |
|---|---|
| `StoveSide 1.0.2.apk` | 被存成 `StoveSide.1.0.2.apk` |
| 用不换行空格 U+00A0 | HTTP 422 拒绝 |

所以实际用**连字符**：`StoveSide-1.0.2.apk`。`publish.py` 的 `asset_name()` 就是这个规则，别改回空格。

### 双语 Release 说明

`notes/v<版本>.md`，格式参考 `Levanll/InventoryLedger` 的 `notes/v1.3.1.md`：

```
## 本次更新
概述一段
### 新增 / 修复 / 下载（表格）
升级提示
---
<!-- English version below / 以下为英文版 -->
## What's new
... 完全对称的英文段 ...
```

**中文在前，两段章节必须对称，每段自带下载表。** 新版本发版前先写这个文件。

### About

`notes/about.txt`，185 字符，中英双语中文在前。GitHub description 上限 350 字符，`step_about` 会截断。

---

## 六、备份与回滚

```powershell
# 高危修改前快照
.\scripts\snapshot.ps1

# 预览还原（默认不落盘）
.\scripts\snapshot.ps1 -Restore F:\deepseek-harness\_archive\20261112-1534-cooktimer

# 实际还原
.\scripts\snapshot.ps1 -Restore <快照目录> -Force
```

快照 zip 排除 `build/`、`.gradle/`、`.venv/`，同时记录 `HEAD.txt`（HEAD / 分支 / 工作区是否干净）。
脚本带 UTF-8 BOM——PowerShell 5.1 对无 BOM 的中文脚本会按 GBK 解码并报语法错误。**编辑它时保留 BOM**。

---

## 七、文档与站点

| 文件 | 作用 |
|---|---|
| `README.md` / `README_EN.md` | 两份独立完整文档，不是摘要 |
| `docs/index.html` + `docs/en/index.html` | Pages 站点，共用 `docs/style.css` |
| `docs/设计方案.md` | 数据模型、计时调度、解析策略 |
| `docs/页面规格.md` | 三个页面的信息层级与交互约束 |
| `docs/交付报告.md` | 上线结果与审查修复记录 |
| `notes/` | Release 说明与 About 源文件 |
| `CHANGELOG.md` | 目标是 Keep a Changelog + 中英双语；**目前残缺，见第二节「已知风险」** |

### 截图按语言分目录

```
docs/images/zh/{cooks,recipes,note,settings}.png   ← 中文界面 + 中文内容
docs/images/en/{cooks,recipes,note,settings}.png   ← English UI + English content
docs/images/hero-banner-{zh,en}.png                ← README 首图
```

**中文 README 只能用 zh/，英文只能用 en/**。曾经因为模拟器语言没换回来，中文 README 里混进了英文界面截图。

### 重新生成截图

1. 准备两份演示库（内容也要对应语言，不能只换界面）：

```bash
D:/python/python.exe scripts/make_demo_db.py   # 生成 shots/db_zh.db 与 shots/db_en.db
```

2. 推库 + 设语言 + 重启 + 截图：

```bash
ADB=/f/android-dev/sdk/platform-tools/adb.exe
$ADB push shots/db_zh.db /data/local/tmp/d.db
$ADB shell "run-as cn.levanll.stoveside.debug sh -c 'rm -f databases/stoveside.db*; cat /data/local/tmp/d.db > databases/stoveside.db'"
$ADB shell "run-as cn.levanll.stoveside.debug sh -c 'printf \"<map><string name=\"lang\">zh</string></map>\" > shared_prefs/stoveside_prefs.xml'"
$ADB shell am start -n cn.levanll.stoveside.debug/cn.levanll.stoveside.MainActivity
sleep 8
$ADB exec-out screencap -p > shots/zh-cooks.png
```

3. 首图由 `scripts/make_banner.py` 生成（PIL，1280×420，奶油底 + 陶土橙 + 右侧三行实例卡片）。

### 本地预览 Markdown

```bash
D:/python/python.exe scripts/preview_md.py README.md _pv-zh.html
```

**必须在项目根目录生成**，否则 `docs/images/...` 相对路径解析不到。预览文件已在 `.gitignore`。

### Pages 部署有 CDN 延迟

推送后立刻访问新图片可能 404，等 30–60 秒再试。不是配置问题。

---

## 八、视觉规范（三种媒介必须一致）

| 用途 | 色值 |
|---|---|
| 奶油底（背景） | `#FDF8F3` |
| 卡片底 | `#FFFDFA` |
| 陶土橙（主色） | `#C96F4A` |
| 深陶土 | `#9C4F2F` |
| 鼠尾草绿（点缀） | `#7E9B80` |
| 墨色（正文） | `#3D3229` |
| 浅灰褐（次要文字） | `#7A6A5D` |
| 深色模式底 | `#211A16` |

- 卡片圆角 16–18dp，胶囊按钮 999dp，卡片内边距 14dp，屏幕边距 16dp
- 图标是「碗 + 两缕热气」的极简图形，应用图标 / README 首图 / Pages 共用同一套
- 深色模式在 Compose 用 `darkColorScheme`，Pages 用 `prefers-color-scheme`

---

## 九、踩过的坑（别再踩）

| 现象 | 原因 | 处理 |
|---|---|---|
| Gradle 报 JVM `agent library failed to init: instrument` | 本机 Kotlin 编译 worker 的 agent 冲突 | 加 `-Pkotlin.compiler.execution.strategy=in-process` |
| 写 Kotlin 源码后编译报 `Illegal escape` | 用脚本写文件时反斜杠被多转义了一层 | 写正则改用手写/逐行核对，`Regex("^\\s*")` 在源码里应是两个反斜杠 |
| `git add -A` 报 `unable to stat CON` | git 的凭据提示把 "Username for..." 写进了名为 `CON` 的文件（Windows 保留名） | 该文件已加入 `.gitignore`，**不要删这行** |
| `git push` 卡在 `read Username` | token 未注入 | `publish.py` 用 `GIT_CONFIG` 凭据 helper 注入，token 不进 URL、不进进程参数、不落盘 |
| Release 附件名带点 `StoveSide.1.0.2.apk` | 上传时 GitHub 把空格替换成点 | 统一用连字符 |
| 站点首页版本号不跟着变 | 手写在 HTML 里 | `sync_version.py`，已进发布流程第 3 步（提交前） |
| 英文长菜名把胶囊挤成竖条 | `Row` 放不下 | 已改 `horizontalScroll` |
| 英文步骤标题和时间贴在一起 | 缺间距 | 时间加了 8dp 左间距，标题/备注限 2 行 + 省略号 |
| 工作流写成 `${ ... }}` | 缺一个 `$`，GitHub 只认 `${{ }}` | 已修 13 处；改完用 `python -c "import yaml;yaml.safe_load(open(...))"` 验一遍 |

---

## 十、下一步可以做什么

按优先级：

1. **修复 CHANGELOG（先做这个）** — 从 `git show 8458ef0:CHANGELOG.md` 取回 `[Unreleased]` 与 `[1.0.0]` 两节，把 H1 提到文件最前，版本按倒序排（Unreleased → 1.0.2 → 1.0.0），底部补回 compare / tag 链接。顺带在 `.gitignore` 补 `__pycache__/` 与 `*.pyc`。这是已确认的实质回退，不是可选项
2. **让 CI 真实跑一次** — 在 GitHub 上手动触发 `workflow_dispatch`，输入 tag，确认 `${{ }}` 修正确实生效。这是唯一还没被验证的环节
3. **真机验证** — 目前只在模拟器上跑过
4. **给导入恢复语言加测试** — 需要 Robolectric 或 instrumented test
5. **A5 内置示例食谱 / A6 桌面小组件** — 用户当时没勾选，要做先确认

---

## 十一、提交信息约定

- `feat(版本):` 新功能
- `fix(范围):` 修 bug（范围如 `ci` / `release` / `1.0.2`）
- `docs:` 文档
- `chore:` 杂项

历史提交示例见 `git log --oneline`。
