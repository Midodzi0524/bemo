package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bemo.model.Formatters
import com.example.bemo.model.WishlistKind
import com.example.bemo.model.WishlistItem
import com.example.bemo.model.WishlistPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bemo", appName)
    }

    @Test
    fun `format pesewas correctly`() {
        // 1250 cedis = 125000 pesewas
        assertEquals("₵1,250.00", Formatters.formatPesewas(125000L))
        assertEquals("−₵25.00", Formatters.formatPesewas(-2500L))
        assertEquals("••••", Formatters.formatPesewas(125000L, hideAmounts = true))
    }

    @Test
    fun `evaluate calculator expression`() {
        val pesewas = Formatters.evaluateExpressionToPesewas("12+8.50")
        assertEquals(2050L, pesewas)
    }

    @Test
    fun `wishlist calculations and progress tracking`() {
        val item = WishlistItem(
            title = "Laptop Upgrade",
            targetAmountPesewas = 800000L, // ₵8,000.00
            savedAmountPesewas = 400000L,  // ₵4,000.00
            kind = WishlistKind.WANT,
            priority = WishlistPriority.HIGH
        )

        assertEquals(0.5f, item.progressFraction, 0.001f)
        assertEquals(50, item.progressPercent)
        assertEquals(400000L, item.remainingPesewas)
        assertFalse(item.isFunded)
        assertEquals("Halfway mark! 🌟", item.milestoneText)

        // Fully funded item
        val fundedItem = item.copy(savedAmountPesewas = 800000L)
        assertEquals(1.0f, fundedItem.progressFraction, 0.001f)
        assertEquals(100, fundedItem.progressPercent)
        assertEquals(0L, fundedItem.remainingPesewas)
        assertTrue(fundedItem.isFunded)
        assertEquals("100% Goal Reached! 🎉", fundedItem.milestoneText)
    }

    @Test
    fun `manual transaction creation and balance tracking`() {
        val expenseTxn = com.example.bemo.model.Transaction(
            type = com.example.bemo.model.TransactionType.EXPENSE,
            amountPesewas = 2500L, // ₵25.00
            accountId = "acc_cash",
            categoryId = "cat_transport",
            note = "Bolt ride",
            dateMillis = System.currentTimeMillis()
        )
        assertEquals(com.example.bemo.model.TransactionType.EXPENSE, expenseTxn.type)
        assertEquals(2500L, expenseTxn.amountPesewas)
        assertEquals("cat_transport", expenseTxn.categoryId)
        assertEquals("Bolt ride", expenseTxn.note)

        val incomeTxn = com.example.bemo.model.Transaction(
            type = com.example.bemo.model.TransactionType.INCOME,
            amountPesewas = 450000L, // ₵4,500.00
            accountId = "acc_bank",
            categoryId = "cat_salary",
            note = "October Salary"
        )
        assertEquals(com.example.bemo.model.TransactionType.INCOME, incomeTxn.type)
        assertEquals("October Salary", incomeTxn.note)
    }

    @Test
    fun `budget overview limits and 6-month trend data points`() {
        val incomeLimit = 450000L
        val expenseLimit = 350000L
        val spent = 285000L
        val remaining = expenseLimit - spent

        assertEquals(65000L, remaining)
        val spentFraction = spent.toFloat() / expenseLimit.toFloat()
        assertTrue(spentFraction < 1.0f)

        val trend = com.example.bemo.ui.components.MonthTrendPoint(
            monthLabel = "Oct",
            fullMonthName = "October 2026",
            incomePesewas = incomeLimit,
            spentPesewas = spent
        )
        assertEquals("Oct", trend.monthLabel)
        assertEquals(450000L, trend.incomePesewas)
        assertEquals(285000L, trend.spentPesewas)
    }
}
