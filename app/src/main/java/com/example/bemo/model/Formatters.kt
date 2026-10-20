package com.example.bemo.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val currencyFormat = DecimalFormat("#,##0.00", symbols)
    private val compactFormat = DecimalFormat("#,##0", symbols)

    fun formatPesewas(pesewas: Long, hideAmounts: Boolean = false, showSign: Boolean = false): String {
        if (hideAmounts) return "••••"
        val isNegative = pesewas < 0
        val absPesewas = Math.abs(pesewas)
        val cedis = absPesewas / 100.0
        val formatted = currencyFormat.format(cedis)
        return when {
            isNegative -> "−₵$formatted"
            showSign && pesewas > 0 -> "+₵$formatted"
            else -> "₵$formatted"
        }
    }

    fun formatPesewasShort(pesewas: Long, hideAmounts: Boolean = false): String {
        if (hideAmounts) return "••••"
        val absPesewas = Math.abs(pesewas)
        val cedis = absPesewas / 100.0
        val formatted = if (cedis % 1.0 == 0.0) compactFormat.format(cedis) else currencyFormat.format(cedis)
        return if (pesewas < 0) "−₵$formatted" else "₵$formatted"
    }

    fun formatDate(millis: Long): String {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = millis
        val dateYear = calendar.get(Calendar.YEAR)
        val dateDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        return when {
            currentYear == dateYear && currentDayOfYear == dateDayOfYear -> "Today"
            currentYear == dateYear && currentDayOfYear - dateDayOfYear == 1 -> "Yesterday"
            currentYear == dateYear && dateDayOfYear - currentDayOfYear == 1 -> "Tomorrow"
            currentYear == dateYear -> SimpleDateFormat("d MMM", Locale.US).format(Date(millis))
            else -> SimpleDateFormat("d MMM yyyy", Locale.US).format(Date(millis))
        }
    }

    fun formatTime(millis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        return SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(millis))
    }

    /**
     * Evaluates simple arithmetic input like "12+8.50" or "25*2" into integer pesewas.
     * Returns null if invalid.
     */
    fun evaluateExpressionToPesewas(expr: String): Long? {
        val clean = expr.trim().replace("×", "*").replace("÷", "/")
        if (clean.isEmpty()) return 0L
        return try {
            val tokens = ArrayList<String>()
            var currentNum = StringBuilder()
            for (ch in clean) {
                if (ch in "+-*/") {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum.toString())
                        currentNum = StringBuilder()
                    }
                    tokens.add(ch.toString())
                } else if (ch.isDigit() || ch == '.') {
                    currentNum.append(ch)
                }
            }
            if (currentNum.isNotEmpty()) {
                tokens.add(currentNum.toString())
            }

            if (tokens.isEmpty()) return 0L
            if (tokens.size == 1) {
                val d = tokens[0].toDoubleOrNull() ?: return null
                return Math.round(d * 100)
            }

            // Simple left-to-right evaluation
            var result = tokens[0].toDoubleOrNull() ?: return null
            var i = 1
            while (i < tokens.size - 1) {
                val op = tokens[i]
                val nextVal = tokens[i + 1].toDoubleOrNull() ?: return null
                when (op) {
                    "+" -> result += nextVal
                    "-" -> result -= nextVal
                    "*" -> result *= nextVal
                    "/" -> if (nextVal != 0.0) result /= nextVal
                }
                i += 2
            }
            Math.round(result * 100)
        } catch (e: Exception) {
            null
        }
    }
}
