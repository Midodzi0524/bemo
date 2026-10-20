package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bemo.model.ThemeMode
import com.example.bemo.ui.BemoTab
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.SubScreen
import com.example.bemo.ui.components.BemoBottomNavigation
import com.example.bemo.ui.components.BemoTopBar
import com.example.bemo.ui.screens.*
import com.example.ui.theme.BemoTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            BemoTheme {
                if (currentUser == null) {
                    SignInScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                        }
                    )
                } else {
                    BemoAppContent(currentUserId = currentUser!!.uid)
                }
            }
        }
    }
}

@Composable
fun BemoAppContent(currentUserId: String) {
    val viewModel: BemoViewModel = viewModel()

    LaunchedEffect(currentUserId) {
        viewModel.repository.startSync(currentUserId)
    }

    val settings by viewModel.settings.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    val addInitialType by viewModel.addSheetInitialType.collectAsState()
    val unreadAlertsCount by viewModel.unreadAlertsCount.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val canUndo by viewModel.canUndoDelete.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Snackbar listener
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            val result = if (canUndo) {
                snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
            }
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
            viewModel.clearSnackbar()
        }
    }

    // Android Back button handling
    BackHandler(enabled = showAddSheet || currentSubScreen != SubScreen.MAIN || currentTab != BemoTab.HOME) {
        when {
            showAddSheet -> viewModel.closeAddSheet()
            currentSubScreen != SubScreen.MAIN && currentSubScreen != SubScreen.WELCOME && currentSubScreen != SubScreen.LOCK -> {
                viewModel.navigateTo(SubScreen.MAIN)
            }
            currentTab != BemoTab.HOME -> {
                viewModel.selectTab(BemoTab.HOME)
            }
        }
    }

    BemoTheme(darkTheme = isDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentSubScreen == SubScreen.MAIN) {
                    BemoTopBar(
                        monthTitle = "October 2026",
                        isAmountsHidden = settings.hideAmounts,
                        unreadAlertsCount = unreadAlertsCount,
                        onToggleHideAmounts = { viewModel.toggleHideAmounts() },
                        onOpenNotifications = { viewModel.navigateTo(SubScreen.NOTIFICATIONS) },
                        onOpenSettings = { viewModel.navigateTo(SubScreen.SETTINGS) },
                        onOpenAccounts = { viewModel.navigateTo(SubScreen.ACCOUNTS) },
                        onOpenInsights = { viewModel.navigateTo(SubScreen.INSIGHTS) }
                    )
                }
            },
            bottomBar = {
                if (currentSubScreen == SubScreen.MAIN) {
                    BemoBottomNavigation(
                        selectedTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        onFabClick = { viewModel.openAddSheet() }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (currentSubScreen) {
                    SubScreen.WELCOME -> {
                        WelcomeScreen(
                            viewModel = viewModel,
                            onEnterApp = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.LOCK -> {
                        LockScreen(
                            viewModel = viewModel,
                            onUnlocked = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.INSIGHTS -> {
                        InsightsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.ACCOUNTS -> {
                        AccountsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.RECURRING -> {
                        RecurringBillsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.NOTIFICATIONS -> {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(SubScreen.MAIN) }
                        )
                    }
                    SubScreen.MAIN, SubScreen.ONBOARDING -> {
                        when (currentTab) {
                            BemoTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenAdd = { viewModel.openAddSheet() }
                            )
                            BemoTab.BUDGET -> BudgetScreen(viewModel = viewModel)
                            BemoTab.TRANSACTIONS -> TransactionsScreen(
                                viewModel = viewModel,
                                onOpenAdd = { viewModel.openAddSheet() }
                            )
                            BemoTab.WISHLIST -> WishlistScreen(viewModel = viewModel)
                        }
                    }
                }

                // Add Transaction Bottom Sheet
                if (showAddSheet) {
                    AddTransactionSheet(
                        viewModel = viewModel,
                        initialType = addInitialType,
                        onDismiss = { viewModel.closeAddSheet() }
                    )
                }
            }
        }
    }
}
