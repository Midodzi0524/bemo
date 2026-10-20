package com.example.bemo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bemo.data.BemoRepository
import com.example.bemo.model.*
import com.example.bemo.ui.components.MonthTrendPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class BemoTab {
    HOME, BUDGET, TRANSACTIONS, WISHLIST
}

enum class SubScreen {
    MAIN, INSIGHTS, ACCOUNTS, RECURRING, SETTINGS, WELCOME, ONBOARDING, LOCK, NOTIFICATIONS
}

data class CategorySpendProgress(
    val category: Category,
    val assignedPesewas: Long,
    val spentPesewas: Long,
    val progressFraction: Float,
    val leftPesewas: Long,
    val isOver: Boolean,
    val isAheadOfPace: Boolean
)

data class BemoDashboardState(
    val unassignedPesewas: Long = 0L,
    val totalIncomePesewas: Long = 0L,
    val totalSpentPesewas: Long = 0L,
    val totalSavedPesewas: Long = 0L,
    val totalAssignedPesewas: Long = 0L,
    val safeToSpendTodayPesewas: Long = 0L,
    val daysLeftInMonth: Int = 1,
    val netBalancePesewas: Long = 0L,
    val availableBalancePesewas: Long = 0L,
    val topCategories: List<CategorySpendProgress> = emptyList(),
    val upcomingBills: List<RecurringRule> = emptyList(),
    val needsConfirmationBills: List<RecurringRule> = emptyList(),
    val monthlyIncomeLimitPesewas: Long = 450000L,
    val monthlyExpenseLimitPesewas: Long = 350000L,
    val remainingBudgetPesewas: Long = 65000L,
    val budgetSpentFraction: Float = 0.0f,
    val isOverBudget: Boolean = false,
    val monthlyTrends: List<MonthTrendPoint> = emptyList()
)

class BemoViewModel(application: Application) : AndroidViewModel(application) {
    val repository = BemoRepository(application)

    val accounts = repository.accounts
    val categories = repository.categories
    val transactions = repository.transactions
    val budgetAllocations = repository.budgetAllocations
    val wishlistItems = repository.wishlistItems
    val recurringRules = repository.recurringRules
    val quickAddTemplates = repository.quickAddTemplates
    val settings = repository.settings
    val alerts = repository.alerts
    val unreadAlertsCount: StateFlow<Int> = repository.alerts
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI Navigation State
    private val _currentTab = MutableStateFlow(BemoTab.HOME)
    val currentTab: StateFlow<BemoTab> = _currentTab.asStateFlow()

    private val _currentSubScreen = MutableStateFlow(SubScreen.MAIN)
    val currentSubScreen: StateFlow<SubScreen> = _currentSubScreen.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _addSheetInitialType = MutableStateFlow(TransactionType.EXPENSE)
    val addSheetInitialType: StateFlow<TransactionType> = _addSheetInitialType.asStateFlow()

