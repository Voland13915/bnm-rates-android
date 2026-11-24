package md.bnm.rates.core

import java.math.BigDecimal

/** Проверка суммы, которую ввёл пользователь. */
sealed class AmountInput {

    data class Valid(val amount: BigDecimal) : AmountInput()

    data class Invalid(val message: String) : AmountInput()

    companion object {
        val MAX: BigDecimal = BigDecimal("1000000000")

        /** Целая часть, необязательный знак минус, разделитель «.» или «,» и дробная часть. */
        private val NUMBER = Regex("""-?\d+([.,]\d+)?""")

        fun parse(text: String): AmountInput {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return Invalid("Введите сумму")
            if (!NUMBER.matches(trimmed)) return Invalid("Введите число, например 125.50")

            val amount = BigDecimal(trimmed.replace(',', '.'))
            return when {
                amount.signum() <= 0 -> Invalid("Сумма должна быть больше нуля")
                amount.scale() > 2 -> Invalid("Не больше двух знаков после запятой")
                amount > MAX -> Invalid("Сумма не больше 1 000 000 000")
                else -> Valid(amount)
            }
        }
    }
}
