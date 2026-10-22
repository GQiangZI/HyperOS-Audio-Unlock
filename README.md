# HyperOS 音效解锁（LSPosed）

把 HyperOS / MIUI 的**锁屏、解锁、按键、相机**等系统 UI 音效，强制指向
`/product/media/audio/ui/` 下的自定义文件——很多「音效替换」模块只是把文件塞进这个目录，
但 SystemUI 并不一定真去读，于是听着还是原声。这个模块负责让 SystemUI 真的去读。

## 它能干什么

| 开关 | 作用 |
| --- | --- |
| 路径重定向（核心） | Hook `SoundPool.load(String)`、`MediaPlayer.setDataSource(...)`：只要文件名是 `Lock.ogg`/`Unlock.ogg` 这类系统 UI 音效，就把路径改写到 `/product/media/audio/ui/同名文件` |
| 忽略系统设置里的自定义锁屏音 | Hook `Settings.Global.getString("lock_sound"/"unlock_sound")` 返回 `null`，避免系统优先播放你曾经选过的第三方铃声 |
| 详细日志 | 每个音效调用都写一行日志，方便定位 SystemUI 到底从哪儿加载 |

另外还会 hook `AudioManager.playSoundEffect(int)` 做记录（仅详细日志下），
用来判断这台机器是否走的是「音效枚举」这条路。

## 用法

1. 安装 APK，在 LSPosed 里启用模块。
2. **作用域**勾选：
   - 系统界面 / `com.android.systemui`
   - 系统框架 / `android`
3. 重启手机。
4. 锁屏 / 解锁试听。没变化就去 App 里点「查看模块日志」。

## 它和「音效替换模块」的分工

```
音效替换模块  ->  往 /product/media/audio/ui/ 放 Lock.ogg / Unlock.ogg
本模块        ->  把 SystemUI 的加载路径重定向到上面那个目录
```

## 关于日志

`XposedBridge.log()` 在 LSPosed 里的 tag 是 `LSPosed`，内容前缀 `[HyperOSAudioUnlock]`。
想看实时日志：

```bash
logcat | grep HyperOSAudioUnlock
```

## 构建

推送到 `main` 后由 GitHub Actions 自动打包（`.github/workflows/build.yml`），
产物在 Actions 的 Artifacts 里：`HyperOS-Audio-Unlock-apk`。

本地构建（需要 Android SDK）：

```bash
gradle :app:assembleRelease
```

APK 使用 debug 密钥签名，装完能用；要上架/长期分发请换成自己的 keystore。
依赖只有 `app/libs/api-82.jar`（Xposed API 82，`compileOnly`，不进 APK）。

## 免责

改的是系统进程里的音频加载路径，出问题最多是音效不响或 SystemUI 抽风，重启即可恢复；
但请自行评估风险，别在重要设备上瞎折腾。
