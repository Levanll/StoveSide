package cn.levanll.stoveside.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import cn.levanll.stoveside.data.entity.Recipe
import cn.levanll.stoveside.domain.RecipeParser
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
            Modifier.fillMaxSize().padding(pad).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(I18n.t("菜名", "Dish name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().scrollIntoViewOnFocus(),
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(I18n.t("整体备注（可选）", "Notes (optional)")) },
                    modifier = Modifier.fillMaxWidth().scrollIntoViewOnFocus(),
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

        if (showPaste) {
            ModalBottomSheet(
                onDismissRequest = {
                    showPaste = false
                    paste = ""
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                // 弹层自带 IME 处理，粘贴框独占一屏，最下方文字不会再被键盘盖住
            ) {
                PasteSheet(
                    paste = paste,
                    onPasteChange = { paste = it },
                    preview = preview,
                    onPreview = { vm.previewParse(paste, name.ifBlank { I18n.t("未命名食谱", "Untitled") }) },
                    onUse = {
                        if (name.isBlank()) name = preview!!.name
                        vm.applyPreview()
                        showPaste = false
                        paste = ""
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasteSheet(
    paste: String,
    onPasteChange: (String) -> Unit,
    preview: RecipeParser.ParsedRecipe?,
    onPreview: () -> Unit,
    onUse: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Text(
            I18n.t("粘贴菜谱原文", "Paste the recipe text"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        // 输入框固定占位，剩余空间留给识别结果，结果多时可滚动
        OutlinedTextField(
            value = paste,
            onValueChange = onPasteChange,
            placeholder = { Text(I18n.t("把菜谱原文粘进来", "Paste the recipe text")) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 260.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPreview, enabled = paste.isNotBlank()) {
                Text(I18n.t("解析预览", "Preview"))
            }
            if (preview != null) {
                Button(onClick = onUse) {
                    Text(I18n.t("采用 ${preview.steps.size} 步", "Use ${preview.steps.size} steps"))
                }
            }
        }
        preview?.let { p ->
            Spacer(Modifier.height(8.dp))
            Text(
                I18n.t("识别结果可再编辑", "Parsed result is editable"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                items(p.steps.take(12).size) { i ->
                    val s = p.steps[i]
                    Text(
                        "${i + 1}. ${s.title}  ·  ${s.durationSec}s  ${s.note}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/**
 * 输入框获得焦点时把它滚进可视区。
 * LazyColumn 不会仅因为输入框被聚焦就自动滚动，键盘弹起后排在下面的输入框会被挡住。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.scrollIntoViewOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { if (it.isFocused) scope.launch { requester.bringIntoView() } }
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
                modifier = Modifier.fillMaxWidth().scrollIntoViewOnFocus(),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.all { c -> c.isDigit() }) { minutes = it; push() } },
                    label = { Text(I18n.t("分钟（留空=不计时）", "Minutes (blank = none)")) },
                    singleLine = true,
                    modifier = Modifier.width(180.dp).scrollIntoViewOnFocus(),
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
