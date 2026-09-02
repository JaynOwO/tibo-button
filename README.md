# ⚡ Tibo Button

[![Latest release](https://img.shields.io/github/v/release/JaynOwO/tibo-button?display_name=tag)](https://github.com/JaynOwO/tibo-button/releases/latest)
[![Android CI](https://github.com/JaynOwO/tibo-button/actions/workflows/build-apk.yml/badge.svg)](https://github.com/JaynOwO/tibo-button/actions/workflows/build-apk.yml)
[![License](https://img.shields.io/github/license/JaynOwO/tibo-button)](LICENSE)

[简体中文](#简体中文) · [English](#english)

## 简体中文

Tibo Button 是一个非官方 Android 应用和桌面 Widget，用于查看 Reset Beacon 提供的公开 Codex / ChatGPT Work 共享额度重置信号。

> **独立项目声明：** 本项目与 OpenAI、ChatGPT、Codex、Reset Beacon、Sound Media 或 Thibault “Tibo” Sottiaux 没有隶属、赞助、认可或其他官方关系。

### 当前稳定版：v0.3.2

[下载 GitHub Releases 中的最新签名 APK](https://github.com/JaynOwO/tibo-button/releases/latest)

> 当前稳定版仍为 v0.3.2；`main` 正在开发 v0.3.3 的 Samsung / One UI Widget 兼容性修复。设备体验验收清单见 [docs/DEVICE-QA.md](docs/DEVICE-QA.md)。

### 功能概览

| 部分 | 内容 |
| --- | --- |
| App 详情页 | 当前重置状态、下次重置时间或来源说明、24h / 48h 概率、上次重置时间、数据来源和更新时间 |
| Widget | 四种选择：标准 4×2、标准 2×2、Pulse Orb 脉冲核心和 Command Deck 指挥台；均显示真实状态与更新时间 |
| Reset Pulse | 使用真实的最近最多 7 次已完成共享重置事件，计算近 7 天次数、平均间隔和近期连击 |
| 刷新 | App 和 Widget 均提供立即刷新；刷新期间显示确定的 loading 状态和“正在刷新…”反馈 |
| 通知 | scheduled / confirmed 和 completed reset 默认通知；“很可能”通知可选且默认关闭，并对相同事件去重 |
| 更新器 | 只检查公开的稳定版 GitHub Release；下载和安装始终需要用户确认，并校验版本、包名、SHA-256 和签名 |

### Widget 选择

- **标准 4×2：** 显示 TIBO RESET RADAR、刷新入口、下次重置、确定性状态、24h / 48h 概率、上次重置、近 7 天次数、Reset Beacon 来源和更新时间。
- **标准 2×2：** 优先显示当前状态、下次重置、24h / 48h 概率和更新时间，避免在有限空间中塞入难以阅读的长文本。
- **Pulse Orb · 脉冲核心：** 用静态状态光环突出当前信号，同时保留下次重置、24h / 48h 概率、上次重置和来源更新时间。
- **Command Deck · 指挥台：** 使用紧凑的控制台布局突出下次重置，并列显示 24h / 48h 概率、上次重置和近 7 天统计。

四种 Widget 都使用不透明的深色 Surface 和稳定的 loading 状态切换，不依赖 Launcher 持续执行动画，也不会增加高频自刷新。Pulse Orb 的光环只表达当前状态颜色，不代表额外预测数据。

### Reset Pulse 统计规则

Reset Pulse 只做可解释的历史摘要，不承诺下一次重置时间：

- 只统计 Reset Beacon 标记为 completed 的事件；
- 只统计 broad / all-user 范围的事件；
- 最多展示最近 7 条符合条件的事件；
- “近 7 天次数”指最近七天内公布的符合条件的事件数；
- “近期连击”从最新事件开始计算，相邻事件间隔不超过 **72 小时**；
- 平均间隔根据当前展示的符合条件事件计算；
- 少于两条历史记录时显示明确的空状态，不绘制误导性趋势。

### 安装

1. 打开仓库的 [Releases](https://github.com/JaynOwO/tibo-button/releases) 页面。
2. 下载 TiboButton-vX.Y.Z.apk，当前稳定版为 TiboButton-v0.3.2.apk。
3. 可选：使用同一 Release 中的 SHA256SUMS.txt 校验文件完整性。
4. 在 Android 上安装 APK，并在 Launcher 的 Widget 选择器中选择 Tibo Button 的任意一种布局。

公开 Release APK 使用同一项目签名密钥，以便未来版本覆盖更新。Debug APK 是开发构建，可能无法覆盖由其他构建环境签名的 APK。

### 应用内更新

应用会检查 JaynOwO/tibo-button 的公开稳定版 GitHub Release，并只提供用户确认后的更新。自动检查不会自动下载或安装。

在交给 Android 安装器前，更新器会验证：

- 稳定的 vX.Y.Z Tag 和对应的精确 APK asset；
- GitHub 提供的 SHA-256 及 SHA256SUMS.txt；
- com.tibobutton.app 包名、versionName 和 versionCode；
- 与已安装应用匹配的签名证书。

下载文件仅保存于应用私有的 cache/updates/，并通过受限的 FileProvider 交给 Android。系统安装确认和未知来源权限流程始终保留。验证失败时会删除文件并停止安装；不需要 GitHub 登录、Token 或 API key。

### 数据来源、准确性与隐私

应用读取 Reset Beacon 的公开 JSON API：

- GET https://resetbeacon.com/api/forecast
- GET https://resetbeacon.com/api/history

当来源没有可靠的机器可读截止时间时，应用不会从模糊文字推算精确时刻，而会明确显示“已排期 · 时间见来源”等说明。过期或不可用的预测会被标记，避免把旧概率伪装成当前数据。

Reset Beacon 的数据只用于这个公开状态摘要，并在 App / Widget 中保留来源和回链。公开帖子引用或摘要仍归原作者所有；项目不会把完整帖子归为自己的内容。

Tibo Button 不要求 OpenAI 登录、X 登录、API key，也不读取你的 Codex 使用量。Widget 状态、历史摘要、通知去重指纹和偏好设置保存在本地 Android 设备；网络请求仅访问 Reset Beacon 的公开 GET 接口。

### 开发构建

仓库包含普通的 [Build Android APK workflow](.github/workflows/build-apk.yml)：

1. 打开 **Actions → Build Android APK**。
2. 运行 workflow。
3. 下载 TiboButton-debug-apk artifact。
4. 解压 app-debug.apk 后安装到 Android 设备。

本地验证命令：

~~~bash
gradle test
gradle :app:assembleDebug
~~~

### 发布与许可

签名发布流程见 [docs/RELEASING.md](docs/RELEASING.md)。发布签名凭据只保存在 GitHub Actions Secrets 中，不提交到仓库。

源代码使用 [MIT License](LICENSE)。第三方服务、商标、公开帖子文字及其他第三方材料不由 MIT License 授权，详见 [NOTICE.md](NOTICE.md)。

## English

Tibo Button is an unofficial Android companion app and home-screen widget for viewing public Codex / ChatGPT Work shared-usage reset signals reported by Reset Beacon.

> **Independent project:** This project is not affiliated with, sponsored by, endorsed by, or an official product of OpenAI, ChatGPT, Codex, Reset Beacon, Sound Media, or Thibault “Tibo” Sottiaux.

### Current stable release: v0.3.2

[Download the latest signed APK from GitHub Releases](https://github.com/JaynOwO/tibo-button/releases/latest)

> The current stable release remains v0.3.2; `main` is developing the v0.3.3 Samsung / One UI widget-compatibility fix. See [docs/DEVICE-QA.md](docs/DEVICE-QA.md) for the device experience checklist.

### Feature overview

| Area | Details |
| --- | --- |
| App details | Current reset status, next reset time or source wording, 24h / 48h probabilities, last reset, data source, and update time |
| Widgets | Four choices: standard 4×2, standard 2×2, Pulse Orb, and Command Deck, all using the real status and update time |
| Reset Pulse | Uses up to seven real recent completed shared-reset events to calculate a trailing 7-day count, average interval, and recent streak |
| Refresh | Immediate refresh in the app and widgets with a deterministic loading state and “正在刷新…” feedback |
| Notifications | Scheduled / confirmed and completed resets are enabled by default; “very likely” is optional and off by default, with event deduplication |
| Updater | Checks only the public stable GitHub Release; download and installation always require user confirmation and verify version, package, SHA-256, and signing certificate |

### Widget choices

- **Standard 4×2:** TIBO RESET RADAR, refresh control, next reset, certainty status, 24h / 48h probabilities, last reset, trailing 7-day count, Reset Beacon source, and update time.
- **Standard 2×2:** Prioritizes current status, next reset, 24h / 48h probabilities, and update time so the compact layout remains readable.
- **Pulse Orb:** A static state-colored halo emphasizes the current signal while retaining next reset, 24h / 48h probabilities, last reset, and source/update time.
- **Command Deck:** A compact control-deck layout emphasizes the next reset and places 24h / 48h probabilities, last reset, and trailing statistics in separate metric areas.

All four widgets use an opaque dark surface and a deterministic loading-state swap. They do not depend on continuous launcher-side animation or add high-frequency background refreshes. The Pulse Orb halo communicates the current state color only; it does not add or imply forecast data.

### Reset Pulse rules

Reset Pulse is an explainable activity summary, not a promise about the next reset:

- only Reset Beacon events marked completed are counted;
- only broad / all-user scopes are counted;
- at most the latest seven qualifying events are shown;
- “trailing 7-day count” means qualifying events announced during the last seven days;
- “recent streak” starts at the newest event and continues while each adjacent event is no more than **72 hours** apart;
- average interval is calculated across the displayed qualifying events;
- fewer than two history records produce an explicit empty state rather than a misleading trend.

### Install

1. Open the repository’s [Releases](https://github.com/JaynOwO/tibo-button/releases) page.
2. Download TiboButton-vX.Y.Z.apk; the current stable release is TiboButton-v0.3.2.apk.
3. Optionally verify the file against SHA256SUMS.txt from the same Release.
4. Install the APK on Android and choose any Tibo Button layout from the Launcher widget picker.

Public release APKs use one stable project signing key so future versions can upgrade an existing install. Debug APKs are development artifacts and may not upgrade over APKs signed by another build environment.

### In-app updates

The app checks the public stable GitHub Release for JaynOwO/tibo-button and offers only user-confirmed updates. Automatic checks never download or install an update by themselves.

Before Android’s installer is opened, the updater verifies:

- a stable vX.Y.Z tag and the exact APK asset;
- the SHA-256 supplied by GitHub and SHA256SUMS.txt;
- the com.tibobutton.app package ID, versionName, and versionCode;
- a signing certificate matching the installed app.

Downloads stay in the app-private cache/updates/ directory and are handed to Android through a narrowly scoped FileProvider. Android’s installation confirmation and unknown-source permission flow remain visible. If verification fails, the file is deleted and installation stops. No GitHub login, token, or API key is required.

### Data source, accuracy, and privacy

The app reads Reset Beacon’s public JSON API:

- GET https://resetbeacon.com/api/forecast
- GET https://resetbeacon.com/api/history

When the source does not provide a reliable machine-readable deadline, the app does not infer an exact time from vague text; it shows wording such as “scheduled · see source for time.” Expired or unavailable forecasts are marked instead of presenting old probabilities as current.

Reset Beacon data is used for this public status summary with attribution and a link back in the app and widgets. Quoted or summarized public-post text remains the property of its original author; this project does not claim ownership of full post archives.

Tibo Button does not request an OpenAI login, X login, API key, or your Codex usage data. Widget state, history summaries, notification-deduplication fingerprints, and preferences stay on the local Android device; network requests are public GET requests to Reset Beacon.

### Development build

The repository includes the [Build Android APK workflow](.github/workflows/build-apk.yml):

1. Open **Actions → Build Android APK**.
2. Run the workflow.
3. Download the TiboButton-debug-apk artifact.
4. Extract app-debug.apk and install it on Android.

Local verification commands:

~~~bash
gradle test
gradle :app:assembleDebug
~~~

### Release and license

See [docs/RELEASING.md](docs/RELEASING.md) for the signed-release process. Signing credentials remain in GitHub Actions Secrets and are never committed.

The source code is released under the [MIT License](LICENSE). Third-party services, trademarks, public-post text, and other third-party material are not licensed by the MIT License; see [NOTICE.md](NOTICE.md).
