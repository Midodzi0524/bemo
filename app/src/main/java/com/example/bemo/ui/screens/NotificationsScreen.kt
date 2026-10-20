package com.example.bemo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bemo.model.AlertThreshold
import com.example.bemo.model.BudgetAlert
import com.example.bemo.model.Formatters
import com.example.bemo.notification.NotificationHelper
import com.example.bemo.ui.BemoTab
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.components.BemoBentoCard
import com.example.ui.theme.LocalBemoColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: BemoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalBemoColors.current
    val alerts by viewModel.alerts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var hasPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Budget Alerts", fontWeight = FontWeight.Bold)
                        if (alerts.any { !it.isRead }) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(8.dp)
                            ) {}
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (alerts.any { !it.isRead }) {
                        IconButton(
                            onClick = { viewModel.markAllAlertsRead() },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "Mark all read")
                        }
                    }
                    if (alerts.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllAlerts() },
                            modifier = Modifier.testTag("clear_alerts_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all alerts")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Android 13+ Notification Permission Request Banner if not granted
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("notification_permission_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Enable Device Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Receive immediate alerts on your Samsung A05 or device when your spending reaches 80% or 100% of any budget limit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("enable_notifications_button")
                            ) {
                                Text("Allow Notifications")
                            }
                        }
                    }
                }
            }

            // Notification Threshold Settings & Testing Controls
            item {
                BemoBentoCard(
                    backgroundColor = extended.surfaceAlt,
                    borderColor = extended.border
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Alert Threshold Rules",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 80% Rule Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "80% Warning Alert",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Triggers when you hit 80% of a category's monthly allocation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textMuted
                                )
                            }
                            Switch(
                                checked = settings.notifyAt80Percent,
                                onCheckedChange = {
                                    viewModel.updateNotificationSettings(
                                        enabled = settings.budgetNotificationsEnabled,
                                        at80 = it,
                                        at100 = settings.notifyAt100Percent
                                    )
                                },
                                modifier = Modifier.testTag("toggle_80_percent")
                            )
                        }

                        HorizontalDivider(color = extended.border.copy(alpha = 0.5f))

                        // 100% Rule Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "100% Limit Reached Alert",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Triggers immediately when 100% or more of an envelope is spent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textMuted
                                )
                            }
                            Switch(
                                checked = settings.notifyAt100Percent,
                                onCheckedChange = {
                                    viewModel.updateNotificationSettings(
                                        enabled = settings.budgetNotificationsEnabled,
                                        at80 = settings.notifyAt80Percent,
                                        at100 = it
                                    )
                                },
                                modifier = Modifier.testTag("toggle_100_percent")
                            )
                        }

                        HorizontalDivider(color = extended.border.copy(alpha = 0.5f))

                        // Test Notification Buttons
                        Text(
                            text = "Test Notifications on this device:",
                            style = MaterialTheme.typography.labelMedium,
                            color = extended.textMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.sendTestAlert(AlertThreshold.WARNING_80)
                                },
                                modifier = Modifier.weight(1f).testTag("test_80_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Test 80% Alert", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = {
                                    viewModel.sendTestAlert(AlertThreshold.LIMIT_100)
                                },
                                modifier = Modifier.weight(1f).testTag("test_100_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Test 100% Alert", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Alerts Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Alerts (${alerts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (alerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = extended.surfaceAlt)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "All Limits Respected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "None of your categories have exceeded 80% or 100% of their set expense budgets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(alerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onMarkRead = { viewModel.markAlertRead(alert.id) },
                        onViewBudget = {
                            viewModel.selectTab(BemoTab.BUDGET)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AlertItemCard(
    alert: BudgetAlert,
    onMarkRead: () -> Unit,
    onViewBudget: () -> Unit
) {
    val extended = LocalBemoColors.current
    val is100 = alert.threshold == AlertThreshold.LIMIT_100

    val bannerBg = if (is100) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = if (alert.isRead) 0.35f else 0.7f)
    } else {
        extended.surfaceAlt
    }

    val bannerBorder = if (is100) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    } else {
        extended.warning.copy(alpha = 0.6f)
    }

    val progressFraction = if (alert.limitPesewas > 0) {
        (alert.spentPesewas.toFloat() / alert.limitPesewas.toFloat()).coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkRead() }
            .testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(bannerBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = alert.categoryEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (is100) MaterialTheme.colorScheme.error else extended.warning.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (is100) "🚨 100% LIMIT REACHED" else "⚠️ 80% WARNING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (is100) MaterialTheme.colorScheme.onError else extended.textMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!alert.isRead) {
                    Surface(
                        shape = CircleShape,
                        color = if (is100) MaterialTheme.colorScheme.error else extended.warning,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (is100) MaterialTheme.colorScheme.error else extended.warning,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(alert.timestampMillis))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textMuted
                )

                TextButton(
                    onClick = onViewBudget,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("View Budget", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
