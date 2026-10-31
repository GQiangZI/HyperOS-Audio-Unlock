package com.qiangzi.hyperosaudiomanager

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.InputStream
import java.io.OutputStream

/**
 * Phase1 管理器，界面换成 Miuix 0.9.4。
 *
 * 导入逻辑与 Phase1 完全一致：
 * SAF 选音频 -> MediaStore 注册（铃声 / 闹钟 / 通知标志），
 * 不申请宽泛存储权限，不写系统分区，不静默改当前默认铃声。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                ManagerScreen()
            }
        }
    }
}

@Composable
private fun ManagerScreen() {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("Ringtones") }
    var status by remember { mutableStateOf("状态：等待导入。第一阶段支持本地导入和系统媒体库注册。") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val target = importToMediaStore(context, uri, selectedCategory)
            status = "已导入系统媒体库：\n$target\n分类：$selectedCategory"
            Toast.makeText(context, "音频已添加到系统媒体库", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            status = "导入失败：${e.message}"
            Toast.makeText(context, "导入失败，请检查音频文件", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "HyperOS Audio Manager",
                subtitle = "本地音频库 · 铃声注册 · UI 音效管理",
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
            SmallTitle(text = "选择导入类别")
            Card(modifier = Modifier.fillMaxWidth()) {
                val categories = listOf(
                    "Ringtones" to "手机铃声",
                    "Alarms" to "闹钟声音",
                    "Notifications" to "通知声音",
                    "Music" to "普通音频",
                )
                for ((value, label) in categories) {
                    RadioButtonPreference(
                        title = label,
                        selected = selectedCategory == value,
                        onClick = { selectedCategory = value },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { picker.launch(arrayOf("audio/*")) },
            ) {
                Text(text = "从手机选择音频并导入")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = "打开系统声音设置",
                    onClick = {
                        try {
                            context.startActivity(Intent("android.settings.SOUND_SETTINGS"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "当前系统未提供声音设置入口", Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "状态")
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(text = status, style = MiuixTheme.textStyles.body2)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "说明：铃声、闹钟和通知通过 Android MediaStore 注册。锁屏、解锁、充电和触摸音效由 LSPosed 模块（:app）接管，本版本不会修改系统分区。",
                    style = MiuixTheme.textStyles.body2,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun importToMediaStore(context: Context, source: Uri, selectedCategory: String): Uri {
    val resolver = context.contentResolver
    var mime = resolver.getType(source)
    if (mime == null || !mime.startsWith("audio/")) mime = "audio/mpeg"
    val ext = extensionForMime(mime)
    val name = "HAM_" + System.currentTimeMillis() + ext

    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, name)
        put(MediaStore.Audio.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= 29) {
            val folder = if (selectedCategory == "Music") "Music/" else "$selectedCategory/"
            put(MediaStore.Audio.Media.RELATIVE_PATH, folder)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        put(MediaStore.Audio.Media.IS_RINGTONE, if (selectedCategory == "Ringtones") 1 else 0)
        put(MediaStore.Audio.Media.IS_ALARM, if (selectedCategory == "Alarms") 1 else 0)
        put(MediaStore.Audio.Media.IS_NOTIFICATION, if (selectedCategory == "Notifications") 1 else 0)
    }

    val dest = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException("MediaStore 未返回目标 Uri")
    try {
        val input: InputStream? = resolver.openInputStream(source)
        val output: OutputStream? = resolver.openOutputStream(dest, "w")
        if (input == null || output == null) throw IllegalStateException("无法打开音频流")
        input.use { inputStream ->
            output.use { outputStream ->
                val buffer = ByteArray(8192)
                var n: Int
                while (inputStream.read(buffer).also { n = it } > 0) {
                    outputStream.write(buffer, 0, n)
                }
            }
        }
    } catch (e: Exception) {
        resolver.delete(dest, null, null)
        throw e
    }
    if (Build.VERSION.SDK_INT >= 29) {
        val done = ContentValues().apply {
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }
        resolver.update(dest, done, null, null)
    }
    return dest
}

private fun extensionForMime(mime: String): String {
    return when (mime) {
        "audio/wav", "audio/x-wav" -> ".wav"
        "audio/ogg", "application/ogg" -> ".ogg"
        "audio/mp4", "audio/m4a" -> ".m4a"
        "audio/flac" -> ".flac"
        "audio/aac" -> ".aac"
        else -> ".mp3"
    }
}
