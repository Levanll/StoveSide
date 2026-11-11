package cn.levanll.stoveside.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cn.levanll.stoveside.i18n.I18n
import cn.levanll.stoveside.ui.StoveViewModel

@Composable
fun AppRoot(vm: StoveViewModel) {
    var tab by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    // 语言是全局状态，读一次即可触发重组
    val lang = I18n.lang

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
