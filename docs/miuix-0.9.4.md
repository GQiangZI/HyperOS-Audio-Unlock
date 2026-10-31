# Miuix 0.9.4 集成说明

## 用的就是官方库，不是复刻

- Release：https://github.com/compose-miuix-ui/miuix/releases/tag/v0.9.4
- 0.9.0 起主产物已拆分，旧的 `miuix` / `miuix-android` 只到 0.8.8，
  0.9.4 必须用拆分后的产物：

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
```

## 工具链（按 Miuix 0.9.4 Release Notes 对齐）

| 项目 | 版本 |
|---|---|
| Miuix | 0.9.4 |
| Compose Multiplatform | 1.12.0 |
| Kotlin | 2.4.20 |
| AGP | 9.4.1（JDK 17，Gradle 最低 9.6） |
| Gradle | 9.7.1 |
| compileSdk | 36 |
| minSdk | 26（Miuix 0.9.4 要求最低 24，`miuix-blur` 除外，本仓库没用 blur） |
| targetSdk | 35 |

## 改动范围

- `:app`：旧 Java `MainActivity` 已删除，换成 Kotlin Compose
  `MainActivity.kt`。Hook 代码（`AudioHook` / `Config` / `HookPrefs` /
  `Diagnostics`）**没有动**，开关 key、诊断、日志功能全部保留。
  界面组件：`Scaffold` + `TopAppBar` + `Card` + `SmallTitle` +
  `SwitchPreference` + `ArrowPreference` + `Button`。
- `:manager`：新模块，Phase1 的 SAF + MediaStore 导入逻辑原样移植到
  Kotlin Compose，界面组件：`Scaffold` + `TopAppBar` +
  `RadioButtonPreference`（选铃声 / 闹钟 / 通知 / 普通音频）+
  `ArrowPreference` + `Button`。
- `android.useAndroidX` 从 `false` 改成 `true`：Miuix / Compose
  必须用 AndroidX，旧的“不用 AndroidX”设定与之冲突。

## 已核对的 API（对照 v0.9.4 源码）

- `top.yukonga.miuix.kmp.theme.MiuixTheme` /
  `ThemeController(ColorSchemeMode.System)`
- `top.yukonga.miuix.kmp.basic.Scaffold`、`TopAppBar(title, subtitle)`、
  `Card`、`SmallTitle(text)`、`Text(text, style)`、`Button(onClick) { Text }`
- `top.yukonga.miuix.kmp.preference.SwitchPreference(checked, onCheckedChange, title, summary?)`
- `top.yukonga.miuix.kmp.preference.RadioButtonPreference(title, selected, onClick)`
- `top.yukonga.miuix.kmp.preference.ArrowPreference(title, summary?, onClick)`

## 本地没有 Android SDK 时

本机无法本地编译，CI 是验证路径：
push 到 `main` 后 GitHub Actions 用 JDK 17 + Gradle 9.7.1 编译
`:app:assembleRelease :manager:assembleRelease`，产物在 Artifacts 里。
如果 CI 报 Compose / Activity 版本冲突，先看 CI 日志里的具体冲突，
不要直接降 Miuix 版本——0.9.4 的 Compose 1.12 / Kotlin 2.4 是成套的。