    // Transaction filters
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)

    // User Feedback / Snackbars
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _canUndoDelete = MutableStateFlow(false)
    val canUndoDelete: StateFlow<Boolean> = _canUndoDelete.asStateFlow()

    // Duplicate detection warning flag
    val duplicateWarningTxn = MutableStateFlow<Transaction?>(null)

    // Dashboard calculations
    val dashboardState: StateFlow<BemoDashboardState> = combine(
        combine(transactions, budgetAllocations, categories) { t, b, c -> Triple(t, b, c) },
        combine(accounts, recurringRules, settings) { a, r, s -> Triple(a, r, s) }
    ) { (txns, allocations, cats), (accs, rules, set) ->
        computeDashboard(txns, allocations, cats, accs, rules, set)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BemoDashboardState())

    init {
        // If not completed onboarding, start on Welcome screen
        if (!settings.value.hasCompletedOnboarding) {
            _currentSubScreen.value = SubScreen.WELCOME
        } else if (settings.value.pinEnabled) {
            _currentSubScreen.value = SubScreen.LOCK
        }
    }

    private fun computeDashboard(
        txns: List<Transaction>,
        allocations: Map<String, Long>,
        cats: List<Category>,
        accs: List<Account>,
        rules: List<RecurringRule>,
        set: AppSettings
    ): BemoDashboardState {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = Math.max(1, maxDaysInMonth - currentDay + 1)
        val timeElapsedFraction = currentDay.toFloat() / maxDaysInMonth.toFloat()

        var incomeTotal = 0L
        var spentTotal = 0L
        var savedTotal = 0L

        // Spend per category
        val spendByCat = mutableMapOf<String, Long>()

        for (t in txns) {
            if (t.isDeleted || t.status != TransactionStatus.POSTED) continue
            cal.timeInMillis = t.dateMillis
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                when (t.type) {
                    TransactionType.INCOME -> incomeTotal += t.amountPesewas
                    TransactionType.EXPENSE -> {
                        spentTotal += t.amountPesewas
                        t.categoryId?.let { cId ->
                            spendByCat[cId] = (spendByCat[cId] ?: 0L) + t.amountPesewas
                        }
                    }
                    TransactionType.SAVING -> savedTotal += t.amountPesewas
                    TransactionType.TRANSFER -> { /* Transfers don't impact income/spend totals */ }
                }
            }
        }

        // Total assigned
        val totalAssigned = allocations.values.sum()
        val unassigned = incomeTotal - totalAssigned

        // Category progress list
        val progressList = cats.filter { it.kind == CategoryKind.EXPENSE }.map { cat ->
            val assigned = allocations[cat.id] ?: 0L
            val spent = spendByCat[cat.id] ?: 0L
            val fraction = if (assigned > 0) (spent.toFloat() / assigned.toFloat()) else if (spent > 0) 1.0f else 0.0f
            val isOver = spent > assigned && assigned > 0
            val isAhead = fraction > (timeElapsedFraction + 0.15f) && assigned > 0
            CategorySpendProgress(
                category = cat,
                assignedPesewas = assigned,
                spentPesewas = spent,
                progressFraction = fraction,
                leftPesewas = Math.max(0L, assigned - spent),
                isOver = isOver,
                isAheadOfPace = isAhead
            )
        }

        // Top categories sorted by spent
        val topCats = progressList.filter { it.spentPesewas > 0 }.sortedByDescending { it.spentPesewas }

        // Safe to spend today: (totalAssigned - spentTotal) / daysLeft
        val remainingAssigned = Math.max(0L, totalAssigned - spentTotal)
        val safeToSpendToday = remainingAssigned / daysLeft

        // Net balance & Available balance
        var netBalance = 0L
        for (acc in accs) {
            var accBal = acc.openingBalancePesewas
            for (t in txns) {
                if (t.isDeleted || t.status != TransactionStatus.POSTED) continue
                when {
                    t.type == TransactionType.INCOME && t.accountId == acc.id -> accBal += t.amountPesewas
                    t.type == TransactionType.EXPENSE && t.accountId == acc.id -> accBal -= t.amountPesewas
                    t.type == TransactionType.SAVING && t.accountId == acc.id -> accBal -= t.amountPesewas
                    t.type == TransactionType.TRANSFER && t.toAccountId == acc.id -> accBal += t.amountPesewas
                    t.type == TransactionType.TRANSFER && t.accountId == acc.id -> accBal -= t.amountPesewas
                }
            }
            netBalance += accBal
        }
        val availableBalance = Math.max(0L, netBalance - savedTotal)

        // Upcoming and Needs confirmation
        val upcoming = rules.filter { it.isActive }.sortedBy { it.nextDueDateMillis }
        val toConfirm = upcoming.filter { !it.autoPost }

        val incomeLimit = set.monthlyIncomeLimitPesewas
        val expenseLimit = set.monthlyExpenseLimitPesewas
        val remainingBudget = Math.max(0L, expenseLimit - spentTotal)
        val spentFraction = if (expenseLimit > 0) (spentTotal.toFloat() / expenseLimit.toFloat()) else 0f
        val isOver = spentTotal > expenseLimit

        // 6-Month Income & Spending Trend for Recharts line chart
        val trendPoints = listOf(
            MonthTrendPoint("May", "May 2026", 420000L, 315000L),
            MonthTrendPoint("Jun", "June 2026", 450000L, 330000L),
            MonthTrendPoint("Jul", "July 2026", 420000L, 295000L),
            MonthTrendPoint("Aug", "August 2026", 480000L, 360000L),
            MonthTrendPoint("Sep", "September 2026", 450000L, 345000L),
            MonthTrendPoint(
                "Oct",
                "October 2026",
                maxOf(incomeTotal, incomeLimit),
                spentTotal
            )
        )

        return BemoDashboardState(
            unassignedPesewas = unassigned,
            totalIncomePesewas = incomeTotal,
            totalSpentPesewas = spentTotal,
            totalSavedPesewas = savedTotal,
            totalAssignedPesewas = totalAssigned,
            safeToSpendTodayPesewas = safeToSpendToday,
            daysLeftInMonth = daysLeft,
            netBalancePesewas = netBalance,
            availableBalancePesewas = availableBalance,
            topCategories = topCats,
            upcomingBills = upcoming,
            needsConfirmationBills = toConfirm,
            monthlyIncomeLimitPesewas = incomeLimit,
            monthlyExpenseLimitPesewas = expenseLimit,
            remainingBudgetPesewas = remainingBudget,
            budgetSpentFraction = spentFraction,
            isOverBudget = isOver,
            monthlyTrends = trendPoints
        )
    }

    fun updateBudgetLimits(incomeLimitPesewas: Long, expenseLimitPesewas: Long) {
        repository.updateBudgetLimits(incomeLimitPesewas, expenseLimitPesewas)
        _snackbarMessage.value = "Monthly budget limits updated"
    }

    fun selectTab(tab: BemoTab) {
        _currentTab.value = tab
        _currentSubScreen.value = SubScreen.MAIN
    }

    fun navigateTo(subScreen: SubScreen) {
        _currentSubScreen.value = subScreen
    }

    fun openAddSheet(type: TransactionType = TransactionType.EXPENSE) {
        _addSheetInitialType.value = type
        _showAddSheet.value = true
    }

    fun closeAddSheet() {
        _showAddSheet.value = false
    }

    fun saveTransaction(
        type: TransactionType,
        amountPesewas: Long,
        accountId: String,
        toAccountId: String? = null,
        categoryId: String? = null,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        // Duplicate check (within 2 minutes with same amount + account + category)
        val now = System.currentTimeMillis()
        val recentMatch = transactions.value.firstOrNull {
            !it.isDeleted &&
            it.amountPesewas == amountPesewas &&
            it.accountId == accountId &&
            it.categoryId == categoryId &&
            (now - it.dateMillis) < 120_000L
        }

        val txn = Transaction(
            type = type,
            amountPesewas = amountPesewas,
            accountId = accountId,
            toAccountId = toAccountId,
            categoryId = categoryId,
            note = note,
            dateMillis = dateMillis
        )

        if (recentMatch != null && duplicateWarningTxn.value == null) {
            duplicateWarningTxn.value = txn
            _snackbarMessage.value = "Similar transaction logged recently. Tap save again to confirm."
            return
        }

        duplicateWarningTxn.value = null
        repository.addTransaction(txn)
        _showAddSheet.value = false
        _snackbarMessage.value = "Transaction saved"
    }

    fun deleteTransaction(txnId: String) {
        val deleted = repository.deleteTransaction(txnId)
        if (deleted != null) {
            _canUndoDelete.value = true
            _snackbarMessage.value = "Transaction deleted"
        }
    }

    fun undoDelete() {
        if (repository.undoDeleteTransaction()) {
            _canUndoDelete.value = false
            _snackbarMessage.value = "Transaction restored"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun toggleHideAmounts() {
        repository.toggleHideAmounts()
    }

    fun updateTheme(mode: ThemeMode) {
        repository.updateSettings(settings.value.copy(themeMode = mode))
    }

    fun setPin(pin: String) {
        repository.updateSettings(settings.value.copy(pinEnabled = true, pinHash = pin))
        _snackbarMessage.value = "App PIN enabled"
    }

    fun removePin() {
        repository.updateSettings(settings.value.copy(pinEnabled = false, pinHash = ""))
        _snackbarMessage.value = "App PIN removed"
    }

    fun unlockApp(pin: String): Boolean {
        return if (pin == settings.value.pinHash) {
            _currentSubScreen.value = SubScreen.MAIN
            true
        } else {
            false
        }
    }

    fun finishOnboarding() {
        repository.updateSettings(settings.value.copy(hasCompletedOnboarding = true))
        _currentSubScreen.value = SubScreen.MAIN
    }

    fun resetDemo() {
        repository.resetDemoData()
        _snackbarMessage.value = "Demo data refreshed"
    }

    fun markAlertRead(id: String) {
        repository.markAlertRead(id)
    }

    fun markAllAlertsRead() {
        repository.markAllAlertsRead()
    }

    fun clearAllAlerts() {
        repository.clearAlerts()
        _snackbarMessage.value = "Alerts cleared"
    }

    fun sendTestAlert(threshold: AlertThreshold) {
        repository.sendTestAlert(threshold)
        _snackbarMessage.value = "Test ${if (threshold == AlertThreshold.LIMIT_100) "100%" else "80%"} notification triggered"
    }

    fun updateNotificationSettings(enabled: Boolean, at80: Boolean, at100: Boolean) {
        repository.updateSettings(
            settings.value.copy(
                budgetNotificationsEnabled = enabled,
                notifyAt80Percent = at80,
                notifyAt100Percent = at100
            )
        )
        _snackbarMessage.value = "Notification settings saved"
    }
}
