# HyperOS Audio Unlock (LSPosed 模块)

解除 HyperOS / MIUI 对系统音效路径的限制，把 UI 音效、通知音、铃声、闹钟音重定向到你自己的 Magisk 模块音频目录。

## 适配
- Android 10+ (minSdk 28)
- LSPosed (Zygisk)
- 配合 Magisk/KSU 模块 `Hyperos_audio_core` 使用

## 工作原理
1. Hook `android.media.SoundPool.load(String, int)`
2. 发现路径以 `/system/media/audio/`、`/product/media/audio/` 开头时，替换为
   `/data/adb/modules/Hyperos_audio_core/system/media/audio/` 下同名文件
3. 自定义文件不存在则回退原路径，不破坏系统音效
4. 设置页面可独立开关 UI / 通知 / 铃声 / 闹钟四类替换
5. 日志写入 `/sdcard/Android/HyperOSAudioUnlock/hook.log`，同时输出到 LSP 日志

## 手机端编译（GitHub Actions）
1. 新建仓库 `HyperOSAudioUnlock`（私有即可）
2. 上传本仓库全部文件
3. 进入 **Actions** → 选 **Build & Release** → **Run workflow**
4. 等待 3~5 分钟，在运行页底部 **Artifacts** 下载 `HyperOSAudioUnlock-APK`
5. 解压得到 `HyperOSAudioUnlock-debug.apk`，安装到手机

## 安装后
1. 打开 LSPosed → 模块 → 启用 **HyperOS Audio Unlock**
2. 作用域勾选：
   - `android` (系统框架)
   - `com.android.systemui`
   - `com.android.settings`
3. 桌面点模块图标，按需开关四类替换
4. 重启手机
5. 查看日志：`/sdcard/Android/HyperOSAudioUnlock/hook.log`

## 打 Release
给仓库打 tag：
```
v1.0.0
```
push 后 Actions 自动创建 Release 并附上 APK。
