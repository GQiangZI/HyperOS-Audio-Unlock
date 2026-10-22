package com.qiangzi.hyperosaudiounlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 模块设置界面：全代码构建，不依赖 layout 与 AndroidX，编译体积小、兼容性好。 */
public final class MainActivity extends Activity {

    private static final String[] UI_FILES = {"Lock.ogg", "Unlock.ogg"};

    private static final String HELP =
            "1. 在 LSPosed 里启用本模块，作用域勾选「系统界面 com.android.systemui」和「系统框架 android」。\n"
                    + "2. 用音效替换模块把 Lock.ogg / Unlock.ogg 放进 /product/media/audio/ui/。\n"
                    + "3. 重启手机，锁屏 / 解锁试一下。\n"
                    + "4. 还是原声？点「查看模块日志」，看 SystemUI 到底从哪儿加载音效。";

    private SharedPreferences sp;
    private LinearLayout content;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(Config.PREFS, MODE_PRIVATE);

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        content.setPadding(pad, pad, pad, pad);
        sv.addView(content);
        setContentView(sv);

        buildUi();
        refresh();
    }

    private void buildUi() {
        TextView title = new TextView(this);
        title.setText("HyperOS 音效解锁");
        title.setTextSize(23);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(title);

        TextView sub = new TextView(this);
        sub.setText("v" + versionName() + " · LSPosed 模块");
        sub.setTextSize(12);
        sub.setAlpha(0.65f);
        content.addView(sub);
        content.addView(gap(18));

        content.addView(caption("运行状态"));
        status = new TextView(this);
        status.setTypeface(Typeface.MONOSPACE);
        status.setTextSize(11);
        status.setPadding(dp(12), dp(12), dp(12), dp(12));
        status.setBackgroundColor(0x14000000);
        content.addView(status);
        content.addView(gap(18));

        content.addView(caption("功能开关"));
        content.addView(toggle("路径重定向（核心）", Config.KEY_REDIRECT, true));
        content.addView(toggle("忽略系统设置里的自定义锁屏音", Config.KEY_IGNORE_CUSTOM, true));
        content.addView(toggle("详细日志", Config.KEY_VERBOSE, false));
        content.addView(gap(18));

        content.addView(caption("工具"));
        content.addView(button("刷新状态", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
                Toast.makeText(MainActivity.this, "已刷新", Toast.LENGTH_SHORT).show();
            }
        }));
        content.addView(button("采集诊断信息 → Download", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiagnose();
            }
        }));
        content.addView(button("查看模块日志", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog();
            }
        }));
        content.addView(gap(18));

        content.addView(caption("怎么用"));
        TextView help = new TextView(this);
        help.setText(HELP);
        help.setTextSize(13);
        help.setLineSpacing(dp(5), 1f);
        content.addView(help);
    }

    // ------------------------------------------------------------------ UI

    private TextView caption(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setAlpha(0.7f);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private View gap(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return v;
    }

    private LinearLayout toggle(String label, final String key, boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(15);
        row.addView(tv, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(sp.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 用 commit 保证 XSharedPreferences 立刻能读到
                sp.edit().putBoolean(key, isChecked).commit();
                refresh();
            }
        });
        row.addView(sw);
        return row;
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    // -------------------------------------------------------------- actions

    private void refresh() {
        StringBuilder b = new StringBuilder();
        b.append("音效目录  ").append(Config.TARGET_DIR).append('\n');
        for (String name : UI_FILES) {
            File f = new File(Config.TARGET_DIR + name);
            if (f.isFile()) {
                b.append("  ").append(name).append("  ")
                        .append(f.length()).append(" B  ")
                        .append(fmt(f.lastModified())).append('\n');
            } else {
                b.append("  ").append(name).append("  不存在\n");
            }
        }
        b.append('\n');
        b.append("作用域  ").append(TextUtils.join(", ", Config.SCOPE)).append('\n');
        b.append("配置  重定向=").append(onOff(Config.KEY_REDIRECT, true))
                .append("  忽略自定义=").append(onOff(Config.KEY_IGNORE_CUSTOM, true))
                .append('\n');
        b.append('\n');
        b.append("Hook 是否真正加载，需要看 LSPosed 日志\n或点下面「查看模块日志」。");
        status.setText(b.toString());
    }

    private void doDiagnose() {
        Toast.makeText(this, "正在采集，可能会请求 root…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    File f = Diagnostics.save(MainActivity.this);
                    msg = "已保存：" + f.getAbsolutePath();
                } catch (Throwable t) {
                    msg = "失败：" + t;
                }
                final String result = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void showLog() {
        Toast.makeText(this, "读取日志中…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String text = Diagnostics.logcat();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = new TextView(MainActivity.this);
                        tv.setText(text);
                        tv.setTextSize(10);
                        tv.setTypeface(Typeface.MONOSPACE);
                        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
                        ScrollView sv = new ScrollView(MainActivity.this);
                        sv.addView(tv);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("模块日志")
                                .setView(sv)
                                .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                });
            }
        }).start();
    }

    // -------------------------------------------------------------- helpers

    private String onOff(String key, boolean def) {
        return sp.getBoolean(key, def) ? "开" : "关";
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    private static String fmt(long time) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(time));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}