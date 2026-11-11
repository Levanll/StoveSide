# 灶暖 · StoveSide

> 几口锅同时开火也不乱：多流程并行计时、每步留备注、同一份食谱的多次实操分开记录。

**简体中文** ｜ [English](README_EN.md)

[下载最新版本](https://github.com/Levanll/StoveSide/releases/latest) ｜ [GitHub 仓库](https://github.com/Levanll/StoveSide)

---

## 这是什么

灶暖是一个专注「做饭当下」的 Android 计时器。它不管理你收藏了多少菜谱，而是解决真正手忙脚乱的那二十分钟：炖汤、蒸鱼、炒菜、焖饭同时在跑，每一步的火候和临时改动都得记得住。

把食谱拆成步骤，每一步带自己的计时；一次下厨开一个「烹饪实例」，同一份食谱可以同时开多个实例、各自独立推进。做饭时随手写下的备注留在实例里，不会污染原食谱模板。

## 截图

| 进行中的烹饪 | 食谱库 | 设置 |
| --- | --- | --- |
| ![多流程并行计时界面](docs/images/screenshot-cooks.png) | ![食谱与步骤管理界面](docs/images/screenshot-recipes.png) | ![设置与导出导入界面](docs/images/screenshot-settings.png) |

## 功能

- **多流程并行计时**：同时跑多个烹饪流程，每个步骤独立计时，互不阻塞；剩余时间统一呈现，到点即提醒。
- **步骤备注**：每个步骤都可以写备注——火力大小、临时替换的配料、下次要改的地方。
- **同食谱多实例追踪**：同一份食谱可以同时开出多个烹饪实例，各自记录进度与备注，适合一次做两锅或重复试做。
- **粘贴文本自动拆分步骤**：把一段菜谱文本粘进来，自动按行与序号切成步骤，省掉逐条手打的功夫。
- **中英双语**：界面跟随系统语言在中文与英文之间切换。
- **导出与导入**：数据可整体导出备份，换机或重装后一键导回。
- **应用内更新**：启动后检查新版本，可直接在应用内下载并安装升级。

## 数据模型

模板与实例严格分离，这是整个应用的核心设计：

```
Recipe（食谱模板） ──< Step（步骤）
Recipe（食谱模板） ──< Cook（烹饪实例） ──< CookStep（实例步骤记录）
```

- **Recipe → Step**：模板只回答「这道菜怎么做」，可被反复使用。
- **Recipe → Cook → CookStep**：每次真正下厨开一个 Cook，步骤状态与备注写在 CookStep 上。
- **备注不回写模板**：实例中的任何改动都停留在实例内，模板保持干净。

## 界面与配色

整体是奶油底加陶土橙的暖调，圆角克制的卡片式布局，图标是「碗 + 两缕热气」的极简图形。

| 用途 | 色值 |
| --- | --- |
| 奶油底 | `#FDF8F3` |
| 卡片底 | `#FFFDFA` |
| 陶土橙（主色） | `#C96F4A` |
| 深陶土 | `#9C4F2F` |
| 鼠尾草绿 | `#7E9B80` |
| 墨色文字 | `#3D3229` |
| 浅灰褐（次要文字） | `#7A6A5D` |
| 深色模式底色 | `#211A16` |

卡片圆角统一在 16–18dp。

## 安装

- 系统要求：Android 7.0（API 24）及以上。
- 从 [Releases](https://github.com/Levanll/StoveSide/releases/latest) 下载最新 APK，安装时允许「安装未知来源应用」。
- 已安装旧版本时可直接覆盖安装，数据保留；跨版本升级建议先在设置里导出一份备份。

## 技术栈

| 项 | 说明 |
| --- | --- |
| 语言 | Kotlin |
| 界面 | Jetpack Compose（Material 3） |
| 存储 | Room 2.6.1 |
| 包名 | `cn.levanll.stoveside` |
| minSdk / targetSdk | 24 / 34 |
| 当前版本 | 1.0.0（versionCode 1） |

## 从源码构建

仓库自带 `scripts/build.ps1`，默认任务为 `assembleRelease`，可传入其它任务：

```powershell
./scripts/build.ps1 -Task assembleDebug
```

脚本会显式指定 Gradle 用户目录与 Gradle 发行版位置，让缓存和构建产物不落到系统盘；它以 `--offline --no-daemon` 调用 Gradle，因此首次构建前需要依赖已在本地缓存就绪。

签名配置从 `keystore/keystore.properties` 读取，该目录已在 `.gitignore` 中排除，不会进入版本库。

## 项目结构

```
cooking-timer/
├─ app/src/main/java/cn/levanll/stoveside/
│  ├─ data/       Room 实体、DAO、数据库与仓库
│  ├─ domain/     时间文本等纯逻辑
│  ├─ service/    前台计时服务、闹铃与通知
│  ├─ backup/     导出与导入
│  ├─ update/     应用内更新与安装
│  ├─ i18n/       中英文案
│  └─ ui/         Compose 界面与主题
├─ docs/          GitHub Pages 站点与截图
└─ scripts/       构建与图标脚本
```

## 版本记录

见 [CHANGELOG.md](CHANGELOG.md)。当前版本 1.0.0。

## 相关链接

- 项目主页：https://github.com/Levanll/StoveSide
- 下载地址：https://github.com/Levanll/StoveSide/releases/latest
- 问题反馈：https://github.com/Levanll/StoveSide/issues
