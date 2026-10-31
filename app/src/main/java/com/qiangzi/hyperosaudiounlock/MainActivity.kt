package com.qiangzi.hyperosaudiounlock

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Miuix 0.9.4 设置页。
 *
 * 保留旧版 Java MainActivity 的全部功能：
 * 状态（Lock.ogg / Unlock.ogg 是否存在）、三个开关
 * （redirect_paths / ignore_custom_sound / verbose_log）、
 * 刷新、采集诊断、查看模块日志。
 *
 * 只是界面换成 Miuix：Scaffold + TopAppBar + Card + SwitchPreference +
 * ArrowPreference，不再用原生 Button / Switch 堆叠。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = getSharedPreferences(Config.PREFS, MODE_PRIVATE)
        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                MainScreen(sp = sp, onToast = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() })
            }
        }
    }
}

@Composable
private fun MainScreen(sp: SharedPreferences, onToast: (String) -> Unit) {
    val context = LocalContext.current
    var redirect by remember { mutableStateOf(sp.getBoolean(Config.KEY_REDIRECT, true)) }
    var ignoreCustom by remember { mutableStateOf(sp.getBoolean(Config.KEY_IGNORE_CUSTOM, true)) }
    var verbose by remember { mutableStateOf(sp.getBoolean(Config.KEY_VERBOSE, false)) }
    var statusText by remember { mutableStateOf(buildStatus(sp)) }
    var logText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun refresh() {
        statusText = buildStatus(sp)
    }

    fun save(key: String, value: Boolean) {
        // 用 commit，保证 Hook 侧 XSharedPreferences 立刻能读到。
        sp.edit().putBoolean(key, value).commit()
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "HyperOS 音效解锁",
                subtitle = "v${versionName(context)} · LSPosed 模块",
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SmallTitle(text = "运行状态")
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(text = statusText, style = MiuixTheme.textStyles.body2)
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "作用域（LSPosed 里都要勾选）")
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                // 直接读 Config，不手写第二份列表，避免三处不一致。
                for (i in Config.SCOPE.indices) {
                    Text(
                        text = "${Config.SCOPE_LABELS[i]}  ${Config.SCOPE[i]}",
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "功能开关")
            Card(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    title = "路径重定向（核心）",
                    summary = "把系统 UI 音效重定向到自定义目录",
                    checked = redirect,
                    onCheckedChange = { redirect = it; save(Config.KEY_REDIRECT, it) },
                )
                SwitchPreference(
                    title = "忽略系统设置里的自定义锁屏音",
                    summary = "让 lock_sound / unlock_sound 返回空，回落到系统文件",
                    checked = ignoreCustom,
                    onCheckedChange = { ignoreCustom = it; save(Config.KEY_IGNORE_CUSTOM, it) },
                )
                SwitchPreference(
                    title = "详细日志",
                    summary = "在 LSPosed 日志里输出更多 Hook 记录",
                    checked = verbose,
                    onCheckedChange = { verbose = it; save(Config.KEY_VERBOSE, it) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "工具")
            Card(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = "刷新状态",
                    onClick = { refresh(); onToast("已刷新") },
                )
                ArrowPreference(
                    title = "采集诊断信息 → Download",
                    summary = if (busy) "正在采集…" else null,
                    onClick = {
                        if (busy) return@ArrowPreference
                        busy = true
                        onToast("正在采集，可能会请求 root…")
                        Thread {
                            val msg = try {
                                "已保存：" + Diagnostics.save(context).absolutePath
                            } catch (t: Throwable) {
                                "失败：$t"
                            }
                            (context as? android.app.Activity)?.runOnUiThread {
                                busy = false
                                onToast(msg)
                            }
                        }.start()
                    },
                )
                ArrowPreference(
                    title = "查看模块日志",
                    onClick = {
                        onToast("读取日志中…")
                        Thread {
                            val text = Diagnostics.logcat()
                            (context as? android.app.Activity)?.runOnUiThread {
                                logText = text
                            }
                        }.start()
                    },
                )
            }

            logText?.let { text ->
                Spacer(modifier = Modifier.height(16.dp))
                SmallTitle(text = "模块日志")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Text(text = text, style = MiuixTheme.textStyles.body2)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "怎么用")
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "1. 在 LSPosed 里启用本模块，作用域勾选「系统界面 com.android.systemui」和「系统框架 android」。\n"
                        + "2. 用音效替换模块把 Lock.ogg / Unlock.ogg 放进 /product/media/audio/ui/。\n"
                        + "3. 重启手机，锁屏 / 解锁试一下。\n"
                        + "4. 还是原声？点「查看模块日志」，看 SystemUI 到底从哪儿加载音效。",
                    style = MiuixTheme.textStyles.body2,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 保留一个主按钮入口，避免旧用户找不到操作。
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { refresh(); onToast("已刷新") },
            ) {
                Text(text = "刷新状态")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun buildStatus(sp: SharedPreferences): String {
    val b = StringBuilder()
    b.append("音效目录  ").append(Config.TARGET_DIR).append('\n')
    for (name in arrayOf("Lock.ogg", "Unlock.ogg")) {
        val f = File(Config.TARGET_DIR + name)
        if (f.isFile()) {
            b.append("  ").append(name).append("  ")
                .append(f.length()).append(" B  ")
                .append(fmt(f.lastModified())).append('\n')
        } else {
            b.append("  ").append(name).append("  不存在\n")
        }
    }
    b.append('\n')
    b.append("作用域  ").append(Config.SCOPE.joinToString(", ")).append('\n')
    b.append("配置  重定向=").append(if (sp.getBoolean(Config.KEY_REDIRECT, true)) "开" else "关")
        .append("  忽略自定义=").append(if (sp.getBoolean(Config.KEY_IGNORE_CUSTOM, true)) "开" else "关")
        .append('\n')
    b.append('\n')
    b.append("Hook 是否真正加载，需要看 LSPosed 日志\n或点下面「查看模块日志」。")
    return b.toString()
}

private fun versionName(context: android.content.Context): String {
    return try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (t: Throwable) {
        "?"
    }
}

private fun fmt(time: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))
}
