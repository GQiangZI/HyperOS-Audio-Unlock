package com.qiangzi.hyperosaudiounlock;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 通过 su 采集诊断信息，供 App 界面一键生成报告。 */
public final class Diagnostics {

    private Diagnostics() {
    }

    /** 执行一条 root 命令，返回 stdout+stderr。 */
    public static String sh(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            p.waitFor();
            String out = sb.toString().trim();
            return out.isEmpty() ? "(无输出)" : out;
        } catch (Throwable t) {
            return "(需要 root：" + t + ")";
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 从 logcat 里抓模块日志。 */
    public static String logcat() {
        String s = sh("logcat -d -t 4000 2>/dev/null | grep -i HyperOSAudioUnlock | tail -n 120");
        if (s == null || s.isEmpty()) {
            return "(没有抓到日志：确认模块已在 LSPosed 里启用并重启过)";
        }
        return s;
    }

    /** 生成完整报告文本。 */
    public static String report() {
        StringBuilder b = new StringBuilder();
        b.append("HyperOS Audio Unlock 诊断报告\n\n");

        b.append("== 设备 ==\n");
        b.append(sh("getprop ro.product.model")).append('\n');
        b.append("Android ").append(sh("getprop ro.build.version.release"))
                .append(" / SDK ").append(sh("getprop ro.build.version.sdk")).append('\n');
        b.append(sh("getprop ro.build.version.incremental")).append("\n\n");

        b.append("== 音效设置 ==\n");
        b.append("lock_sound   = ").append(sh("settings get global lock_sound")).append('\n');
        b.append("unlock_sound = ").append(sh("settings get global unlock_sound")).append("\n\n");

        b.append("== 系统 UI 音效文件 ==\n");
        b.append(sh("ls -l /product/media/audio/ui/Lock.ogg /product/media/audio/ui/Unlock.ogg 2>&1")).append('\n');
        b.append(sh("sha256sum /product/media/audio/ui/Lock.ogg /product/media/audio/ui/Unlock.ogg 2>&1")).append("\n\n");

        b.append("== 挂载记录 ==\n");
        b.append(sh("grep -E 'audio/ui' /proc/mounts 2>&1")).append('\n');
        b.append(sh("mount | grep -E 'audio/ui' 2>&1")).append("\n\n");

        b.append("== 已装 Magisk 模块 ==\n");
        b.append(sh("ls -l /data/adb/modules/ 2>&1")).append('\n');
        b.append(sh("ls -l /data/adb/modules/Hyperos_audio_core/system/product/media/audio/ui/ 2>&1")).append("\n\n");

        b.append("== 模块日志 ==\n");
        b.append(logcat()).append('\n');
        return b.toString();
    }

    /** 生成报告并写到 /sdcard/Download/。 */
    public static File save(Context ctx) throws Exception {
        String text = report();
        File cache = new File(ctx.getCacheDir(), "hyperos-audio-diagnostic.txt");
        FileOutputStream fos = new FileOutputStream(cache);
        try {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        } finally {
            fos.close();
        }
        sh("cp " + cache.getAbsolutePath() + " /sdcard/Download/hyperos-audio-diagnostic.txt; "
                + "chmod 0644 /sdcard/Download/hyperos-audio-diagnostic.txt");
        return new File("/sdcard/Download/hyperos-audio-diagnostic.txt");
    }
}