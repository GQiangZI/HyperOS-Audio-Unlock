package com.qiangzi.hyperosaudiounlock;

/** 双端共享的常量：App 与 Hook 都要用，不能引入 Xposed / Android 特有的依赖。 */
public final class Config {

    /** 模块包名，Hook 侧读 SharedPreferences 时要用。 */
    public static final String PKG = "com.qiangzi.hyperosaudiounlock";

    /** 配置文件名。 */
    public static final String PREFS = "config";

    public static final String KEY_REDIRECT = "redirect_paths";
    public static final String KEY_IGNORE_CUSTOM = "ignore_custom_sound";
    public static final String KEY_VERBOSE = "verbose_log";

    /** 系统的 UI 音效目录（HyperOS 上这是可被模块替换的实体目录）。 */
    public static final String TARGET_DIR = "/product/media/audio/ui/";

    /**
     * 推荐作用域。必须和 res/values/arrays.xml 里的 xposed_scope 保持一致，
     * 否则 LSPosed 里显示的推荐列表和 Hook 实际加载的列表会不一致。
     *
     * 分组说明（详见 docs/scope.md）：
     * - android / com.android.systemui：核心，框架 AudioService 和锁屏/解锁/充电/通知
     * - com.android.settings / com.xiaomi.misettings：设置，AOSP 设置和 HyperOS 的小米设置
     * - com.android.thememanager：主题，主题音效预览与应用
     * - com.miui.miwallpaper / com.android.wallpaper.livepicker /
     *   com.miui.android.fashiongallery：壁纸，超级壁纸、动态壁纸选择器、壁纸轮播
     * - com.android.camera：相机，camera_click.ogg 等快门音效在相机进程里加载
     *
     * 注意：Hook 只重定向白名单里的 UI 音效文件名（AudioHook.UI_SOUNDS），
     * 所以多勾一个系统包不会把第三方 App 的音乐也重定向。
     * 不要把作用域扩大到所有 App，那样只会增加 Hook 加载开销和崩溃面。
     */
    public static final String[] SCOPE = {
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.xiaomi.misettings",
            "com.android.thememanager",
            "com.miui.miwallpaper",
            "com.android.wallpaper.livepicker",
            "com.miui.android.fashiongallery",
            "com.android.camera",
    };

    /** 作用域的中文说明，和 SCOPE 顺序一一对应，用于设置页展示。 */
    public static final String[] SCOPE_LABELS = {
            "系统框架",
            "系统界面",
            "设置",
            "小米设置",
            "主题",
            "壁纸",
            "动态壁纸选择器",
            "壁纸轮播",
            "相机",
    };

    private Config() {
    }
}
