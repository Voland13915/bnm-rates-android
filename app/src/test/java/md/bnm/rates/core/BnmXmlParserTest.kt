package md.bnm.rates.core

import md.bnm.rates.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BnmXmlParserTest {

    private val parser = BnmXmlParser()

    @Test
    fun parsesRealWeekdayResponse() {
        val rates = parser.parse(Fixtures.WEEKDAY)

        assertEquals(LocalDate.of(2026, 11, 20), rates.date)
        assertEquals(40, rates.rates.size)
        val eur = rates.rates.getValue("EUR")
        assertEquals("Euro", eur.name)
        assertEquals(1, eur.nominal)
        assertEquals(BigDecimal("20.1267"), eur.value)
    }

    @Test
    fun ratePerUnitDividesByNominal() {
        val rates = parser.parse(Fixtures.WEEKDAY)

        val jpy = rates.rates.getValue("JPY")
        assertEquals(100, jpy.nominal)
        assertEquals(0, BigDecimal("0.11448").compareTo(jpy.perUnit))
        assertEquals(0, BigDecimal("0.054553").compareTo(rates.perUnit("HUF")))
    }

    @Test
    fun moldovanLeuIsNotInResponseButHasRateOne() {
        val rates = parser.parse(Fixtures.WEEKDAY)

        assertNull(rates.rates["MDL"])
        assertEquals(BigDecimal.ONE, rates.perUnit("MDL"))
        assertEquals(listOf("MDL", "EUR", "USD"), rates.currencies().take(3).map { it.code })
        assertEquals(41, rates.currencies().size)
    }

    @Test
    fun weekendResponseRepeatsFridayRatesWithWeekendDate() {
        val friday = parser.parse(Fixtures.WEEKDAY)
        val sunday = parser.parse(Fixtures.WEEKEND)

        assertEquals(LocalDate.of(2026, 11, 22), sunday.date)
        assertEquals(friday.rates, sunday.rates)
    }

    @Test
    fun responseWithoutCurrenciesIsEmptyNotError() {
        val rates = parser.parse(Fixtures.empty(LocalDate.of(2026, 1, 1)))

        assertTrue(rates.isEmpty)
        assertEquals(LocalDate.of(2026, 1, 1), rates.date)
    }

    @Test
    fun truncatedXmlIsMalformed() {
        assertThrows(MalformedResponseException::class.java) {
            parser.parse("<ValCurs Date=\"20.11.2026\"><Valute><CharCode>EUR")
        }
    }

    @Test
    fun blankResponseIsMalformed() {
        assertThrows(MalformedResponseException::class.java) { parser.parse("   ") }
    }

    @Test
    fun htmlErrorPageIsMalformed() {
        assertThrows(MalformedResponseException::class.java) {
            parser.parse("<html><body>Service unavailable</body></html>")
        }
    }

    @Test
    fun missingValueIsMalformed() {
        val xml = Fixtures.xml(LocalDate.of(2026, 11, 20), mapOf("EUR" to "20.1267"))
            .replace("<Value>20.1267</Value>", "")

        val error = assertThrows(MalformedResponseException::class.java) { parser.parse(xml) }
        assertEquals("У валюты нет поля Value", error.message)
    }

    @Test
    fun nonPositiveRateIsMalformed() {
        val xml = Fixtures.xml(LocalDate.of(2026, 11, 20), mapOf("EUR" to "0"))

        assertThrows(MalformedResponseException::class.java) { parser.parse(xml) }
    }

    @Test
    fun badDateIsMalformed() {
        val xml = Fixtures.WEEKDAY.replace("Date=\"20.11.2026\"", "Date=\"2026-11-20\"")

        assertThrows(MalformedResponseException::class.java) { parser.parse(xml) }
    }

    @Test
    fun doctypeIsRejected() {
        val xml = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>" +
            "<ValCurs Date=\"20.11.2026\">&e;</ValCurs>"

        assertThrows(MalformedResponseException::class.java) { parser.parse(xml) }
    }
}
