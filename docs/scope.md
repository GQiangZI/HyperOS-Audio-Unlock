# 插件作用域（为什么是这 9 个包）

LSPosed 的作用域 = Hook 会加载进哪些进程。
Hook 只在作用域里的进程里跑，所以漏勾一个包，
那个包自己加载的音效就不会被重定向。

## 推荐作用域（和 `Config.SCOPE` / `xposed_scope` 一致）

| 包名 | 中文 | 为什么要勾 |
|---|---|---|
| `android` | 系统框架 | AudioService、SoundPool、Settings.Global 都在系统进程里，核心 |
| `com.android.systemui` | 系统界面 | 锁屏、解锁、充电、通知、低电量，SystemUI 自己播这些音 |
| `com.android.settings` | 设置 | AOSP 设置，锁屏音设置、音效预览 |
| `com.xiaomi.misettings` | 小米设置 | HyperOS / 小米 18 等机型的设置是这个包，不是 `com.android.settings`，两个都要勾 |
| `com.android.thememanager` | 主题 | 小米主题，主题音效预览和应用时自己加载音效 |
| `com.miui.miwallpaper` | 壁纸 | 超级壁纸，锁屏联动场景 |
| `com.android.wallpaper.livepicker` | 动态壁纸选择器 | 选动态壁纸时的预览 |
| `com.miui.android.fashiongallery` | 壁纸轮播 | 小米壁纸轮播 |
| `com.android.camera` | 相机 | `camera_click.ogg`、`VideoRecord.ogg` 等在相机进程里加载 |

## 故意没加的包

- `com.miui.home`（桌面）：桌面本身不直接播锁屏音，锁屏在 SystemUI。
- 第三方 App（微信、音乐等）：它们不加载系统 UI 音效白名单里的文件，
  勾了也只会增加 Hook 开销；Hook 的白名单（`AudioHook.UI_SOUNDS`）
  本来就会挡住非 UI 音效。
- `com.android.phone` / 通话：来电铃声走 RingtoneManager / Telecom，
  不走本模块的 UI 音效白名单，勾了也没用。

## 如果某个音效还是原声，怎么查

1. 在 LSPosed 里确认上面 9 个包都已勾选，重启。
2. 打开模块设置页 → 查看模块日志，搜 `loaded pkg=`，
   看实际加载音效的进程包名是不是在上面列表里。
3. 如果日志里出现一个新包名（比如某个 ROM 的定制设置包），
   把它加进 `Config.SCOPE` 和 `arrays.xml`，再编译一次。
   不要直接在 LSPosed 里只勾一个包就完事——下次重新安装模块，
   `xposed_scope` 里的推荐列表会覆盖你的手动选择。

## 作用域和 Miuix 设置页

设置页的「作用域」卡片直接读 `Config.SCOPE` / `SCOPE_LABELS`，
所以代码、Manifest、设置页三处不可能不一致。
改作用域时只改 `Config.java` 和 `arrays.xml`，设置页会自动跟上。
