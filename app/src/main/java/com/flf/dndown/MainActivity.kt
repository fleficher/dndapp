package com.flf.dndown

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.flf.dndown.core.hasPostNotificationsPermission
import com.flf.dndown.core.isIgnoringBatteryOptimizations
import com.flf.dndown.core.notificationManager
import com.flf.dndown.core.requestBatteryExemption
import com.flf.dndown.core.requestDndPermission
import com.flf.dndown.service.DnDService
import com.flf.dndown.service.DnDServiceState
import com.flf.dndown.ui.theme.DnDownTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Refresh state when returning to activity (e.g., after granting permissions in settings)
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

    val setupComplete = hasNotificationPolicyAccess &&
            hasNotificationPermission &&
            isIgnoringBatteryOptimizations

    var isStarting by remember { mutableStateOf(false) }

    LaunchedEffect(isServiceRunning) {
        if (isServiceRunning) {
            isStarting = false
        }
    }

    LaunchedEffect(isStarting) {
        if (isStarting) {
            delay(3.seconds)
            isStarting = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (setupComplete) {
                    ReadyView(isServiceRunning = isServiceRunning)
                } else {
                    SetupView(
                        hasNotificationPolicyAccess = hasNotificationPolicyAccess,
                        hasNotificationPermission = hasNotificationPermission,
                        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                        onGrantDnd = { context.requestDndPermission() },
                        onGrantNotifications = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onGrantBattery = { context.requestBatteryExemption() }
                    )
                }
            }

            if (setupComplete) {
                StartServiceButton(
                    isServiceRunning = isServiceRunning,
                    isStarting = isStarting,
                    enabled = !isStarting,
                    onClick = {
                        if (isServiceRunning) {
                            context.stopService(Intent(context, DnDService::class.java))
                        } else {
                            isStarting = true
                            ContextCompat.startForegroundService(
                                context,
                                Intent(context, DnDService::class.java)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupView(
    hasNotificationPolicyAccess: Boolean,
    hasNotificationPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onGrantDnd: () -> Unit,
    onGrantNotifications: () -> Unit,
    onGrantBattery: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.titleMedium
                )
                SetupRow(
                    label = stringResource(R.string.setup_dnd),
                    granted = hasNotificationPolicyAccess,
                    onAction = onGrantDnd
                )
                SetupRow(
                    label = stringResource(R.string.setup_notifications),
                    granted = hasNotificationPermission,
                    onAction = onGrantNotifications
                )
                SetupRow(
                    label = stringResource(R.string.setup_battery),
                    granted = isIgnoringBatteryOptimizations,
                    onAction = onGrantBattery
                )
            }
        }
    }
}

@Composable
private fun ReadyView(isServiceRunning: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ServiceStatusCard(isServiceRunning = isServiceRunning)
        Text(
            text = stringResource(R.string.hint_face_down),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ServiceStatusCard(isServiceRunning: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isServiceRunning) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = if (isServiceRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(
                        if (isServiceRunning) R.string.status_running else R.string.status_stopped
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetupRow(label: String, granted: Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (!granted) {
            TextButton(onClick = onAction) {
                Text(stringResource(R.string.action_grant))
            }
        }
    }
}

@Composable
private fun StartServiceButton(
    isServiceRunning: Boolean,
    isStarting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (isStarting && !isServiceRunning) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = stringResource(
                when {
                    isStarting && !isServiceRunning -> R.string.service_starting
                    isServiceRunning -> R.string.toggle_service_off
                    else -> R.string.toggle_service_on
                }
            )
        )
    }
}
