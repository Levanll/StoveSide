# 安全策略 / Security Policy

StoveSide（灶暖）是一个**纯本地**的厨房计时与菜谱管理应用。本文说明漏洞报告方式、数据处理边界，以及发布流程中的脱敏要求。

StoveSide is a **fully local** kitchen timer and recipe manager. This document covers how to report vulnerabilities, what the app does with your data, and how releases are scrubbed.

---

## 1. 报告漏洞 / Reporting a Vulnerability

**请不要通过公开 Issue 报告安全问题。**

**Please do not report security issues through public issues.**

| 渠道 / Channel | 说明 / Notes |
| --- | --- |
| GitHub Security Advisories | 在 <https://github.com/Levanll/StoveSide>/security/advisories/new 私下提交（推荐） |
| 邮件 / Email | levanll@users.noreply.github.com，标题前缀 `[SECURITY] StoveSide` |

报告时请包含：受影响版本（versionName / versionCode）、复现步骤、影响范围，以及可行的修复建议。
Please include the affected version (versionName / versionCode), reproduction steps, impact, and any suggested fix.

**响应承诺 / Response targets**

- 3 个工作日内确认收到 / acknowledge within 3 business days
- 30 天内给出修复或缓解方案 / fix or mitigation within 30 days
- 修复发布后在 CHANGELOG 与 Release Notes 中致谢（除非你要求匿名）/ credit in CHANGELOG and release notes unless you ask to stay anonymous

请给我们合理的修复窗口，不要提前公开细节。
Please allow a reasonable disclosure window before publishing details.

---

## 2. 数据处理说明 / Data Handling

### 纯本地存储 / Local-only storage

- 计时器、菜谱、烹饪记录全部存放在设备本地的 Room/SQLite 数据库内，**不上传任何服务器**。
  Timers, recipes, and cooking logs live in a local Room/SQLite database. **Nothing is uploaded to any server.**
- 应用不含账号体系，不收集设备标识，不接入统计分析或广告 SDK。
  There is no account system, no device-identifier collection, and no analytics or ad SDK.
- 备份文件由用户主动导出到你选择的位置（含系统文件选择器），应用不会自行外发。
  Backups are exported only on your explicit action to a location you choose.

### 唯一的外部网络访问 / The only outbound request

- 更新检查会向 GitHub 公共 API 请求本项目的最新 Release 信息，用于提示新版本。该请求只包含标准 HTTP 头，不携带任何用户数据、菜谱内容或设备标识。
  The update checker queries the public GitHub API for this project's latest release. It sends only standard HTTP headers — no user data, recipe content, or device identifiers.
- 你可以在系统设置中断网运行，应用的核心功能不受影响。
  The app works fully offline; blocking network access breaks only the update prompt.

### 权限 / Permissions

- 通知权限：用于计时到点提醒。
  Notification permission: timer alerts.
- 存储 / 文件选择器权限：仅在导入导出备份时按需使用。
  File access: used only for backup import/export.
- 不请求通讯录、定位、相机、麦克风等无关权限。
  No contacts, location, camera, or microphone access is requested.

---

## 3. 发布脱敏流程 / Release Scrubbing

所有公开发布物在离开构建机之前都要经过以下检查。任何一步失败都会中止发布。
Every public release goes through the checks below before leaving the build machine. Any failing step aborts the release.

### 3.1 仓库入库卫生 / Repository hygiene

以下内容**永不入库**（见 .gitignore）/ **never committed** (see .gitignore):

- `keystore/`（`release.jks`、`keystore.properties`）—— release 签名密钥与口令
- `local.properties`、`build/`、`.gradle/`、`*.apk`、`*.aab`
- 任何形式的 token 文件

签名口令只存在于本机 `keystore/keystore.properties` 与 GitHub Actions 的 secrets（KEYSTORE_BASE64、KEYSTORE_PASSWORD、KEY_ALIAS、KEY_PASSWORD），不进入仓库、不进入日志。

### 3.2 发布前扫描 / Pre-release scan

`scripts/publish.py` 的**每一步执行前**都会先运行 `scripts/scan_secrets.py`，命中以下任意模式即以退出码 1 中止发布：

- `ghp_` 形式的 GitHub classic token
- `github_pat_` 形式的 fine-grained token
- `-----BEGIN ... PRIVATE KEY-----` 私钥块
- `storePassword` / `keyPassword` 字段上填了真实口令（变量引用、占位符不算）
- 名为 `.gh_token` 的凭据文件

扫描输出**只包含模式名、文件路径和行号，绝不打印命中的真实值**。
The scanner prints only pattern names, file paths, and line numbers — never the matched value.

手动执行 / Run manually:

```bash
python scripts/scan_secrets.py                    # 扫描将要发布的内容
python scripts/scan_secrets.py --include-ignored  # 连被忽略的文件一起审计
```

### 3.3 构建产物 / Build artifacts

- Release APK 由 `.github/workflows/build-release.yml` 在 GitHub Actions 中构建，构建环境不含本地 keystore。
  Release APKs are built in GitHub Actions; the runner never sees the local keystore.
- 仓库未配置签名 secrets 时，流水线只产出**未签名** APK 并在日志中明确告警，产物不能直接安装分发。
  Without signing secrets the pipeline produces an **unsigned** APK, warns loudly in the log, and the artifact is not installable or distributable.
- 每个 Release 同时附上 `SHA256SUMS`，请下载后校验：
  Every release ships `SHA256SUMS`; verify after download:

```bash
sha256sum -c SHA256SUMS
```

### 3.4 凭证处理 / Credential handling

- 发布脚本只从环境变量 `GITHUB_TOKEN` 或本地 token 文件读取凭据，日志中**只打印长度与前缀类型，绝不打印内容**。
  The publish script reads credentials only from `GITHUB_TOKEN` or a local token file, and logs **only the length and prefix type — never the value**.
- 一旦怀疑密钥泄漏：立即在 GitHub 撤销 token、轮换 keystore、撤下对应 Release 资产，并按第 1 节渠道通知维护者。
  If a credential leak is suspected: revoke the token, rotate the keystore, pull the affected release assets, and notify the maintainers via the channel in section 1.

---

## 4. 支持的版本 / Supported Versions

只对最新 Release 提供安全修复。
Security fixes are provided for the latest release only.

| 版本 / Version | 支持 / Supported |
| --- | --- |
| 最新 Release / Latest release | 支持 / Yes |
| 更早版本 / Older releases | 不支持，请升级 / No, please upgrade |

---

## 5. 覆盖范围 / Scope

本策略覆盖 StoveSide 应用本体、发布流水线（`.github/workflows/`）与发布脚本（`scripts/`）。第三方依赖的漏洞请同时参考其上游公告。
This policy covers the StoveSide app, its release workflow, and release scripts. For third-party dependencies, also consult the upstream advisories.
