# 更新日志 · Changelog

本文件遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。
每条变更同时给出中文与英文：中文 / English.

## [Unreleased]

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
