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

---

# 更新日志 · Changelog
