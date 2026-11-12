# 更新日志 · Changelog

本文件遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。
每条变更同时给出中文与英文：中文 / English.

## [Unreleased]

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

[Unreleased]: https://github.com/Levanll/StoveSide/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Levanll/StoveSide/releases/tag/v1.0.0
