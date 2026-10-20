package com.example.bemo.data

import android.content.Context
import android.util.Log
import com.example.bemo.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.bemo.notification.NotificationHelper
import java.util.Calendar

class BemoRepository(
    private val context: Context,
    val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(com.example.R.string.firestore_database_id)
    )
) {
    private val auth: FirebaseAuth = Firebase.auth

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _budgetAllocations = MutableStateFlow<Map<String, Long>>(emptyMap())
    val budgetAllocations: StateFlow<Map<String, Long>> = _budgetAllocations.asStateFlow()

    private val _wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistItem>> = _wishlistItems.asStateFlow()

    private val _recurringRules = MutableStateFlow<List<RecurringRule>>(emptyList())
    val recurringRules: StateFlow<List<RecurringRule>> = _recurringRules.asStateFlow()

    private val _quickAddTemplates = MutableStateFlow<List<QuickAddTemplate>>(emptyList())
    val quickAddTemplates: StateFlow<List<QuickAddTemplate>> = _quickAddTemplates.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _alerts = MutableStateFlow<List<BudgetAlert>>(emptyList())
    val alerts: StateFlow<List<BudgetAlert>> = _alerts.asStateFlow()
    private val notifiedThresholds = mutableSetOf<String>()

    // Soft delete undo stack
    private var lastDeletedTransaction: Transaction? = null
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private var activeSyncUserId: String? = null

    init {
        // Quick add templates default in-memory
        val accCashId = "acc_cash"
        val accMomoId = "acc_momo"
        val catTransportId = "cat_transport"
        val catFoodId = "cat_food"
        val catDataId = "cat_data"
        _quickAddTemplates.value = listOf(
            QuickAddTemplate(id = "tpl_trotro", label = "Trotro fare ₵15", amountPesewas = 1500L, categoryId = catTransportId, accountId = accCashId),
            QuickAddTemplate(id = "tpl_bolt", label = "Bolt ride ₵25", amountPesewas = 2500L, categoryId = catTransportId, accountId = accMomoId),
            QuickAddTemplate(id = "tpl_lunch", label = "Lunch ₵30", amountPesewas = 3000L, categoryId = catFoodId, accountId = accCashId),
            QuickAddTemplate(id = "tpl_airtime", label = "Airtime ₵20", amountPesewas = 2000L, categoryId = catDataId, accountId = accMomoId)
        )

        NotificationHelper.createNotificationChannel(context)

        _alerts.value = listOf(
            BudgetAlert(
                id = "alert_food_init",
                categoryId = "cat_food",
                categoryName = "Food & Groceries",
                categoryEmoji = "🛒",
                threshold = AlertThreshold.WARNING_80,
                spentPesewas = 62000L,
                limitPesewas = 70000L,
                title = "⚠️ Approaching Limit (80%): 🛒 Food & Groceries",
                message = "You have reached 88% of your Food & Groceries budget (₵620.00 of ₵700.00). ₵80.00 remaining."
            ),
            BudgetAlert(
                id = "alert_rent_init",
                categoryId = "cat_rent",
                categoryName = "Rent / Housing",
                categoryEmoji = "🏠",
                threshold = AlertThreshold.LIMIT_100,
                spentPesewas = 90000L,
                limitPesewas = 90000L,
                title = "🚨 Budget Limit Reached: 🏠 Rent / Housing",
                message = "You have spent 100% of your Rent / Housing budget (₵900.00 of ₵900.00)."
            )
        )

        // Check if user is already signed in on startup
        auth.currentUser?.let { user ->
            startSync(user.uid)
        }
    }

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun startSync(userId: String) {
        if (activeSyncUserId == userId) return
        stopSync()
        activeSyncUserId = userId

        val userDocRef = db.collection("users").document(userId)
        val userPath = "users/$userId"

        // Listen to User Profile & Settings
        val userSub = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, userPath)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data ?: return@addSnapshotListener
                val current = _settings.value
                _settings.value = current.copy(
                    userName = data["userName"] as? String ?: current.userName,
                    currencyCode = data["currencyCode"] as? String ?: current.currencyCode,
                    hideAmounts = data["hideAmounts"] as? Boolean ?: current.hideAmounts,
                    pinEnabled = data["pinEnabled"] as? Boolean ?: current.pinEnabled,
                    monthlyIncomeLimitPesewas = (data["monthlyIncomeLimitPesewas"] as? Number)?.toLong() ?: current.monthlyIncomeLimitPesewas,
                    monthlyExpenseLimitPesewas = (data["monthlyExpenseLimitPesewas"] as? Number)?.toLong() ?: current.monthlyExpenseLimitPesewas,
                    hasCompletedOnboarding = data["hasCompletedOnboarding"] as? Boolean ?: current.hasCompletedOnboarding
                )
            } else if (snapshot != null && !snapshot.exists()) {
                // Seed initial user data to Firestore
                seedFirestoreData(userId)
            }
        }
        listenerRegistrations.add(userSub)

        // Listen to Accounts
        val accountsPath = "$userPath/accounts"
        val accountsSub = userDocRef.collection("accounts").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, accountsPath)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Account(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        emoji = data["emoji"] as? String ?: "💳",
                        kind = try { AccountKind.valueOf(data["kind"] as? String ?: "OTHER") } catch (e: Exception) { AccountKind.OTHER },
                        openingBalancePesewas = (data["openingBalancePesewas"] as? Number)?.toLong() ?: 0L,
                        isDefault = data["isDefault"] as? Boolean ?: false,
                        isArchived = data["isArchived"] as? Boolean ?: false
                    )
                }
                if (list.isNotEmpty()) {
                    _accounts.value = list
                }
            }
        }
        listenerRegistrations.add(accountsSub)

        // Listen to Categories
        val categoriesPath = "$userPath/categories"
        val catSub = userDocRef.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, categoriesPath)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Category(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        emoji = data["emoji"] as? String ?: "🏷️",
                        kind = try { CategoryKind.valueOf(data["kind"] as? String ?: "EXPENSE") } catch (e: Exception) { CategoryKind.EXPENSE },
                        bgTintHex = data["bgTintHex"] as? String ?: "#FAECE7",
                        textTintHex = data["textTintHex"] as? String ?: "#993C1D",
                        isStarter = data["isStarter"] as? Boolean ?: false
                    )
                }
                if (list.isNotEmpty()) {
                    _categories.value = list
                }
            }
        }
        listenerRegistrations.add(catSub)

        // Listen to Transactions
        val txPath = "$userPath/transactions"
        val txSub = userDocRef.collection("transactions").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, txPath)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Transaction(
                        id = doc.id,
                        type = try { TransactionType.valueOf(data["type"] as? String ?: "EXPENSE") } catch (e: Exception) { TransactionType.EXPENSE },
                        amountPesewas = (data["amountPesewas"] as? Number)?.toLong() ?: 0L,
                        accountId = data["accountId"] as? String ?: "",
                        toAccountId = data["toAccountId"] as? String,
                        categoryId = data["categoryId"] as? String,
                        dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        note = data["note"] as? String ?: "",
                        status = try { TransactionStatus.valueOf(data["status"] as? String ?: "POSTED") } catch (e: Exception) { TransactionStatus.POSTED },
                        isDeleted = data["isDeleted"] as? Boolean ?: false,
                        deletedAtMillis = (data["deletedAtMillis"] as? Number)?.toLong()
                    )
                }.sortedByDescending { it.dateMillis }
                _transactions.value = list
            }
        }
        listenerRegistrations.add(txSub)

        // Listen to Wishlist
        val wishlistPath = "$userPath/wishlist"
        val wishSub = userDocRef.collection("wishlist").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, wishlistPath)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    WishlistItem(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        targetAmountPesewas = (data["targetAmountPesewas"] as? Number)?.toLong() ?: 0L,
                        savedAmountPesewas = (data["savedAmountPesewas"] as? Number)?.toLong() ?: 0L,
                        emoji = data["emoji"] as? String ?: "🎯",
                        kind = try { WishlistKind.valueOf(data["kind"] as? String ?: "WANT") } catch (e: Exception) { WishlistKind.WANT },
                        priority = try { WishlistPriority.valueOf(data["priority"] as? String ?: "MEDIUM") } catch (e: Exception) { WishlistPriority.MEDIUM },
                        categoryTag = data["categoryTag"] as? String ?: "General",
                        targetDateMillis = (data["targetDateMillis"] as? Number)?.toLong(),
                        isPurchased = data["isPurchased"] as? Boolean ?: false,
                        notes = data["notes"] as? String ?: ""
                    )
                }
                if (list.isNotEmpty()) {
                    _wishlistItems.value = list
                }
            }
        }
        listenerRegistrations.add(wishSub)

        // Listen to Recurring
        val recPath = "$userPath/recurring"
        val recSub = userDocRef.collection("recurring").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, recPath)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    RecurringRule(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        amountPesewas = (data["amountPesewas"] as? Number)?.toLong() ?: 0L,
                        type = try { TransactionType.valueOf(data["type"] as? String ?: "EXPENSE") } catch (e: Exception) { TransactionType.EXPENSE },
                        frequency = try { RecurringFrequency.valueOf(data["frequency"] as? String ?: "MONTHLY") } catch (e: Exception) { RecurringFrequency.MONTHLY },
                        nextDueDateMillis = (data["nextDueDateMillis"] as? Number)?.toLong() ?: 0L,
                        categoryId = data["categoryId"] as? String ?: "",
                        accountId = data["accountId"] as? String ?: "",
                        autoPost = data["autoPost"] as? Boolean ?: false,
                        isSubscription = data["isSubscription"] as? Boolean ?: false,
                        isActive = data["isActive"] as? Boolean ?: true
                    )
                }
                if (list.isNotEmpty()) {
                    _recurringRules.value = list
                }
            }
        }
        listenerRegistrations.add(recSub)

        // Listen to Budget Allocations
        val allocPath = "$userPath/budgetAllocations"
        val allocSub = userDocRef.collection("budgetAllocations").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, allocPath)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val map = mutableMapOf<String, Long>()
                snapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val pesewas = (data["assignedPesewas"] as? Number)?.toLong() ?: 0L
                    map[doc.id] = pesewas
                }
                if (map.isNotEmpty()) {
                    _budgetAllocations.value = map
                }
            }
        }
        listenerRegistrations.add(allocSub)
    }

    fun stopSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
        activeSyncUserId = null
    }

    private fun seedFirestoreData(userId: String) {
        val now = System.currentTimeMillis()

        // 1. User Profile Document
        val userPath = "users/$userId"
        val userProfile = hashMapOf(
            "userId" to userId,
            "userName" to (auth.currentUser?.displayName ?: "Maurice"),
            "currencyCode" to "GHS",
            "hideAmounts" to false,
            "pinEnabled" to false,
            "monthlyIncomeLimitPesewas" to 450000L,
            "monthlyExpenseLimitPesewas" to 350000L,
            "hasCompletedOnboarding" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(userId).set(userProfile)
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, userPath) }

        // 2. Default Accounts
        val accs = listOf(
            Account(id = "acc_cash", name = "Cash", emoji = "💵", kind = AccountKind.CASH, openingBalancePesewas = 22000L, isDefault = true),
            Account(id = "acc_momo", name = "Mobile Money", emoji = "📲", kind = AccountKind.MOMO, openingBalancePesewas = 90000L),
            Account(id = "acc_bank", name = "Bank", emoji = "🏦", kind = AccountKind.BANK, openingBalancePesewas = 200000L),
            Account(id = "acc_savings", name = "Savings", emoji = "💰", kind = AccountKind.SAVINGS, openingBalancePesewas = 40000L)
        )
        for (acc in accs) {
            val path = "$userPath/accounts/${acc.id}"
            val data = hashMapOf(
                "id" to acc.id,
                "userId" to userId,
                "name" to acc.name,
                "emoji" to acc.emoji,
                "kind" to acc.kind.name,
                "openingBalancePesewas" to acc.openingBalancePesewas,
                "isDefault" to acc.isDefault,
                "isArchived" to acc.isArchived,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).collection("accounts").document(acc.id)
                .set(data)
                .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
        }

        // 3. Default Categories
        val cats = listOf(
            Category(id = "cat_rent", name = "Rent / Housing", emoji = "🏠", bgTintHex = "#FAECE7", textTintHex = "#993C1D", isStarter = true),
            Category(id = "cat_food", name = "Food & Groceries", emoji = "🛒", bgTintHex = "#FAEEDA", textTintHex = "#854F0B", isStarter = true),
            Category(id = "cat_transport", name = "Transport", emoji = "🚌", bgTintHex = "#E8F4FD", textTintHex = "#1565C0", isStarter = true),
            Category(id = "cat_utils", name = "Utilities (ECG)", emoji = "💡", bgTintHex = "#FEF7E0", textTintHex = "#B78103", isStarter = true),
            Category(id = "cat_data", name = "Data & Airtime", emoji = "📱", bgTintHex = "#F3E8FD", textTintHex = "#6A1B9A", isStarter = true),
            Category(id = "cat_health", name = "Health & Personal", emoji = "💊", bgTintHex = "#FCE8E6", textTintHex = "#C62828", isStarter = true),
            Category(id = "cat_dining", name = "Dining & Drinks", emoji = "🍽️", bgTintHex = "#FFF0E6", textTintHex = "#D85A30", isStarter = true),
            Category(id = "cat_family", name = "Family Support", emoji = "🤝", bgTintHex = "#FDE8F3", textTintHex = "#AD1457", isStarter = true),
            Category(id = "cat_salary", name = "Salary / Wages", emoji = "💼", kind = CategoryKind.INCOME, bgTintHex = "#E8F8F0", textTintHex = "#1F7A4D", isStarter = true),
            Category(id = "cat_sidegig", name = "Business / Gig", emoji = "📈", kind = CategoryKind.INCOME, bgTintHex = "#E8F8F0", textTintHex = "#1F7A4D", isStarter = true)
        )
        for (cat in cats) {
            val path = "$userPath/categories/${cat.id}"
            val data = hashMapOf(
                "id" to cat.id,
                "userId" to userId,
                "name" to cat.name,
                "emoji" to cat.emoji,
                "kind" to cat.kind.name,
                "bgTintHex" to cat.bgTintHex,
                "textTintHex" to cat.textTintHex,
                "isStarter" to cat.isStarter,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).collection("categories").document(cat.id)
                .set(data)
                .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
        }

        // 4. Budget Allocations
        val allocations = mapOf(
            "cat_rent" to 90000L,
            "cat_food" to 70000L,
            "cat_transport" to 30000L,
            "cat_utils" to 15000L,
            "cat_data" to 8000L,
            "cat_health" to 12000L,
            "cat_dining" to 20000L,
            "cat_family" to 30000L
        )
        allocations.forEach { (catId, pesewas) ->
            val path = "$userPath/budgetAllocations/$catId"
            val data = hashMapOf(
                "categoryId" to catId,
                "userId" to userId,
                "assignedPesewas" to pesewas,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).collection("budgetAllocations").document(catId)
                .set(data)
                .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
        }

        // 5. Initial Transactions
        val seedTxns = listOf(
            Transaction(id = "txn_1", type = TransactionType.INCOME, amountPesewas = 450000L, accountId = "acc_bank", categoryId = "cat_salary", dateMillis = now - (15L * 86400000L), note = "Monthly salary credit"),
            Transaction(id = "txn_2", type = TransactionType.EXPENSE, amountPesewas = 90000L, accountId = "acc_bank", categoryId = "cat_rent", dateMillis = now - (14L * 86400000L), note = "October rent payment"),
            Transaction(id = "txn_3", type = TransactionType.EXPENSE, amountPesewas = 62000L, accountId = "acc_momo", categoryId = "cat_food", dateMillis = now - (2L * 86400000L), note = "Weekly grocery stock"),
            Transaction(id = "txn_4", type = TransactionType.EXPENSE, amountPesewas = 31000L, accountId = "acc_cash", categoryId = "cat_transport", dateMillis = now - (1L * 86400000L), note = "Transport & trotro fares"),
            Transaction(id = "txn_5", type = TransactionType.EXPENSE, amountPesewas = 12000L, accountId = "acc_momo", categoryId = "cat_food", dateMillis = now - 14400000L, note = "Market fresh vegetables"),
            Transaction(id = "txn_6", type = TransactionType.EXPENSE, amountPesewas = 2500L, accountId = "acc_momo", categoryId = "cat_transport", dateMillis = now - 1800000L, note = "Bolt ride to office"),
            Transaction(id = "txn_7", type = TransactionType.SAVING, amountPesewas = 40000L, accountId = "acc_bank", categoryId = null, dateMillis = now - (5L * 86400000L), note = "Earmarked for Laptop goal"),
            Transaction(id = "txn_8", type = TransactionType.EXPENSE, amountPesewas = 15000L, accountId = "acc_momo", categoryId = "cat_utils", dateMillis = now + (5L * 86400000L), note = "ECG Power token", status = TransactionStatus.PLANNED)
        )
        for (txn in seedTxns) {
            val path = "$userPath/transactions/${txn.id}"
            val data = hashMapOf<String, Any>(
                "id" to txn.id,
                "userId" to userId,
                "type" to txn.type.name,
                "amountPesewas" to txn.amountPesewas,
                "accountId" to txn.accountId,
                "dateMillis" to txn.dateMillis,
                "note" to txn.note,
                "status" to txn.status.name,
                "isDeleted" to txn.isDeleted,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (txn.categoryId != null) data["categoryId"] = txn.categoryId
            db.collection("users").document(userId).collection("transactions").document(txn.id)
                .set(data)
                .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
        }

        // 6. Wishlist Items
        val wishlist = listOf(
            WishlistItem(id = "wsh_emergency", title = "Emergency fund", targetAmountPesewas = 600000L, savedAmountPesewas = 120000L, emoji = "🛟", kind = WishlistKind.GOAL, priority = WishlistPriority.HIGH, notes = "Target 3-6 months living expenses"),
            WishlistItem(id = "wsh_laptop", title = "Laptop upgrade", targetAmountPesewas = 800000L, savedAmountPesewas = 496000L, emoji = "💻", kind = WishlistKind.WANT, priority = WishlistPriority.HIGH, notes = "Work computer replacement"),
            WishlistItem(id = "wsh_rent", title = "Annual rent deposit", targetAmountPesewas = 1200000L, savedAmountPesewas = 300000L, emoji = "🏠", kind = WishlistKind.GOAL, priority = WishlistPriority.MEDIUM, notes = "Due in mid 2027")
        )
        for (item in wishlist) {
            val path = "$userPath/wishlist/${item.id}"
            val data = hashMapOf(
                "id" to item.id,
                "userId" to userId,
                "title" to item.title,
                "targetAmountPesewas" to item.targetAmountPesewas,
                "savedAmountPesewas" to item.savedAmountPesewas,
                "emoji" to item.emoji,
                "kind" to item.kind.name,
                "priority" to item.priority.name,
                "categoryTag" to item.categoryTag,
                "isPurchased" to item.isPurchased,
                "notes" to item.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).collection("wishlist").document(item.id)
                .set(data)
                .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
        }

        // 7. Recurring Rules
        val rules = listOf(
            RecurringRule(id = "rec_ecg", title = "ECG Electricity", amountPesewas = 15000L, frequency = RecurringFrequency.MONTHLY, nextDueDateMillis = now + (5L * 86400000L), categoryId = "cat_utils", accountId = "acc_momo", autoPost = true),
            RecurringRule(id = "rec_rent", title = "Apartment Rent", amountPesewas = 90000L, frequency = RecurringFrequency.MONTHLY, nextDueDateMillis = now + (12L * 86400000L), categoryId = "cat_rent", accountId = "acc_bank", autoPost = true),
            RecurringRule(id = "rec_data", title = "Monthly Data Bundle", amountPesewas = 4000L, frequency = RecurringFrequency.MONTHLY, nextDueDateMillis = now + (14L * 86400000L), categoryId = "cat_data", accountId = "acc_momo", autoPost = false),
            RecurringRule(id = "rec_salary", title = "Monthly Salary", amountPesewas = 450000L, type = TransactionType.INCOME, frequency = RecurringFrequency.MONTHLY, nextDueDateMillis = now + (2L * 86400000L), categoryId = "cat_salary", accountId = "acc_bank", autoPost = false)
        )
        for (r in rules) {
            val path = "$userPath/recurring/${r.id}"
            val data = hashMapOf(
                "id" to r.id,
                "userId" to userId,
                "title" to r.title,
                "amountPesewas" to r.amountPesewas,
                "type" to r.type.name,
                "frequency" to r.frequency.name,
                "nextDueDateMillis" to r.nextDueDateMillis,
                "categoryId" to r.categoryId,
                "accountId" to r.accountId,
                "autoPost" to r.autoPost,
                "isSubscription" to r.isSubscription,
                "isActive" to r.isActive,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(userId).collection("recurring").document(r.id)
                .set(data)
                .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
        }
    }

    // --- MUTATOR METHODS ---

    fun addTransaction(transaction: Transaction) {
        val uid = requireUserId()
        val path = "users/$uid/transactions/${transaction.id}"
        val data = hashMapOf<String, Any>(
            "id" to transaction.id,
            "userId" to uid,
            "type" to transaction.type.name,
            "amountPesewas" to transaction.amountPesewas,
            "accountId" to transaction.accountId,
            "dateMillis" to transaction.dateMillis,
            "note" to transaction.note,
            "status" to transaction.status.name,
            "isDeleted" to transaction.isDeleted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.toAccountId != null) data["toAccountId"] = transaction.toAccountId
        if (transaction.categoryId != null) data["categoryId"] = transaction.categoryId
        if (transaction.deletedAtMillis != null) data["deletedAtMillis"] = transaction.deletedAtMillis

        db.collection("users").document(uid).collection("transactions").document(transaction.id)
            .set(data)
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
    }

    fun updateTransaction(transaction: Transaction) {
        val uid = requireUserId()
        val path = "users/$uid/transactions/${transaction.id}"
        val data = hashMapOf<String, Any>(
            "id" to transaction.id,
            "userId" to uid,
            "type" to transaction.type.name,
            "amountPesewas" to transaction.amountPesewas,
            "accountId" to transaction.accountId,
            "dateMillis" to transaction.dateMillis,
            "note" to transaction.note,
            "status" to transaction.status.name,
            "isDeleted" to transaction.isDeleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.toAccountId != null) data["toAccountId"] = transaction.toAccountId
        if (transaction.categoryId != null) data["categoryId"] = transaction.categoryId
        if (transaction.deletedAtMillis != null) data["deletedAtMillis"] = transaction.deletedAtMillis

        db.collection("users").document(uid).collection("transactions").document(transaction.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.UPDATE, path) }
    }

    fun deleteTransaction(id: String, soft: Boolean = true): Transaction? {
        val uid = try { requireUserId() } catch (e: Exception) { null }
        val existing = _transactions.value.find { it.id == id } ?: return null
        lastDeletedTransaction = existing
        if (soft) {
            val updated = existing.copy(isDeleted = true, deletedAtMillis = System.currentTimeMillis())
            updateTransaction(updated)
        } else {
            if (uid != null) {
                val path = "users/$uid/transactions/$id"
                db.collection("users").document(uid).collection("transactions").document(id)
                    .delete()
                    .addOnFailureListener { e -> handleFirestoreError(e, OperationType.DELETE, path) }
            }
        }
        return existing
    }

    fun undoDelete(): Boolean {
        val toRestore = lastDeletedTransaction ?: return false
        val restored = toRestore.copy(isDeleted = false, deletedAtMillis = null)
        updateTransaction(restored)
        lastDeletedTransaction = null
        return true
    }

    fun undoDeleteTransaction(): Boolean = undoDelete()

    fun addAccount(account: Account) {
        val uid = requireUserId()
        val path = "users/$uid/accounts/${account.id}"
        val data = hashMapOf(
            "id" to account.id,
            "userId" to uid,
            "name" to account.name,
            "emoji" to account.emoji,
            "kind" to account.kind.name,
            "openingBalancePesewas" to account.openingBalancePesewas,
            "isDefault" to account.isDefault,
            "isArchived" to account.isArchived,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("accounts").document(account.id)
            .set(data)
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
    }

    fun updateAccount(account: Account) {
        val uid = requireUserId()
        val path = "users/$uid/accounts/${account.id}"
        val data = hashMapOf(
            "id" to account.id,
            "userId" to uid,
            "name" to account.name,
            "emoji" to account.emoji,
            "kind" to account.kind.name,
            "openingBalancePesewas" to account.openingBalancePesewas,
            "isDefault" to account.isDefault,
            "isArchived" to account.isArchived,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("accounts").document(account.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.UPDATE, path) }
    }

    fun deleteAccount(id: String) {
        val uid = requireUserId()
        val path = "users/$uid/accounts/$id"
        db.collection("users").document(uid).collection("accounts").document(id)
            .delete()
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.DELETE, path) }
    }

    fun addCategory(category: Category) {
        val uid = requireUserId()
        val path = "users/$uid/categories/${category.id}"
        val data = hashMapOf(
            "id" to category.id,
            "userId" to uid,
            "name" to category.name,
            "emoji" to category.emoji,
            "kind" to category.kind.name,
            "bgTintHex" to category.bgTintHex,
            "textTintHex" to category.textTintHex,
            "isStarter" to category.isStarter,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("categories").document(category.id)
            .set(data)
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
    }

    fun updateCategory(category: Category) {
        val uid = requireUserId()
        val path = "users/$uid/categories/${category.id}"
        val data = hashMapOf(
            "id" to category.id,
            "userId" to uid,
            "name" to category.name,
            "emoji" to category.emoji,
            "kind" to category.kind.name,
            "bgTintHex" to category.bgTintHex,
            "textTintHex" to category.textTintHex,
            "isStarter" to category.isStarter,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("categories").document(category.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.UPDATE, path) }
    }

    fun deleteCategory(id: String) {
        val uid = requireUserId()
        val path = "users/$uid/categories/$id"
        db.collection("users").document(uid).collection("categories").document(id)
            .delete()
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.DELETE, path) }
    }

    fun setBudgetAllocation(categoryId: String, pesewas: Long) {
        val uid = requireUserId()
        val path = "users/$uid/budgetAllocations/$categoryId"
        val data = hashMapOf(
            "categoryId" to categoryId,
            "userId" to uid,
            "assignedPesewas" to pesewas,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("budgetAllocations").document(categoryId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.WRITE, path) }
    }

    fun addWishlistItem(item: WishlistItem) {
        val uid = requireUserId()
        val path = "users/$uid/wishlist/${item.id}"
        val data = hashMapOf<String, Any>(
            "id" to item.id,
            "userId" to uid,
            "title" to item.title,
            "targetAmountPesewas" to item.targetAmountPesewas,
            "savedAmountPesewas" to item.savedAmountPesewas,
            "emoji" to item.emoji,
            "kind" to item.kind.name,
            "priority" to item.priority.name,
            "categoryTag" to item.categoryTag,
            "isPurchased" to item.isPurchased,
            "notes" to item.notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (item.targetDateMillis != null) data["targetDateMillis"] = item.targetDateMillis

        db.collection("users").document(uid).collection("wishlist").document(item.id)
            .set(data)
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
    }

    fun updateWishlistItem(item: WishlistItem) {
        val uid = requireUserId()
        val path = "users/$uid/wishlist/${item.id}"
        val data = hashMapOf<String, Any>(
            "id" to item.id,
            "userId" to uid,
            "title" to item.title,
            "targetAmountPesewas" to item.targetAmountPesewas,
            "savedAmountPesewas" to item.savedAmountPesewas,
            "emoji" to item.emoji,
            "kind" to item.kind.name,
            "priority" to item.priority.name,
            "categoryTag" to item.categoryTag,
            "isPurchased" to item.isPurchased,
            "notes" to item.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (item.targetDateMillis != null) data["targetDateMillis"] = item.targetDateMillis

        db.collection("users").document(uid).collection("wishlist").document(item.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.UPDATE, path) }
    }

    fun addWishlistContribution(itemId: String, contribution: WishlistContribution) {
        val current = _wishlistItems.value.find { it.id == itemId } ?: return
        val updated = current.copy(
            savedAmountPesewas = current.savedAmountPesewas + contribution.amountPesewas,
            contributions = current.contributions + contribution
        )
        updateWishlistItem(updated)
    }

    fun deleteWishlistItem(id: String) {
        val uid = requireUserId()
        val path = "users/$uid/wishlist/$id"
        db.collection("users").document(uid).collection("wishlist").document(id)
            .delete()
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.DELETE, path) }
    }

    fun addRecurringRule(rule: RecurringRule) {
        val uid = requireUserId()
        val path = "users/$uid/recurring/${rule.id}"
        val data = hashMapOf(
            "id" to rule.id,
            "userId" to uid,
            "title" to rule.title,
            "amountPesewas" to rule.amountPesewas,
            "type" to rule.type.name,
            "frequency" to rule.frequency.name,
            "nextDueDateMillis" to rule.nextDueDateMillis,
            "categoryId" to rule.categoryId,
            "accountId" to rule.accountId,
            "autoPost" to rule.autoPost,
            "isSubscription" to rule.isSubscription,
            "isActive" to rule.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("recurring").document(rule.id)
            .set(data)
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.CREATE, path) }
    }

    fun updateRecurringRule(rule: RecurringRule) {
        val uid = requireUserId()
        val path = "users/$uid/recurring/${rule.id}"
        val data = hashMapOf(
            "id" to rule.id,
            "userId" to uid,
            "title" to rule.title,
            "amountPesewas" to rule.amountPesewas,
            "type" to rule.type.name,
            "frequency" to rule.frequency.name,
            "nextDueDateMillis" to rule.nextDueDateMillis,
            "categoryId" to rule.categoryId,
            "accountId" to rule.accountId,
            "autoPost" to rule.autoPost,
            "isSubscription" to rule.isSubscription,
            "isActive" to rule.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("recurring").document(rule.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.UPDATE, path) }
    }

    fun deleteRecurringRule(id: String) {
        val uid = requireUserId()
        val path = "users/$uid/recurring/$id"
        db.collection("users").document(uid).collection("recurring").document(id)
            .delete()
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.DELETE, path) }
    }

    fun postRecurringNow(ruleId: String) {
        val rule = _recurringRules.value.find { it.id == ruleId } ?: return
        val txn = Transaction(
            type = rule.type,
            amountPesewas = rule.amountPesewas,
            accountId = rule.accountId,
            categoryId = rule.categoryId,
            note = "${rule.title} (recurring auto-post)",
            dateMillis = System.currentTimeMillis(),
            status = TransactionStatus.POSTED
        )
        addTransaction(txn)

        // Advance next due date
        val cal = Calendar.getInstance()
        cal.timeInMillis = rule.nextDueDateMillis
        when (rule.frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.BIWEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        updateRecurringRule(rule.copy(nextDueDateMillis = cal.timeInMillis))
    }

    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        val uid = auth.currentUser?.uid ?: return
        val path = "users/$uid"
        val data = hashMapOf(
            "userId" to uid,
            "userName" to settings.userName,
            "currencyCode" to settings.currencyCode,
            "hideAmounts" to settings.hideAmounts,
            "pinEnabled" to settings.pinEnabled,
            "monthlyIncomeLimitPesewas" to settings.monthlyIncomeLimitPesewas,
            "monthlyExpenseLimitPesewas" to settings.monthlyExpenseLimitPesewas,
            "hasCompletedOnboarding" to settings.hasCompletedOnboarding,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> handleFirestoreError(e, OperationType.WRITE, path) }
    }

    fun updateBudgetLimits(incomeLimitPesewas: Long, expenseLimitPesewas: Long) {
        updateSettings(_settings.value.copy(
            monthlyIncomeLimitPesewas = incomeLimitPesewas,
            monthlyExpenseLimitPesewas = expenseLimitPesewas
        ))
    }

    fun toggleHideAmounts() {
        updateSettings(_settings.value.copy(hideAmounts = !_settings.value.hideAmounts))
    }

    fun resetDemoData() {
        val uid = try { requireUserId() } catch (e: Exception) { null }
        if (uid != null) {
            seedFirestoreData(uid)
        }
    }

    fun transferBetweenAccounts(fromAccountId: String, toAccountId: String, amountPesewas: Long, note: String = "Transfer") {
        val txn = Transaction(
            type = TransactionType.TRANSFER,
            amountPesewas = amountPesewas,
            accountId = fromAccountId,
            toAccountId = toAccountId,
            note = note,
            dateMillis = System.currentTimeMillis()
        )
        addTransaction(txn)
    }

    fun reconcileAccount(accountId: String, actualBalancePesewas: Long) {
        val account = _accounts.value.find { it.id == accountId } ?: return
        val txns = _transactions.value.filter { !it.isDeleted && (it.accountId == accountId || it.toAccountId == accountId) }
        var calculated = account.openingBalancePesewas
        for (t in txns) {
            when (t.type) {
                TransactionType.INCOME -> if (t.accountId == accountId) calculated += t.amountPesewas
                TransactionType.EXPENSE -> if (t.accountId == accountId) calculated -= t.amountPesewas
                TransactionType.SAVING -> if (t.accountId == accountId) calculated -= t.amountPesewas
                TransactionType.TRANSFER -> {
                    if (t.accountId == accountId) calculated -= t.amountPesewas
                    if (t.toAccountId == accountId) calculated += t.amountPesewas
                }
            }
        }
        val diff = actualBalancePesewas - calculated
        if (diff != 0L) {
            val type = if (diff > 0) TransactionType.INCOME else TransactionType.EXPENSE
            val txn = Transaction(
                type = type,
                amountPesewas = Math.abs(diff),
                accountId = accountId,
                note = "Reconciliation adjustment",
                dateMillis = System.currentTimeMillis()
            )
            addTransaction(txn)
        }
    }

    fun updateCategoryAllocation(categoryId: String, pesewas: Long) {
        setBudgetAllocation(categoryId, pesewas)
    }

    fun moveBudgetAllocation(fromCatId: String, toCatId: String, pesewas: Long) {
        val currentAlloc = _budgetAllocations.value
        val fromVal = currentAlloc[fromCatId] ?: 0L
        val toVal = currentAlloc[toCatId] ?: 0L
        val moveAmt = Math.min(fromVal, pesewas)
        setBudgetAllocation(fromCatId, Math.max(0L, fromVal - moveAmt))
        setBudgetAllocation(toCatId, toVal + moveAmt)
    }

    fun skipRecurringBill(ruleId: String) {
        val rule = _recurringRules.value.find { it.id == ruleId } ?: return
        val cal = Calendar.getInstance()
        cal.timeInMillis = rule.nextDueDateMillis
        when (rule.frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.BIWEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        updateRecurringRule(rule.copy(nextDueDateMillis = cal.timeInMillis))
    }

    fun confirmRecurringBill(ruleId: String) {
        postRecurringNow(ruleId)
    }

    fun contributeToWishlist(itemId: String, amountPesewas: Long, accountId: String, note: String = "") {
        val contribution = WishlistContribution(
            amountPesewas = amountPesewas,
            accountId = accountId,
            note = note,
            dateMillis = System.currentTimeMillis()
        )
        addWishlistContribution(itemId, contribution)

        val item = _wishlistItems.value.find { it.id == itemId }
        val title = item?.title ?: "Goal"
        val txn = Transaction(
            type = TransactionType.SAVING,
            amountPesewas = amountPesewas,
            accountId = accountId,
            note = "Contribution to $title",
            dateMillis = System.currentTimeMillis()
        )
        addTransaction(txn)
    }

    fun checkBudgetThresholds() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val monthPeriod = "${year}_$month"

        val cats = _categories.value
        val txns = _transactions.value.filter {
            !it.isDeleted && it.type == TransactionType.EXPENSE && isSameMonth(it.dateMillis, year, month)
        }
        val allocs = _budgetAllocations.value
        val currentSettings = _settings.value

        if (!currentSettings.budgetNotificationsEnabled) return

        val newAlerts = mutableListOf<BudgetAlert>()

        // 1. Evaluate Category Limits
        for (cat in cats) {
            val limit = allocs[cat.id] ?: 0L
            if (limit <= 0L) continue

            val spent = txns.filter { it.categoryId == cat.id }.sumOf { it.amountPesewas }
            val ratio = spent.toDouble() / limit.toDouble()

            if (ratio >= 1.0 && currentSettings.notifyAt100Percent) {
                val key = "${cat.id}_LIMIT_100_$monthPeriod"
                if (!notifiedThresholds.contains(key)) {
                    notifiedThresholds.add(key)
                    val alert = BudgetAlert(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryEmoji = cat.emoji,
                        threshold = AlertThreshold.LIMIT_100,
                        spentPesewas = spent,
                        limitPesewas = limit,
                        title = "🚨 Budget Limit Reached: ${cat.emoji} ${cat.name}",
                        message = "You have spent 100% of your ${cat.name} budget (${Formatters.formatPesewas(spent)} of ${Formatters.formatPesewas(limit)})."
                    )
                    newAlerts.add(alert)
                    NotificationHelper.postBudgetNotification(context, alert)
                }
            } else if (ratio >= 0.8 && currentSettings.notifyAt80Percent) {
                val key = "${cat.id}_WARNING_80_$monthPeriod"
                if (!notifiedThresholds.contains(key)) {
                    notifiedThresholds.add(key)
                    val alert = BudgetAlert(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        categoryEmoji = cat.emoji,
                        threshold = AlertThreshold.WARNING_80,
                        spentPesewas = spent,
                        limitPesewas = limit,
                        title = "⚠️ Approaching Limit (80%): ${cat.emoji} ${cat.name}",
                        message = "You have reached 80% of your ${cat.name} budget (${Formatters.formatPesewas(spent)} of ${Formatters.formatPesewas(limit)}). ${Formatters.formatPesewas(limit - spent)} left."
                    )
                    newAlerts.add(alert)
                    NotificationHelper.postBudgetNotification(context, alert)
                }
            }
        }

        // 2. Evaluate Total Monthly Expense Limit
        val totalSpent = txns.sumOf { it.amountPesewas }
        val overallLimit = currentSettings.monthlyExpenseLimitPesewas
        if (overallLimit > 0L) {
            val overallRatio = totalSpent.toDouble() / overallLimit.toDouble()
            if (overallRatio >= 1.0 && currentSettings.notifyAt100Percent) {
                val key = "overall_LIMIT_100_$monthPeriod"
                if (!notifiedThresholds.contains(key)) {
                    notifiedThresholds.add(key)
                    val alert = BudgetAlert(
                        categoryId = null,
                        categoryName = "Overall Monthly Budget",
                        categoryEmoji = "📊",
                        threshold = AlertThreshold.LIMIT_100,
                        spentPesewas = totalSpent,
                        limitPesewas = overallLimit,
                        title = "🚨 Monthly Expense Ceiling Reached",
                        message = "Your total monthly spending has hit 100% (${Formatters.formatPesewas(totalSpent)} of ${Formatters.formatPesewas(overallLimit)})."
                    )
                    newAlerts.add(alert)
                    NotificationHelper.postBudgetNotification(context, alert)
                }
            } else if (overallRatio >= 0.8 && currentSettings.notifyAt80Percent) {
                val key = "overall_WARNING_80_$monthPeriod"
                if (!notifiedThresholds.contains(key)) {
                    notifiedThresholds.add(key)
                    val alert = BudgetAlert(
                        categoryId = null,
                        categoryName = "Overall Monthly Budget",
                        categoryEmoji = "📊",
                        threshold = AlertThreshold.WARNING_80,
                        spentPesewas = totalSpent,
                        limitPesewas = overallLimit,
                        title = "⚠️ Monthly Budget at 80%",
                        message = "Your total monthly spending has reached 80% (${Formatters.formatPesewas(totalSpent)} of ${Formatters.formatPesewas(overallLimit)})."
                    )
                    newAlerts.add(alert)
                    NotificationHelper.postBudgetNotification(context, alert)
                }
            }
        }

        if (newAlerts.isNotEmpty()) {
            _alerts.value = (newAlerts + _alerts.value).distinctBy { it.id }
        }
    }

    private fun isSameMonth(dateMillis: Long, year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }

    fun markAlertRead(id: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllAlertsRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
    }

    fun clearAlerts() {
        _alerts.value = emptyList()
    }

    fun sendTestAlert(threshold: AlertThreshold) {
        val cat = _categories.value.firstOrNull { it.kind == CategoryKind.EXPENSE }
            ?: Category(id = "cat_sample", name = "Food & Groceries", emoji = "🛒", kind = CategoryKind.EXPENSE)
        val limit = 50000L // ₵500.00
        val spent = if (threshold == AlertThreshold.LIMIT_100) 50000L else 40000L

        val alert = if (threshold == AlertThreshold.LIMIT_100) {
            BudgetAlert(
                categoryId = cat.id,
                categoryName = cat.name,
                categoryEmoji = cat.emoji,
                threshold = AlertThreshold.LIMIT_100,
                spentPesewas = spent,
                limitPesewas = limit,
                title = "🚨 Budget Limit Reached: ${cat.emoji} ${cat.name}",
                message = "You have spent 100% of your ${cat.name} budget (${Formatters.formatPesewas(spent)} of ${Formatters.formatPesewas(limit)})."
            )
        } else {
            BudgetAlert(
                categoryId = cat.id,
                categoryName = cat.name,
                categoryEmoji = cat.emoji,
                threshold = AlertThreshold.WARNING_80,
                spentPesewas = spent,
                limitPesewas = limit,
                title = "⚠️ Approaching Limit (80%): ${cat.emoji} ${cat.name}",
                message = "You have reached 80% of your ${cat.name} budget (${Formatters.formatPesewas(spent)} of ${Formatters.formatPesewas(limit)}). ${Formatters.formatPesewas(limit - spent)} left."
            )
        }
        _alerts.value = listOf(alert) + _alerts.value
        NotificationHelper.postBudgetNotification(context, alert)
    }
}
