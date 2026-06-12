package com.tvanime.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tvanime.app.ui.navigation.TVAnimeNavHost
import com.tvanime.app.ui.screens.PermissionGateScreen
import com.tvanime.app.ui.theme.TVAnimeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var permissionsGranted by remember { mutableStateOf(hasRuntimePermissions()) }
            var permissionFlowDismissed by remember { mutableStateOf(false) }
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionsGranted = hasRuntimePermissions()
                permissionFlowDismissed = true
            }

            TVAnimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!permissionsGranted && !permissionFlowDismissed) {
                        PermissionGateScreen(
                            onRequestPermissions = { permissionLauncher.launch(runtimePermissions()) },
                            onContinueWithoutPermissions = { permissionFlowDismissed = true }
                        )
                    } else {
                        TVAnimeNavHost(permissionsGranted = permissionsGranted)
                    }
                }
            }
        }
    }

    private fun runtimePermissions(): Array<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun hasRuntimePermissions(): Boolean = runtimePermissions().all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}
