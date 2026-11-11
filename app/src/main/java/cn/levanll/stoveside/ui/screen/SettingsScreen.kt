package cn.levanll.stoveside.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.levanll.stoveside.BuildConfig
import cn.levanll.stoveside.StoveApp
import cn.levanll.stoveside.backup.BackupService
import cn.levanll.stoveside.backup.ImportResult
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.ui.StoveViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: StoveViewModel, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = StoveApp.instance
    val backup = remember {
        BackupService(ctx, app.repository, cn.levanll.stoveside.data.StoveDatabase.get(ctx).dao())
    }
    var message by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(I18n.lang) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val r = backup.exportTo(uri)
                message = r.fold(
                    { I18n.t("已导出", "Exported") },
                    { I18n.t("导出失败：${it.message}", "Export failed: ${it.message}") },
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                message = when (val r = backup.importFrom(uri)) {
                    is ImportResult.Success -> I18n.t("已导入 ${r.recipes} 份食谱", "Imported ${r.recipes} recipes")
                    is ImportResult.Failure -> I18n.t("导入失败：${r.reason}", "Import failed: ${r.reason}")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                I18n.t("设置", "Settings"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            SectionCard(I18n.t("语言", "Language")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { I18n.set(I18n.Lang.ZH); lang = I18n.lang },
                        enabled = lang != I18n.Lang.ZH,
                    ) { Text("中文") }
                    OutlinedButton(
                        onClick = { I18n.set(I18n.Lang.EN); lang = I18n.lang },
                        enabled = lang != I18n.Lang.EN,
                    ) { Text("English") }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    I18n.t("选择会立即保存，下次启动沿用", "Saved immediately, restored on next launch"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(I18n.t("数据", "Data")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportLauncher.launch("stoveside-$stamp.json")
                    }) { Text(I18n.t("导出备份", "Export")) }
                    OutlinedButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text(I18n.t("导入恢复", "Import")) }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    I18n.t(
                        "导入前会自动快照当前数据，失败自动还原",
                        "A snapshot is taken before import; failures roll back automatically",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }


        item {
            UpdateSection()
        }

        item {
            SectionCard(I18n.t("关于", "About")) {
                Text("StoveSide ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Spacer(Modifier.height(4.dp))
                Text(
                    I18n.t("本地计时，不上传任何数据", "All timers are local; nothing is uploaded"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (message.isNotBlank()) {
            item {
                Text(message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
@Composable
private fun UpdateSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<cn.levanll.stoveside.update.UpdateChecker.Release?>(null) }
    var status by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }
    var apkFile by remember { mutableStateOf<java.io.File?>(null) }

    SectionCard(I18n.t("版本更新", "Updates")) {
        Text(
            I18n.t("当前版本 ${BuildConfig.VERSION_NAME}", "Current ${BuildConfig.VERSION_NAME}"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = !checking && !downloading,
                onClick = {
                    checking = true
                    status = ""
                    scope.launch {
                        val r = cn.levanll.stoveside.update.UpdateChecker.check()
                        checking = false
                        r.onSuccess {
                            release = it
                            status = if (cn.levanll.stoveside.update.UpdateChecker.hasUpdate(it)) {
                                I18n.t("发现新版本 ${it.versionName}", "New version ${it.versionName}")
                            } else {
                                I18n.t("已是最新版", "Already up to date")
                            }
                        }.onFailure {
                            status = I18n.t("检查失败：${it.message}", "Check failed: ${it.message}")
                        }
                    }
                },
            ) { Text(if (checking) I18n.t("检查中…", "Checking…") else I18n.t("检查更新", "Check")) }

            if (apkFile != null) {
                OutlinedButton(onClick = {
                    if (cn.levanll.stoveside.update.ApkInstaller.canInstall(ctx)) {
                        cn.levanll.stoveside.update.ApkInstaller.install(ctx, apkFile!!)
                    } else {
                        cn.levanll.stoveside.update.ApkInstaller.openInstallPermission(ctx)
                        status = I18n.t("请允许安装未知来源应用后返回", "Allow installs from this app, then return")
                    }
                }) { Text(I18n.t("安装", "Install")) }
            } else {
                OutlinedButton(
                    enabled = release != null && !downloading && release!!.apkUrl.isNotBlank(),
                    onClick = {
                        val rel = release ?: return@OutlinedButton
                        downloading = true
                        status = ""
                        scope.launch {
                            status = runCatching {
                                val file = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val dir = cn.levanll.stoveside.update.ApkInstaller.updatesDir(ctx)
                                    dir.listFiles()?.forEach { it.delete() }
                                    val target = java.io.File(dir, rel.apkName.ifBlank { "update.apk" })
                                    java.net.URL(rel.apkUrl).openStream().use { input ->
                                        target.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    target
                                }
                                apkFile = file
                                I18n.t("下载完成，点「安装」", "Downloaded — tap Install")
                            }.getOrElse {
                                I18n.t("下载失败：${it.message}", "Download failed: ${it.message}")
                            }
                            downloading = false
                        }
                    },
                ) { Text(if (downloading) I18n.t("下载中…", "Downloading…") else I18n.t("下载到手机", "Download")) }
            }
        }
        if (status.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        release?.let { r ->
            if (r.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    r.notes.take(400),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            I18n.t("安装包下载到本应用私有目录，不占用系统内部存储", "APK goes to this app's own folder, not system storage"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}