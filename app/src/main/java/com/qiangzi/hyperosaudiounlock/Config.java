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

    /** 默认作用域。 */
    public static final String[] SCOPE = {"com.android.systemui", "android"};

    private Config() {
    }
}
