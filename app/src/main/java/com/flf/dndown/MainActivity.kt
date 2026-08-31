package com.flf.dndown

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flf.dndown.service.DnDServiceState
import com.flf.dndown.core.hasPostNotificationsPermission
import com.flf.dndown.core.isIgnoringBatteryOptimizations
import com.flf.dndown.core.notificationManager
import com.flf.dndown.core.requestDndPermission
import com.flf.dndown.ui.theme.DnDownTheme
import androidx.core.net.toUri
import com.flf.dndown.service.DnDService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DnDownTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationManager = context.notificationManager

    val isServiceRunning by DnDServiceState.isRunning.collectAsState()
    var hasNotificationPolicyAccess by remember {
        mutableStateOf(notificationManager.isNotificationPolicyAccessGranted)
    }
    var hasNotificationPermission by remember {
        mutableStateOf(context.hasPostNotificationsPermission())
    }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Refresh state when returning to activity (e.g., after granting DND access in settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPolicyAccess = notificationManager.isNotificationPolicyAccessGranted
                isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations()
                hasNotificationPermission = context.hasPostNotificationsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasNotificationPolicyAccess) {
                Text(
                    text = stringResource(R.string.permission_rationale),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = {
                    context.requestDndPermission()
                }) {
                    Text(stringResource(R.string.grant_dnd_access))
                }
            } else if (!isIgnoringBatteryOptimizations) {
                Text(
                    text = stringResource(R.string.battery_optimization_rationale),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.grant_battery_exemption))
                }
            } else {
                Button(
                    onClick = {
                        if (isServiceRunning) {
                            context.stopService(Intent(context, DnDService::class.java))
                        } else {
                            if (!context.hasPostNotificationsPermission()) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                ContextCompat.startForegroundService(
                                    context,
                                    Intent(context, DnDService::class.java)
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isServiceRunning)
                            stringResource(R.string.toggle_service_off)
                        else
                            stringResource(R.string.toggle_service_on)
                    )
                }
            }
        }
    }
}
