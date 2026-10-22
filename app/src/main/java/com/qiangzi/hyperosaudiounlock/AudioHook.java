package com.qiangzi.hyperosaudiounlock;

import android.content.ContentResolver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * HyperOS 音效解锁 —— LSPosed 模块入口。
 *
 * <p>思路：音效替换类模块只是把 Lock.ogg / Unlock.ogg 放进 /product/media/audio/ui/，
 * 但 SystemUI 未必真去读那个文件（可能读系统设置里记的自定义 URI、可能是别的路径、
 * 也可能走了资源 ID）。这里做两件事：</p>
 *
 * <ol>
 *   <li>把路径形式的音效加载（SoundPool.load(String)、MediaPlayer.setDataSource）
 *       统一重定向到 /product/media/audio/ui/ 下的同名文件；</li>
 *   <li>可选地让 Settings.Global 的 lock_sound / unlock_sound 返回空，
 *       避免系统优先播放用户曾经选过的自定义铃声，从而回落到系统文件。</li>
 * </ol>
 */
public final class AudioHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "[HyperOSAudioUnlock] ";

    /** 这些名字才会被重定向，避免误伤第三方应用的音频资源。 */
    private static final String[] UI_SOUNDS = {
            "Lock.ogg", "Unlock.ogg",
            "Effect_Tick.ogg",
            "KeypressStandard.ogg", "KeypressSpacebar.ogg", "KeypressDelete.ogg",
            "KeypressReturn.ogg", "KeypressInvalid.ogg",
            "camera_click.ogg", "VideoRecord.ogg", "VideoStop.ogg", "AudioRecordStart.ogg",
            "LowBattery.ogg", "dock.ogg", "Undock.ogg",
            "Trusted.ogg", "WirelessChargingStarted.ogg"
    };

    private static final String[] SETTING_KEYS = {"lock_sound", "unlock_sound"};

    private static String modulePath;
    private static volatile boolean installed;
    private static volatile HookPrefs prefs;

    @Override
    public void initZygote(StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!isScoped(lpparam.packageName)) {
            return;
        }
        synchronized (AudioHook.class) {
            if (installed) {
                return;
            }
            installed = true;
            try {
                prefs = new HookPrefs();
            } catch (Throwable t) {
                log("读取配置失败，使用默认值: " + t);
            }
            log("loaded pkg=" + lpparam.packageName
                    + " proc=" + lpparam.processName
                    + " module=" + modulePath);
            hookSoundPool();
            hookMediaPlayer();
            hookSettings(lpparam.classLoader);
            hookAudioManager();
        }
    }

    // ---------------------------------------------------------------- hooks

    private static void hookSoundPool() {
        try {
            XposedBridge.hookAllMethods(SoundPool.class, "load", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        Object first = (param.args != null && param.args.length > 0) ? param.args[0] : null;
                        if (!(first instanceof String)) {
                            return;
                        }
                        HookPrefs p = refresh();
                        String before = (String) first;
                        if (p == null || !p.redirect()) {
                            verbose("SoundPool.load(path) 跳过（重定向已关闭）: " + before);
                            return;
                        }
                        String after = resolve(before);
                        if (!after.equals(before)) {
                            param.args[0] = after;
                            log("SoundPool.load " + before + "  ->  " + after);
                        } else {
                            verbose("SoundPool.load " + before + "（无需重定向）");
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
            log("hook SoundPool 失败: " + t);
        }
    }

    private static void hookMediaPlayer() {
        try {
            XposedBridge.hookAllMethods(MediaPlayer.class, "setDataSource", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (param.args == null || param.args.length == 0) {
                            return;
                        }
                        HookPrefs p = refresh();
                        if (p == null || !p.redirect()) {
                            return;
                        }
                        Object first = param.args[0];
                        if (first instanceof String) {
                            String before = (String) first;
                            String after = resolve(before);
                            if (!after.equals(before)) {
                                param.args[0] = after;
                                log("MediaPlayer.setDataSource(String) " + before + "  ->  " + after);
                            } else {
                                verbose("MediaPlayer.setDataSource(String) " + before);
                            }
                        } else if (first instanceof android.content.Context
                                && param.args.length > 1 && param.args[1] instanceof Uri) {
                            Uri uri = (Uri) param.args[1];
                            if (!"file".equalsIgnoreCase(uri.getScheme())) {
                                verbose("MediaPlayer.setDataSource 非 file URI: " + uri);
                                return;
                            }
                            String before = uri.getPath();
                            String after = resolve(before);
                            if (before != null && !after.equals(before)) {
                                param.args[1] = Uri.fromFile(new File(after));
                                log("MediaPlayer.setDataSource(Context,Uri) " + before + "  ->  " + after);
                            }
                        }
                    } catch (Throwable t) {
                        verbose("setDataSource hook 出错: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("hook MediaPlayer 失败: " + t);
        }
    }

    /** 让 lock_sound / unlock_sound 失联，逼 SystemUI 回落到系统文件。 */
    private static void hookSettings(ClassLoader cl) {
        try {
            XposedBridge.hookAllMethods(
                    XposedHelpers.findClass("android.provider.Settings$Global", cl),
                    "getString",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (param.args == null || param.args.length < 2) {
                                    return;
                                }
                                if (!(param.args[1] instanceof String)) {
                                    return;
                                }
                                HookPrefs p = refresh();
                                if (p == null || !p.ignoreCustom()) {
                                    return;
                                }
                                String key = (String) param.args[1];
                                if (!matches(key)) {
                                    return;
                                }
                                Object value = param.getResult();
                                if (value == null) {
                                    return;
                                }
                                log("Settings.Global.getString(" + key + ") = " + value + "  ->  null");
                                param.setResult(null);
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable t) {
            log("hook Settings.Global 失败: " + t);
        }
    }

    /** 只做记录：确认 SystemUI 是否走 playSoundEffect 这条路。 */
    private static void hookAudioManager() {
        try {
            XposedBridge.hookAllMethods(AudioManager.class, "playSoundEffect", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        HookPrefs p = refresh();
                        if (p != null && p.verbose() && param.args != null && param.args.length > 0) {
                            log("AudioManager.playSoundEffect(effect=" + param.args[0] + ")");
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable t) {
            log("hook AudioManager 失败: " + t);
        }
    }

    // -------------------------------------------------------------- helpers

    private static HookPrefs refresh() {
        HookPrefs p = prefs;
        if (p != null) {
            p.reloadIfStale();
        }
        return p;
    }

    private static boolean matches(String key) {
        for (String k : SETTING_KEYS) {
            if (k.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String resolve(String path) {
        if (path == null) {
            return null;
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (!isUiSound(name)) {
            return path;
        }
        File target = new File(Config.TARGET_DIR + name);
        String abs = target.getAbsolutePath();
        if (abs.equals(path)) {
            return path;
        }
        if (target.isFile() && target.canRead()) {
            return abs;
        }
        return path;
    }

    private static boolean isUiSound(String name) {
        for (String s : UI_SOUNDS) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isScoped(String pkg) {
        if (pkg == null) {
            return false;
        }
        for (String s : Config.SCOPE) {
            if (s.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    private static void log(String msg) {
        XposedBridge.log(TAG + msg);
    }

    private static void verbose(String msg) {
        HookPrefs p = prefs;
        if (p == null || p.verbose()) {
            log(msg);
        }
    }
}