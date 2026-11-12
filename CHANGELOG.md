# 更新日志 · Changelog

本文件遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。
每条变更同时给出中文与英文：中文 / English.

## [Unreleased]

## [1.0.4] - 2026-11-12

### Added · 新增

- **持续到点提醒**：到点后持续响铃 + 震动，直到用户点通知上的「知道了」或在「在煮」页点「+1 分钟」续上。此前只是一次性通知，一两秒就结束，人不在手机边就会错过。
  Persistent alarms that ring and vibrate until dismissed, instead of a one-shot notification that is easy to miss.
- **提醒方式可配置**：设置页三档——响铃 + 震动（默认）、仅震动、仅通知，并附「试听」入口。
  Selectable alarm style in Settings: sound + vibrate (default), vibrate only, notification only, with a test button.

### Fixed · 修复

- **「加一分钟」加不满一分钟**：到点后 `dueAt` 停在过去且越走越远，原先以它为基准加 60 秒，加上的时间被已过期时长吃掉——归零 40 秒后点只剩 20 秒，归零 60 秒后点等于没反应。改为 `max(now, dueAt)` 起算，并抽出 `TimeText.extendDue` 加 6 条回归测试。
  "Add one minute" added less than a minute: once overdue, the stale deadline ate most of the added time. Now computed from `max(now, dueAt)`, extracted into `TimeText.extendDue` with six regression tests.
- **「+1」按钮不显眼**：由无边框小字改为描边按钮，文案为「+1 分钟 / +1 min」。
  The "+1" control was bare text; it is now an outlined button labelled "+1 min".
- **到期后无法区分已过多久**：归零步骤额外显示「已到点 / Overdue」。
  Overdue steps now show an "Overdue" label instead of looking identical to the moment they hit zero.
## [1.0.3] - 2026-11-12

### Fixed · 修复

- **新建食谱时最后几行被键盘挡住**：`enableEdgeToEdge()` 之后窗口不再随键盘缩放，界面又没有任何一处消费 IME inset，键盘弹起时内容原地不动。编辑页现在正确让出键盘高度。
  The last lines were hidden behind the keyboard on the new-recipe screen: after edge-to-edge the window stops resizing, and nothing consumed the IME inset. The editor now reserves the keyboard height.
- **光标选区手柄浮在键盘上方**：同一根因，手柄按光标位置绘制而光标行正被键盘盖住。现在手柄随文字上移，不再越界。
  The selection handle floated above the keyboard for the same reason: it is drawn at the cursor position while that line sat under the keyboard. It now moves up with the text.
- **输入框获得焦点后不会自动滚入可视区**：菜名、整体备注、步骤标题、分钟四个输入框现在获焦即滚到可见位置。
  Text fields did not scroll into view on focus; the name, notes, step title and minutes fields now do.
- **粘贴区改为独立底部弹层**：原先内嵌在列表里受滚动容器约束，现在输入框独占一屏、识别结果单独滚动。
  The paste area is now a modal bottom sheet instead of an inline list item, giving the input its own space and letting the parsed result scroll separately.
## [1.0.2] - 2026-11-12

### 文档改版

README / About / Release 排版对齐 `Levanll/InventoryLedger` 的既有约定：

- README 首图改为按同一构图生成的 `docs/images/hero-banner-zh.png` 与 `hero-banner-en.png`（左侧标题+标签组+脚注，右侧主题卡片；配色沿用灶暖的奶油底 + 陶土橙）
- README 结构改为：10 秒看懂 / 核心卖点 / 快速开始 / 界面预览 / 能力一览 / 为什么是模板+实例 / 项目结构 / 技术栈 / 从源码构建 / 隐私速览 / 版本记录 / 文档 / 贡献 / 许可证
- About 改为中英双语、中文在前（`notes/about.txt`，由发布脚本同步到仓库描述）
- Release 改为中英双语、中文在前，格式参考 `notes/v1.3.1.md`：本次更新 / 修复 / 下载表 → 分隔线 → What's new / Fixes / Downloads

