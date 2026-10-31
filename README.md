# HyperOS Audio Unlock (LSPosed 模块)

解除 HyperOS / MIUI 对系统音效路径的限制，把 UI 音效重定向到你自己的
Magisk 模块音频目录。设置页和管理器界面使用官方 Miuix 0.9.4。

## 界面：Miuix 0.9.4（官方库）

- Release：https://github.com/compose-miuix-ui/miuix/releases/tag/v0.9.4
- 产物（0.9.0 起已拆分，旧 `miuix-android` 只到 0.8.8）：
  `top.yukonga.miuix.kmp:miuix-ui-android:0.9.4`、
  `top.yukonga.miuix.kmp:miuix-preference-android:0.9.4`
- 工具链：AGP 9.4.1 / Gradle 9.7.1 / Kotlin 2.4.20 /
  Compose Multiplatform 1.12.0 / JDK 17 / compileSdk 36
- 详情见 `docs/miuix-0.9.4.md`。

## 包含什么

| 模块 | 包名 | 作用 |
|---|---|---|
| `:app` | `com.qiangzi.hyperosaudiounlock` | LSPosed Hook + Miuix 设置页 |
| `:manager` | `com.qiangzi.hyperosaudiomanager` | Phase1 音频导入管理器，Miuix 界面 |

`:manager` 的导入逻辑与 Phase1 一致：SAF 选音频 → MediaStore 注册
（铃声 / 闹钟 / 通知标志），不申请宽泛存储权限，不写系统分区，
不静默改当前默认铃声。

## 适配
- Android 10+ (minSdk 26，Miuix 0.9.4 要求最低 24)
- LSPosed (Zygisk)
- 配合 Magisk/KSU 模块 `Hyperos_audio_core` 使用

## 工作原理

具体 Hook 逻辑以 `app/src/main/java/com/qiangzi/hyperosaudiounlock/AudioHook.java` 为准，
大意是：

1. Hook `SoundPool.load` / `MediaPlayer.setDataSource` 等路径形式的音效加载。
2. 只对白名单里的 UI 音效文件名（如 `Lock.ogg` / `Unlock.ogg`）做重定向，
   目标是 `/product/media/audio/ui/` 下的同名文件。
3. 自定义文件不存在则回退原路径，不破坏系统音效。
4. 设置页可开关：路径重定向、忽略系统设置里的自定义锁屏音、详细日志。
5. 日志输出到 LSPosed 日志；设置页的「查看模块日志」从 logcat 里抓
   `HyperOSAudioUnlock` 相关记录。

## 编译（GitHub Actions）

```bash
gradle :app:assembleRelease :manager:assembleRelease
```

push 到 `main` 后，在 **Actions** 里下载 Artifacts：
`HyperOS-Audio-Unlock.apk`（LSPosed 模块）和
`HyperOS-Audio-Manager.apk`（导入管理器）。

## 安装后
1. 打开 LSPosed → 模块 → 启用 **HyperOS Audio Unlock**
2. 作用域全部勾选（详见 `docs/scope.md`，设置页里也有这张表）：
   - `android`（系统框架）
   - `com.android.systemui`（系统界面）
   - `com.android.settings`（设置）
   - `com.xiaomi.misettings`（小米设置，HyperOS 新机型用这个）
   - `com.android.thememanager`（主题）
   - `com.miui.miwallpaper`（壁纸）
   - `com.android.wallpaper.livepicker`（动态壁纸选择器）
   - `com.miui.android.fashiongallery`（壁纸轮播）
   - `com.android.camera`（相机，快门音效）
3. 桌面点模块图标，按需开关
4. 重启手机
5. 在设置页点「查看模块日志」，确认 SystemUI 到底从哪儿加载音效
