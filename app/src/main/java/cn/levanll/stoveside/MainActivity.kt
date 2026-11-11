package cn.levanll.stoveside

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.levanll.stoveside.ui.StoveViewModel
import cn.levanll.stoveside.ui.screen.AppRoot
import cn.levanll.stoveside.ui.theme.StoveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoveTheme {
                val vm: StoveViewModel = viewModel()
                AppRoot(vm = vm)
            }
        }
    }
}
