# Tibo Button ⚡

Android 主屏幕上的 **Codex / ChatGPT Work 额外重置雷达**。

> 这是独立第三方小工具，不属于 OpenAI、X 或 Tibo。它不读取你的个人 Codex 额度，也不预测你账号自己的 5 小时 / 周额度窗口。

## 第一版包含

- 4×2 完整 Widget：下次重置、状态、24H/48H 概率、上次重置、更新时间
- 2×2 紧凑 Widget：状态、24H 概率、下次/上次
- 状态：几乎不可能 / 可能性较低 / 有可能 / 很可能 / 已确定 / 数据已过期
- 每 15 分钟请求一次后台刷新（由 Android WorkManager 调度，系统可延后）
- Widget 上 `↻` 手动立即刷新
- 自动使用手机本地时区
- 数据过期时隐藏旧概率，避免把旧数字装成新预测
- 若 API 确认“已排期”但未提供标准化时间字段：显示“已排期 · 时间见来源”，绝不从自然语言硬猜
- 点击 Widget 打开 App；4×2 标题可打开当前最强证据链接（如果有）

## 数据源

Reset Beacon 公共 API：

- `https://resetbeacon.com/api/forecast`
- `https://resetbeacon.com/api/history`
- 文档：`https://resetbeacon.com/api/docs/`

公开 GET 不需要账号或 API Key。

## 用 Android Studio 打开

1. 安装最新版 Android Studio。
2. `File -> Open`，选择 `TiboButton` 文件夹。
3. 如果 Android Studio 提示安装 Android SDK 35，请安装。
4. 等待 Gradle Sync 完成。
5. 手机打开开发者选项和 USB 调试，连接电脑。
6. 点击 Run 安装 Debug 版。
7. 手机桌面长按 -> 小组件 -> `Tibo Button` -> 选择 2×2 或 4×2。

### 关于 Gradle Wrapper

这个压缩包没有附带 `gradle-wrapper.jar`（当前生成环境没有 Android/Gradle SDK，也不应伪造二进制 wrapper）。Android Studio 通常可以使用已配置的 Gradle 同步该项目；如果你的环境要求 Wrapper，可在本机安装 Gradle 后于项目根目录执行：

```bash
gradle wrapper --gradle-version 8.9
```

然后正常使用 `./gradlew` / `gradlew.bat`。

## 用 GitHub Actions 直接打 APK

项目已经附带 `.github/workflows/build-apk.yml`。把整个项目推到 GitHub 后：

1. 打开仓库 `Actions`。
2. 选择 `Build Android APK`。
3. 点击 `Run workflow`。
4. 构建完成后下载 `TiboButton-debug-apk` artifact。

这条工作流会在 GitHub Runner 上配置 JDK 17 + Gradle 8.9，然后执行 `:app:assembleDebug`。

## 重要限制

- WorkManager 的 15 分钟周期不是闹钟。Android 为省电可能延后执行，尤其在 Doze / 深度待机时。
- Reset Beacon 本身是独立来源，不是 OpenAI 官方数据。
- `/api/history` 的公开文档保证了 `eventKind/status/scope/announcedAt` 等字段，但没有承诺一个标准化的未来执行时间字段。因此 v0.1 **只在 API 真正给出可解析 ISO 时间时显示具体下次时间**，否则显示“已排期 · 时间见来源”。这是刻意的保守策略。

## 下一步可做

- 通知：Tibo 发出明确 schedule / confirmed 时推送
- 倒计时每分钟更新（不依赖网络）
- Widget 透明度 / 圆角 / 紧凑度设置
- 英文界面
- 最近 7 次 Reset Streak
- 直接加入 GitHub Actions 自动打 APK

## 构建环境说明

本项目源码由当前会话生成并做了结构/资源 XML/基础逻辑检查；当前运行环境没有 Android SDK，因此**未在这里实际编译 APK**。第一次在 Android Studio 同步时，如新版 AGP/Gradle 提示版本升级，按 IDE 推荐升级即可。
