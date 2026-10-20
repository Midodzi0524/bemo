package com.example.bemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bemo.model.Formatters
import com.example.bemo.ui.BemoViewModel
import com.example.bemo.ui.components.BemoBentoCard
import com.example.ui.theme.LocalBemoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsScreen(
    viewModel: BemoViewModel,
    onBack: () -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val extended = LocalBemoColors.current

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring & Bills", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("recurring_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Needs confirmation section
            if (dashboardState.needsConfirmationBills.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Needs Confirmation (${dashboardState.needsConfirmationBills.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(dashboardState.needsConfirmationBills, key = { "confirm_${it.id}" }) { rule ->
                    val cat = catMap[rule.categoryId]
                    val acc = accMap[rule.accountId]

                    BemoBentoCard(
                        backgroundColor = extended.primaryTint,
                        borderColor = extended.primaryTint
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat?.emoji ?: "📅", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = rule.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.onPrimaryTint
                                    )
                                    Text(
                                        text = "Due ${Formatters.formatDate(rule.nextDueDateMillis)} · ${acc?.name ?: "Account"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.onPrimaryTint.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Text(
                                text = Formatters.formatPesewas(rule.amountPesewas, settings.hideAmounts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = extended.onPrimaryTint
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.repository.skipRecurringBill(rule.id) },
                                modifier = Modifier.testTag("skip_bill_${rule.id}")
                            ) {
                                Text("Skip", color = extended.textMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.repository.confirmRecurringBill(rule.id) },
                                modifier = Modifier.testTag("confirm_bill_${rule.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Upcoming Bills (Next 30 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(dashboardState.upcomingBills, key = { "up_${it.id}" }) { rule ->
                val cat = catMap[rule.categoryId]
                val acc = accMap[rule.accountId]

                BemoBentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(extended.surfaceAlt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat?.emoji ?: "💡", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = rule.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${Formatters.formatDate(rule.nextDueDateMillis)} · ${acc?.name ?: "Account"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatPesewas(rule.amountPesewas, settings.hideAmounts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (rule.autoPost) "Auto-post" else "Confirm first",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rule.autoPost) extended.successText else extended.onWarning
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
