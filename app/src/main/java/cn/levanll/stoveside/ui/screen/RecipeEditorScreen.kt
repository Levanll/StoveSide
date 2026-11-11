package cn.levanll.stoveside.ui.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.data.entity.Step
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.ui.StoveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(vm: StoveViewModel, onClose: () -> Unit) {
    val draft by vm.draft.collectAsState()
    val preview by vm.parsePreview.collectAsState()
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paste by remember { mutableStateOf("") }
    var showPaste by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(I18n.t("新建食谱", "New recipe")) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.ArrowBack, I18n.t("返回", "Back"))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.saveRecipe(Recipe(name = name.ifBlank { I18n.t("未命名食谱", "Untitled") }, note = note)) { onClose() }
                        },
                        enabled = draft.isNotEmpty(),
                    ) { Text(I18n.t("保存", "Save")) }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(I18n.t("菜名", "Dish name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(I18n.t("整体备注（可选）", "Notes (optional)")) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showPaste = !showPaste }) {
                        Text(I18n.t("粘贴菜谱自动拆分", "Paste & auto-split"))
                    }
                    OutlinedButton(onClick = {
                        vm.addDraftStep(Step(recipeId = 0, orderNo = draft.size, title = "", durationSec = 0))
                    }) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(I18n.t("加一步", "Add step"))
                    }
                }
            }

            if (showPaste) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = paste,
                                onValueChange = { paste = it },
                                label = { Text(I18n.t("把菜谱原文粘进来", "Paste the recipe text")) },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { vm.previewParse(paste, name.ifBlank { I18n.t("未命名食谱", "Untitled") }) },
                                    enabled = paste.isNotBlank(),
                                ) { Text(I18n.t("解析预览", "Preview")) }
                                if (preview != null) {
                                    Button(onClick = {
                                        if (name.isBlank()) name = preview!!.name
                                        vm.applyPreview()
                                        showPaste = false
                                        paste = ""
                                    }) { Text(I18n.t("采用 ${preview!!.steps.size} 步", "Use ${preview!!.steps.size} steps")) }
                                }
                            }
                            preview?.let { p ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    I18n.t("识别结果可再编辑", "Parsed result is editable"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                p.steps.take(12).forEachIndexed { i, s ->
                                    Text(
                                        "${i + 1}. ${s.title}  ·  ${s.durationSec}s  ${s.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            itemsIndexed(draft) { index, step ->
                StepEditorCard(
                    step = step,
                    onChange = { updated ->
                        val list = draft.toMutableList()
                        list[index] = updated
                        // draft 是只读快照，这里通过 vm 的替换接口落回去
                        vm.replaceDraft(list)
                    },
                    onDelete = { vm.removeDraftStep(index) },
                )
            }

            if (draft.isEmpty()) {
                item {
                    Text(
                        I18n.t("粘贴菜谱或手动加一步", "Paste a recipe or add steps manually"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepEditorCard(step: Step, onChange: (Step) -> Unit, onDelete: () -> Unit) {
    var title by remember(step.id, step.orderNo) { mutableStateOf(step.title) }
    var minutes by remember(step.id, step.orderNo) { mutableStateOf(if (step.durationSec > 0) (step.durationSec / 60).toString() else "") }
    var note by remember(step.id, step.orderNo) { mutableStateOf(step.note) }

    fun push() {
        val m = minutes.toIntOrNull() ?: 0
        onChange(step.copy(title = title, durationSec = m * 60, note = note))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                Text(
                    "${I18n.t("第", "Step ")}${step.orderNo + 1}${I18n.t("步", "")}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, I18n.t("删除", "Delete"), Modifier.size(16.dp))
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; push() },
                label = { Text(I18n.t("这一步做什么", "What to do")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.all { c -> c.isDigit() }) { minutes = it; push() } },
                    label = { Text(I18n.t("分钟（留空=不计时）", "Minutes (blank = none)")) },
                    singleLine = true,
                    modifier = Modifier.width(180.dp),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; push() },
                    label = { Text(I18n.t("备注", "Note")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
