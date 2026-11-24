package md.bnm.rates.core

import md.bnm.rates.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ConverterTest {

    private val rates = BnmXmlParser().parse(Fixtures.WEEKDAY)

    private fun convert(amount: String, from: String, to: String) =
        Converter.convert(BigDecimal(amount), from, to, rates)

    @Test
    fun foreignToLeu() {
        assertEquals(BigDecimal("2012.67"), convert("100", "EUR", "MDL"))
    }

    @Test
    fun leuToForeign() {
        assertEquals(BigDecimal("100.00"), convert("2012.67", "MDL", "EUR"))
    }

    @Test
    fun crossCurrencyGoesThroughLeu() {
        // 100 × 20.1267 ÷ 18.3070
        assertEquals(BigDecimal("109.94"), convert("100", "EUR", "USD"))
        assertEquals(BigDecimal("1.099399"), Converter.crossRate(rates, "EUR", "USD"))
    }

    @Test
    fun nominalIsTakenIntoAccount() {
        // JPY: 11.4480 лея за 100 иен
        assertEquals(BigDecimal("114.48"), convert("1000", "JPY", "MDL"))
    }

    @Test
    fun sameCurrencyReturnsAmount() {
        assertEquals(BigDecimal("100.00"), convert("100", "EUR", "EUR"))
        assertEquals(BigDecimal("55.50"), convert("55.5", "MDL", "MDL"))
    }

    @Test
    fun reverseDirectionReturnsOriginalWithinRounding() {
        for (amount in listOf("1", "99.99", "12345.67")) {
            val there = convert(amount, "EUR", "USD")
            val back = Converter.convert(there, "USD", "EUR", rates)
            val difference = back.subtract(BigDecimal(amount)).abs()
            assertTrue("$amount -> $there -> $back", difference <= BigDecimal("0.01"))
        }
    }

    @Test
    fun unknownCurrencyIsReported() {
        val error = assertThrows(UnknownCurrencyException::class.java) { convert("1", "XYZ", "MDL") }
        assertEquals("XYZ", error.code)
    }
}
