package cn.levanll.stoveside

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.levanll.stoveside.service.Notifier
import cn.levanll.stoveside.service.TimerService
import cn.levanll.stoveside.ui.StoveViewModel
import cn.levanll.stoveside.ui.screen.AppRoot
import cn.levanll.stoveside.ui.theme.StoveTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 拒绝也不阻断计时 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannels(this)
        askNotificationPermission()
        // 只在确实有进行中的计时时才拉起前台服务；
        // 否则冷启动就会出现一个常驻的"没有进行中的计时"通知。
        lifecycleScope.launch {
            val running = withContext(Dispatchers.IO) {
                runCatching { StoveApp.instance.repository.runningSteps() }.getOrDefault(emptyList())
            }
            if (running.isNotEmpty()) TimerService.start(this@MainActivity)
        }
        setContent {
            StoveTheme {
                val vm: StoveViewModel = viewModel()
                AppRoot(vm = vm)
            }
        }
    }

    /** Android 13+ 必须显式申请，否则通知发不出去（A1 到期提醒的前提）。 */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
