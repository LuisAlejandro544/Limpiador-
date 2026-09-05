package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.shizuku.ShizukuHelper
import com.example.ui.AppScreen
import com.example.ui.CleanScreen
import com.example.ui.CleanViewModel
import com.example.ui.DebugConsoleScreen
import com.example.ui.OtherStorageScreen
import com.example.ui.RamCleanScreen
import com.example.ui.theme.MyApplicationTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: CleanViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permiso de notificaciones para LeakCanary y avisos del sistema
        }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuHelper.SHIZUKU_REQUEST_CODE) {
                viewModel.refreshStorageAndShizuku(this)
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.refreshStorageAndShizuku(this)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        viewModel.refreshStorageAndShizuku(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permiso de notificaciones en Android 13+ para LeakCanary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        try {
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            // Handled gracefully if Shizuku provider isn't available
        }

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        AppScreen.DASHBOARD -> CleanScreen(
                            viewModel = viewModel,
                            onNavigateToOtherStorage = { viewModel.navigateToOtherStorage() }
                        )
                        AppScreen.OTHER_STORAGE -> OtherStorageScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToDashboard() }
                        )
                        AppScreen.RAM_CLEANER -> RamCleanScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToDashboard() }
                        )
                        AppScreen.DEBUG_CONSOLE -> DebugConsoleScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToDashboard() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStorageAndShizuku(this)
        viewModel.checkStoragePermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            // Handled gracefully
        }
    }
}
