package cn.levanll.stoveside.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.levanll.stoveside.domain.TimeText
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.ui.CookView
import cn.levanll.stoveside.ui.StepView
import cn.levanll.stoveside.ui.StoveViewModel

@Composable
fun CooksScreen(vm: StoveViewModel, modifier: Modifier = Modifier) {
    val cooks by vm.cooks.collectAsState()
    val recipes by vm.recipes.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        I18n.t("在煮的东西", "On the stove"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        I18n.t("${cooks.size} 个流程 · 每秒刷新", "${cooks.size} running · live"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (recipes.isNotEmpty()) {
            item {
                Text(
                    I18n.t("从食谱开始一份", "Start a new cook"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipes.take(3).forEach { r ->
                        Card(
                            onClick = { vm.startCook(r) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    r.name,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (cooks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍲", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            I18n.t("先建一份食谱，再开火", "Create a recipe, then start cooking"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(cooks, key = { it.cook.id }) { cook -> CookCard(vm, cook) }
    }
}

@Composable
private fun CookCard(vm: StoveViewModel, cook: CookView) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${cook.recipeName} ${cook.cook.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        I18n.t(
                            "已完成 ${cook.doneCount}/${cook.totalCount} 步",
                            "${cook.doneCount}/${cook.totalCount} steps done",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { vm.deleteCook(cook.cook) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        I18n.t("删除", "Delete"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            cook.steps.forEach { sv ->
                StepRow(vm, cook, sv)
            }
        }
    }
}

@Composable
private fun StepRow(vm: StoveViewModel, cook: CookView, sv: StepView) {
    val step = sv.step
    val running = step.status == "running"
    val done = step.status == "done" || step.status == "skipped"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(
                    when {
                        done -> MaterialTheme.colorScheme.primary
                        running -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(
                    "${step.orderNo + 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        var editingNote by remember(step.id) { mutableStateOf(false) }
        if (editingNote) {
            NoteDialog(
                initial = step.note,
                onDismiss = { editingNote = false },
                onSave = { text ->
                    vm.updateNote(step, text)
                    editingNote = false
                },
            )
        }
        // 备注整块可点：有备注显示内容，没备注显示占位提示——两处都能进入编辑
        Column(
            Modifier
                .weight(1f)
                .clickable { editingNote = true },
        ) {
            Text(
                step.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                step.note.ifBlank { I18n.t("点这里写这次的做法", "Tap to note this run") },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (step.durationSec > 0) {
            Text(
                if (running) TimeText.clock(sv.remainingMs) else TimeText.duration(step.durationSec),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (running) FontWeight.Bold else FontWeight.Normal,
                color = if (running && sv.remainingMs < 60_000) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!done) {
            if (running) {
                TextButton(onClick = { vm.addOneMinute(step) }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("+1'", style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = { if (running) vm.pauseStep(step) else vm.startStep(step) }) {
                Icon(
                    if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    if (running) I18n.t("暂停", "Pause") else I18n.t("开始", "Start"),
                    Modifier.size(18.dp),
                )
            }
            IconButton(onClick = { vm.completeStep(cook.cook.id, step) }) {
                Icon(Icons.Outlined.Check, I18n.t("完成", "Done"), Modifier.size(18.dp))
            }
        }
    }
}

/** 实例备注编辑：只改这次烹饪的 CookStep.note，不回写模板。 */
@Composable
private fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("这次的备注", "Note for this run")) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(I18n.t("比如：火大了，下次少两分钟", "e.g. heat was high, cut 2 min next time")) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) { Text(I18n.t("保存", "Save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("取消", "Cancel")) }
        },
    )
}
