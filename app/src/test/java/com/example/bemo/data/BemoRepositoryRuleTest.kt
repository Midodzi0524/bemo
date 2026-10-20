package com.example.bemo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.bemo.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BemoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun addTransaction_authenticatedUser_writesToFirestore(): Unit = runBlocking {
        val uid = signInTestUser("alice@example.com")
        val repository = BemoRepository(context, firestore)

        val txnId = "txn_${UUID.randomUUID().toString().replace("-", "")}"
        val transaction = Transaction(
            id = txnId,
            type = TransactionType.EXPENSE,
            amountPesewas = 4500L,
            accountId = "acc_cash",
            note = "Groceries",
            dateMillis = System.currentTimeMillis()
        )

        withTimeout(5000L) {
            repository.addTransaction(transaction)
            val snapshot = firestore.collection("users").document(uid)
                .collection("transactions").document(txnId).get().await()
            assertTrue(snapshot.exists())
            assertEquals(4500L, snapshot.getLong("amountPesewas"))
            assertEquals(uid, snapshot.getString("userId"))
        }
    }

    @Test
    fun unauthenticatedUser_throwsIllegalStateException() {
        auth.signOut()
        val repository = BemoRepository(context, firestore)

        val txn = Transaction(
            id = "txn_unauth",
            type = TransactionType.EXPENSE,
            amountPesewas = 1000L,
            accountId = "acc_cash"
        )

        assertThrows(IllegalStateException::class.java) {
            repository.addTransaction(txn)
        }
    }

    @Test
    fun addAccount_authenticatedUser_writesAccount(): Unit = runBlocking {
        val uid = signInTestUser("bob@example.com")
        val repository = BemoRepository(context, firestore)

        val accId = "acc_${UUID.randomUUID().toString().replace("-", "")}"
        val account = Account(
            id = accId,
            name = "MoMo Wallet",
            emoji = "📲",
            kind = AccountKind.MOMO,
            openingBalancePesewas = 50000L
        )

        withTimeout(5000L) {
            repository.addAccount(account)
            val snapshot = firestore.collection("users").document(uid)
                .collection("accounts").document(accId).get().await()
            assertTrue(snapshot.exists())
            assertEquals("MoMo Wallet", snapshot.getString("name"))
            assertEquals("MOMO", snapshot.getString("kind"))
        }
    }

    @Test
    fun setBudgetAllocation_authenticatedUser_writesAllocation(): Unit = runBlocking {
        val uid = signInTestUser("charlie@example.com")
        val repository = BemoRepository(context, firestore)

        val catId = "cat_groceries"
        withTimeout(5000L) {
            repository.setBudgetAllocation(catId, 75000L)
            val snapshot = firestore.collection("users").document(uid)
                .collection("budgetAllocations").document(catId).get().await()
            assertTrue(snapshot.exists())
            assertEquals(75000L, snapshot.getLong("assignedPesewas"))
        }
    }
}
