package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import com.example.myapplication.security.EncryptionManager
import com.example.myapplication.service.MonitoringService
import com.example.myapplication.ui.screens.LauncherScreen
import com.example.myapplication.ui.screens.ParentPortalScreen
import com.example.myapplication.ui.screens.VaultScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.createGuardianViewModel

class MainActivity : ComponentActivity() {
    private val permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.QUERY_ALL_PACKAGES
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startMonitoringService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissions()

        setContent {
            MyApplicationTheme {
                val viewModel = remember { createGuardianViewModel(this@MainActivity) }
                val encryptionManager = remember { EncryptionManager(this@MainActivity) }
                var showVault by remember { mutableStateOf(false) }
                var showParentPortal by remember { mutableStateOf(false) }
                var cornerTapCount by remember { mutableIntStateOf(0) }
                var lastCornerTapTime by remember { mutableLongStateOf(0L) }
                var isLongPressing by remember { mutableStateOf(false) }

                val showVaultState by viewModel.showVault.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1A))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val isTopRightCorner = offset.x > size.width - 100 && offset.y < 100
                                    if (isTopRightCorner) {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastCornerTapTime < 500) {
                                            cornerTapCount++
                                        } else {
                                            cornerTapCount = 1
                                        }
                                        lastCornerTapTime = currentTime

                                        if (cornerTapCount >= 3) {
                                            showParentPortal = true
                                            cornerTapCount = 0
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    when {
                        showParentPortal -> ParentPortalScreen(
                            encryptionManager = encryptionManager,
                            onClose = { showParentPortal = false }
                        )

                        showVaultState -> VaultScreen(
                            viewModel = viewModel,
                            onClose = { viewModel.toggleVault() }
                        )

                        else -> LauncherScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> mutableLongStateOf(value: Long): androidx.compose.runtime.MutableState<Long> {
    return mutableStateOf(value) as androidx.compose.runtime.MutableState<Long>
}