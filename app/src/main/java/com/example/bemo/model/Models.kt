package com.example.bemo.model

import java.util.UUID

enum class AccountKind(val displayName: String, val defaultEmoji: String) {
    CASH("Cash", "💵"),
    MOMO("Mobile Money", "📲"),
    BANK("Bank", "🏦"),
    SAVINGS("Savings", "💰"),
    OTHER("Other", "💳")
}

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val emoji: String = "💳",
    val kind: AccountKind = AccountKind.OTHER,
    val openingBalancePesewas: Long = 0L,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

enum class CategoryKind {
    EXPENSE, INCOME
}

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val emoji: String = "🏷️",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val bgTintHex: String = "#FAECE7",
    val textTintHex: String = "#993C1D",
    val isStarter: Boolean = false
)

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    SAVING("Saving"),
    TRANSFER("Transfer")
}

enum class TransactionStatus {
    POSTED, PENDING, PLANNED
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType = TransactionType.EXPENSE,
    val amountPesewas: Long = 0L,
    val accountId: String = "",
    val toAccountId: String? = null, // for transfers
    val categoryId: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val status: TransactionStatus = TransactionStatus.POSTED,
    val isDeleted: Boolean = false,
    val deletedAtMillis: Long? = null
)

enum class WishlistKind {
    GOAL, WANT
}

enum class WishlistPriority {
    HIGH, MEDIUM, LOW
}

data class WishlistContribution(
    val id: String = UUID.randomUUID().toString(),
    val amountPesewas: Long = 0L,
    val dateMillis: Long = System.currentTimeMillis(),
    val accountId: String? = null,
    val note: String = ""
)

data class WishlistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val targetAmountPesewas: Long = 0L,
    val savedAmountPesewas: Long = 0L,
    val emoji: String = "🎯",
    val kind: WishlistKind = WishlistKind.WANT,
    val priority: WishlistPriority = WishlistPriority.MEDIUM,
    val categoryTag: String = "General",
    val targetDateMillis: Long? = null,
    val isPurchased: Boolean = false,
    val notes: String = "",
    val contributions: List<WishlistContribution> = emptyList()
) {
    val progressFraction: Float
        get() = if (targetAmountPesewas > 0) {
            (savedAmountPesewas.toFloat() / targetAmountPesewas.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val remainingPesewas: Long
        get() = Math.max(0L, targetAmountPesewas - savedAmountPesewas)

    val isFunded: Boolean
        get() = savedAmountPesewas >= targetAmountPesewas && targetAmountPesewas > 0

    val milestoneText: String
        get() = when {
            isFunded -> "100% Goal Reached! 🎉"
            progressPercent >= 75 -> "75% Almost there! 🚀"
            progressPercent >= 50 -> "Halfway mark! 🌟"
            progressPercent >= 25 -> "25% Quarter funded! 💪"
            else -> "$progressPercent% saved"
        }
}

enum class RecurringFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 weeks"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class RecurringRule(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amountPesewas: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val nextDueDateMillis: Long = 0L,
    val categoryId: String = "",
    val accountId: String = "",
    val autoPost: Boolean = false, // false = confirm first
    val isSubscription: Boolean = false,
    val isActive: Boolean = true
)

data class QuickAddTemplate(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val amountPesewas: Long = 0L,
    val categoryId: String = "",
    val accountId: String = "",
    val type: TransactionType = TransactionType.EXPENSE
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideAmounts: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val userName: String = "Maurice",
    val hasCompletedOnboarding: Boolean = true,
    val currencyCode: String = "GHS",
    val ageConfirmed: Boolean = true,
    val termsAccepted: Boolean = true,
    val monthlyIncomeLimitPesewas: Long = 450000L, // ₵4,500.00
    val monthlyExpenseLimitPesewas: Long = 350000L, // ₵3,500.00
    val budgetNotificationsEnabled: Boolean = true,
    val notifyAt80Percent: Boolean = true,
    val notifyAt100Percent: Boolean = true
)

enum class AlertThreshold {
    WARNING_80,
    LIMIT_100
}

data class BudgetAlert(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String? = null,
    val categoryName: String = "",
    val categoryEmoji: String = "⚠️",
    val threshold: AlertThreshold = AlertThreshold.WARNING_80,
    val spentPesewas: Long = 0L,
    val limitPesewas: Long = 0L,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val title: String = "",
    val message: String = ""
)