### Fixed / 修复

- **CI 表达式**：GitHub Actions 工作流里 13 处模板表达式缺少第一个 $（写成 ${ ... }}），流水线必然启动失败——已全部改为 ${{ ... }}。
- **实例备注**：「在煮」页每个步骤现在可直接点开写这次的做法，改动只留在本次实例，不回写模板。
- **前台服务常驻**：冷启动不再无条件拉起计时服务；只有确实存在进行中的计时才启动，空闲约 3 秒后自行停止并释放通知栏。
- **「一个半小时」解析**：「一个半小时 / 1个半小时」此前被算成 30 分钟，现正确得到 90 分钟；「半小时」仍为 30 分钟。
- **英文 h 单位**：bake 1h、simmer 1.5h 现在能正确解析（此前注释声称支持但正则里没有）。
- **新克隆无法配置 Gradle**：缺少 keystore/keystore.properties 时不再抛 FileNotFoundException，改为产出未签名包；克隆后可直接 gradle tasks / assembleRelease。
- **导入丢失语言设置**：导入备份与回滚快照现在都会恢复 settings.lang，不再是只写不读的字段。

### Tests

- 新增「一个半小时」与英文 h 两组断言，修正「两小时半」应为 9000 秒。

## [1.0.0] - 2026-11-12

首个公开版本 · First public release.

### Added · 新增

- 多流程并行烹饪计时：同时进行多个烹饪流程，每个步骤独立计时、互不阻塞；全程展示剩余时间并在到点提醒。
  Parallel multi-flow cooking timers: run several cooks at once, each step with its own countdown, none blocking the others; remaining time stays visible and an alert fires when a step is due.
- 步骤备注：每个步骤都可写备注，用于记录火力、临时替换与下次改进项。
  Per-step notes on every step, for heat level, on-the-fly substitutions and reminders for next time.
- 同一食谱的多实例追踪：模板与实例分离，同一份食谱可同时开启多个烹饪实例，各自记录进度与备注，实例备注不回写模板。
  Multiple instances per recipe: templates and instances are separated, so one recipe can run as several cook instances with independent progress and notes, and instance notes never write back to the template.
- 粘贴文本自动拆分步骤：粘贴一段菜谱文本即可按行与序号自动切分为步骤。
  Paste-to-steps parsing: pasting a block of recipe text splits it into steps by line breaks and numbering.
- 中英双语界面：跟随系统语言在中文与英文之间切换。
  Chinese and English interface that follows the system language.
- 导出与导入：数据可整体导出备份，并可在换机或重装后导回。
  Export and import: the whole data set can be exported as a backup and imported back after a reinstall or a device change.
- 应用内更新：启动后检查新版本，可在应用内下载并安装更新。
  In-app updates: a newer build is checked on launch and can be downloaded and installed from inside the app.
- 完整视觉规范：奶油底配陶土橙主色，卡片圆角 16–18dp，提供深色模式。
  A complete visual language: cream base with clay orange as primary, 16–18dp card corners, and a dark mode.

### Known limitations · 已知限制

- 最低支持 Android 7.0（API 24）。
  Requires Android 7.0 (API 24) or newer.
- 应用内更新依赖 GitHub Releases 的发布产物。
  In-app updates rely on the assets published in GitHub Releases.

[Unreleased]: https://github.com/Levanll/StoveSide/compare/v1.0.4...HEAD
[1.0.4]: https://github.com/Levanll/StoveSide/releases/tag/v1.0.4
[1.0.3]: https://github.com/Levanll/StoveSide/releases/tag/v1.0.3
[1.0.2]: https://github.com/Levanll/StoveSide/releases/tag/v1.0.2
[1.0.0]: https://github.com/Levanll/StoveSide/releases/tag/v1.0.0
