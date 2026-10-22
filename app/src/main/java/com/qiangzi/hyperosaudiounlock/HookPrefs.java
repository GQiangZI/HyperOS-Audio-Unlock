package com.qiangzi.hyperosaudiounlock;

import android.os.SystemClock;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Hook 侧读取配置。XSharedPreferences 由 LSPosed 兼容提供，可以跨进程读取模块自己的
 * SharedPreferences，不需要在 system_server 里额外申请权限。
 */
public final class HookPrefs {

    private final XSharedPreferences sp;
    private long lastRead;
    private boolean redirect = true;
    private boolean ignoreCustom = true;
    private boolean verbose;

    public HookPrefs() {
        sp = new XSharedPreferences(Config.PKG, Config.PREFS);
        try {
            sp.makeWorldReadable();
        } catch (Throwable ignored) {
            // 新版 Android 已不允许，LSPosed 自己做了兼容，忽略即可
        }
        reload(true);
    }

    /** 在每个 hook 调用点调用，最多 1.5 秒刷新一次，避免频繁读盘。 */
    public void reloadIfStale() {
        reload(false);
    }

    private synchronized void reload(boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastRead < 1500L) {
            return;
        }
        lastRead = now;
        try {
            sp.reload();
        } catch (Throwable ignored) {
        }
        redirect = bool(Config.KEY_REDIRECT, true);
        ignoreCustom = bool(Config.KEY_IGNORE_CUSTOM, true);
        verbose = bool(Config.KEY_VERBOSE, false);
    }

    private boolean bool(String key, boolean def) {
        try {
            return sp.getBoolean(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public boolean redirect() {
        return redirect;
    }

    public boolean ignoreCustom() {
        return ignoreCustom;
    }

    public boolean verbose() {
        return verbose;
    }
}