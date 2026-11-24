package md.bnm.rates.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class AmountInputTest {

    private fun valid(text: String) = AmountInput.parse(text) as AmountInput.Valid

    private fun errorFor(text: String) = (AmountInput.parse(text) as AmountInput.Invalid).message

    @Test
    fun acceptsWholeNumber() {
        assertEquals(BigDecimal("100"), valid("100").amount)
    }

    @Test
    fun acceptsCommaAndPeriod() {
        assertEquals(BigDecimal("12.5"), valid("12,5").amount)
        assertEquals(BigDecimal("12.50"), valid("12.50").amount)
    }

    @Test
    fun trimsSpaces() {
        assertEquals(BigDecimal("7.25"), valid("  7.25 ").amount)
    }

    @Test
    fun acceptsBoundaries() {
        assertEquals(BigDecimal("0.01"), valid("0.01").amount)
        assertEquals(AmountInput.MAX, valid("1000000000").amount)
    }

    @Test
    fun rejectsEmpty() {
        assertEquals("Введите сумму", errorFor(""))
        assertEquals("Введите сумму", errorFor("   "))
    }

    @Test
    fun rejectsLettersAndBadFormat() {
        for (text in listOf("abc", "12a", "1,2,3", "1.2.3", "+5", "1 000", ".5", "5.")) {
            assertEquals(text, "Введите число, например 125.50", errorFor(text))
        }
    }

    @Test
    fun rejectsZeroAndNegative() {
        for (text in listOf("0", "0.00", "-5", "-0.01")) {
            assertEquals(text, "Сумма должна быть больше нуля", errorFor(text))
        }
    }

    @Test
    fun rejectsMoreThanTwoDecimals() {
        assertEquals("Не больше двух знаков после запятой", errorFor("1.234"))
        assertEquals("Не больше двух знаков после запятой", errorFor("0,001"))
    }

    @Test
    fun rejectsAboveMaximum() {
        assertEquals("Сумма не больше 1 000 000 000", errorFor("1000000000.01"))
        assertEquals("Сумма не больше 1 000 000 000", errorFor("5000000000"))
    }
}
