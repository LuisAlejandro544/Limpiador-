package com.example

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.shizuku.ShizukuHelper
import com.example.ui.AppScreen
import com.example.ui.CleanScreen
import com.example.ui.CleanViewModel
import com.example.ui.OtherStorageScreen
import com.example.ui.theme.MyApplicationTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: CleanViewModel by viewModels()

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
