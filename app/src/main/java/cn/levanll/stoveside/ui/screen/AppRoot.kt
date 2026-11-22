package cn.levanll.stoveside.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cn.levanll.stoveside.domain.TimeText
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.ui.AlarmPrompt
import cn.levanll.stoveside.ui.StoveViewModel

@Composable
fun AppRoot(vm: StoveViewModel) {
    var tab by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    // 语言是全局状态，读一次即可触发重组
    val lang = I18n.lang
    val prompt by vm.alarmPrompt.collectAsState()

    // 弹窗挂在 Scaffold 外层：不论当前在哪个 tab，到点都会弹
    prompt?.let { p ->
        AlarmDialog(
            prompt = p,
            onAck = { vm.ackAlarm() },
            onExtend = { p.step?.let { vm.addOneMinute(it) } },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                    label = { Text(I18n.t("在煮", "Cooking")) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                    label = { Text(I18n.t("食谱", "Recipes")) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(I18n.t("设置", "Settings")) },
                )
            }
        },
    ) { pad ->
        when (tab) {
            0 -> CooksScreen(vm, Modifier.padding(pad))
            1 -> RecipesScreen(vm, Modifier.padding(pad))
            else -> SettingsScreen(vm, Modifier.padding(pad))
        }
    }
}

/**
 * 到点弹窗。和通知栏的「知道了」同一条语义：停铃、记下这轮到点，
 * 但**不改步骤状态**——这锅客观上还在跑，「+1 分钟」与勾选仍留在原位。
 */
@Composable
private fun AlarmDialog(
    prompt: AlarmPrompt,
    onAck: () -> Unit,
    onExtend: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAck,
        title = { Text(I18n.t("到点了", "Timer done")) },
        text = {
            Column {
                Text(prompt.title, style = MaterialTheme.typography.bodyLarge)
                if (prompt.subtitle.isNotBlank()) {
                    Text(
                        prompt.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (prompt.overdueMs > 0L) {
                    val overdueSec = ((prompt.overdueMs + 999) / 1000).toInt()
                    // 隔夜才回到 App 时「已过 236 小时」既没用又像 bug；超过 1 小时只说早就到点了
                    Text(
                        if (overdueSec > 3600) {
                            I18n.t("早就到点了", "Due a while ago")
                        } else {
                            I18n.t(
                                "已过 " + TimeText.duration(overdueSec),
                                "Overdue by " + TimeText.duration(overdueSec),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (prompt.extraCount > 0) {
                    Text(
                        I18n.t(
                            "还有 " + prompt.extraCount + " 个也到点了",
                            prompt.extraCount.toString() + " more also due",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAck) { Text(I18n.t("知道了", "Got it")) }
        },
        dismissButton = if (prompt.step != null) {
            { TextButton(onClick = onExtend) { Text(I18n.t("+1 分钟", "+1 min")) } }
        } else {
            null
        },
    )
}
