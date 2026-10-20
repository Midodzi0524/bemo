package com.example.bemo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.ui.BemoTab
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BemoTopBar(
    monthTitle: String = "October 2026",
    isAmountsHidden: Boolean = false,
    unreadAlertsCount: Int = 0,
    onToggleHideAmounts: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onMonthSelected: (String) -> Unit = {}
) {
    val extended = LocalBemoColors.current
    var showMonthDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Month selector badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(extended.surfaceAlt)
                    .clickable { showMonthDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("topbar_month_badge")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Month",
                    tint = extended.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Month",
                    tint = extended.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Right icons: Insights, Accounts badge, Quick-hide eye, Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenInsights,
                    modifier = Modifier.size(38.dp).testTag("topbar_insights_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = "Financial Insights",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onOpenAccounts,
                    modifier = Modifier.size(38.dp).testTag("topbar_accounts_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = "Accounts",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onToggleHideAmounts,
                    modifier = Modifier.size(38.dp).testTag("topbar_hide_button")
                ) {
                    Icon(
                        imageVector = if (isAmountsHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isAmountsHidden) "Show amounts" else "Hide amounts",
                        tint = if (isAmountsHidden) MaterialTheme.colorScheme.primary else extended.textMuted
                    )
                }

                // Notification Bell with badge
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.size(38.dp).testTag("topbar_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadAlertsCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(text = if (unreadAlertsCount > 9) "9+" else unreadAlertsCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (unreadAlertsCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Budget Alerts",
                            tint = if (unreadAlertsCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Sync indicator dot (Green = up to date)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(extended.success)
                )

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(38.dp).testTag("topbar_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

    if (showMonthDialog) {
        val months = listOf(
            "August 2026",
            "September 2026",
            "October 2026",
            "November 2026"
        )
        AlertDialog(
            onDismissRequest = { showMonthDialog = false },
            title = { Text("Select Budget Period", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    months.forEach { month ->
                        val isCurrent = month.startsWith("October")
                        Surface(
                            onClick = {
                                onMonthSelected(month)
                                showMonthDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (month == monthTitle) extended.primaryTint else extended.surfaceAlt,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = month,
                                    fontWeight = if (month == monthTitle) FontWeight.Bold else FontWeight.Medium,
                                    color = if (month == monthTitle) extended.onPrimaryTint else MaterialTheme.colorScheme.onSurface
                                )
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = extended.successTint
                                    ) {
                                        Text(
                                            text = "Current",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = extended.successText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BemoBentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalBemoColors.current.border,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val cardModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(padding)

    Column(modifier = cardModifier) {
        content()
    }
}

@Composable
fun PaceProgressBar(
    progressFraction: Float,
    timeElapsedFraction: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    val extended = LocalBemoColors.current
    val animatedProgress by animateFloatAsState(targetValue = progressFraction.coerceIn(0f, 1f), label = "prog")

    val fillColor = when {
        progressFraction >= 1.0f -> extended.danger
        progressFraction >= 0.8f -> extended.warning
        else -> extended.success
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(extended.surfaceAlt)
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(fillColor, RoundedCornerShape(5.dp))
        )

        // Subtle pace tick for elapsed time in month
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .offset(x = 0.dp) // Handled dynamically or via Canvas below
        )
    }
}

@Composable
fun BemoBottomNavigation(
    selectedTab: BemoTab,
    onTabSelected: (BemoTab) -> Unit,
    onFabClick: () -> Unit
) {
    val extended = LocalBemoColors.current
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.border(1.dp, extended.border, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            NavigationBarItem(
                selected = selectedTab == BemoTab.HOME,
                onClick = { onTabSelected(BemoTab.HOME) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == BemoTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = extended.primaryTint
                ),
                modifier = Modifier.testTag("nav_home")
            )
            NavigationBarItem(
                selected = selectedTab == BemoTab.BUDGET,
                onClick = { onTabSelected(BemoTab.BUDGET) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == BemoTab.BUDGET) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                        contentDescription = "Budget"
                    )
                },
                label = { Text("Budget", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = extended.primaryTint
                ),
                modifier = Modifier.testTag("nav_budget")
            )
            NavigationBarItem(
                selected = selectedTab == BemoTab.TRANSACTIONS,
                onClick = { onTabSelected(BemoTab.TRANSACTIONS) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == BemoTab.TRANSACTIONS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                        contentDescription = "Transactions"
                    )
                },
                label = { Text("Txns", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = extended.primaryTint
                ),
                modifier = Modifier.testTag("nav_transactions")
            )
            NavigationBarItem(
                selected = selectedTab == BemoTab.WISHLIST,
                onClick = { onTabSelected(BemoTab.WISHLIST) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == BemoTab.WISHLIST) Icons.Filled.Stars else Icons.Outlined.Stars,
                        contentDescription = "Wishlist"
                    )
                },
                label = { Text("Wishlist", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = extended.primaryTint
                ),
                modifier = Modifier.testTag("nav_wishlist")
            )
        }
    }
}
