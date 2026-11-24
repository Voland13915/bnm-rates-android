package md.bnm.rates.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PeriodStatsTest {

    private fun point(day: Int, rate: String) = RatePoint(LocalDate.of(2026, 11, day), BigDecimal(rate))

    private fun stats(vararg points: RatePoint) = PeriodStats.of(points.toList())!!

    @Test
    fun emptySetHasNoStats() {
        assertNull(PeriodStats.of(emptyList()))
    }

    @Test
    fun singlePointHasNoChange() {
        val result = stats(point(20, "20.1267"))

        assertEquals(result.first, result.last)
        assertEquals(BigDecimal("20.1267"), result.min.rate)
        assertEquals(BigDecimal("20.1267"), result.max.rate)
        assertEquals(BigDecimal("20.126700"), result.average)
        assertEquals(BigDecimal("0.000000"), result.absoluteChange)
        assertEquals(BigDecimal("0.00"), result.percentChange)
        assertEquals(Trend.FLAT, result.trend)
    }

    @Test
    fun minMaxAverageAndRise() {
        // Порядок перепутан нарочно: статистика сортирует по дате сама
        val result = stats(point(19, "20.40"), point(16, "20.00"), point(18, "19.80"), point(17, "20.50"))

        assertEquals(point(16, "20.00"), result.first)
        assertEquals(point(19, "20.40"), result.last)
        assertEquals(point(18, "19.80"), result.min)
        assertEquals(point(17, "20.50"), result.max)
        assertEquals(BigDecimal("20.175000"), result.average)
        assertEquals(BigDecimal("0.400000"), result.absoluteChange)
        assertEquals(BigDecimal("2.00"), result.percentChange)
        assertEquals(Trend.UP, result.trend)
    }

    @Test
    fun fall() {
        val result = stats(point(16, "20.10"), point(17, "19.95"))

        assertEquals(BigDecimal("-0.150000"), result.absoluteChange)
        assertEquals(BigDecimal("-0.75"), result.percentChange)
        assertEquals(Trend.DOWN, result.trend)
    }

    @Test
    fun sameFirstAndLastIsFlatEvenIfRateMovedInBetween() {
        val result = stats(point(16, "20.00"), point(17, "21.00"), point(18, "20.00"))

        assertEquals(BigDecimal("0.000000"), result.absoluteChange)
        assertEquals(BigDecimal("0.00"), result.percentChange)
        assertEquals(Trend.FLAT, result.trend)
        assertEquals(BigDecimal("21.00"), result.max.rate)
    }

    @Test
    fun changeBelowDisplayedPercentIsFlat() {
        // +0.000002 — это 0.0002 %, на экране «+0.00 %»: тенденция должна совпадать с тем, что видно
        val result = stats(point(16, "1.099399"), point(17, "1.099401"))

        assertEquals(BigDecimal("0.000002"), result.absoluteChange)
        assertEquals(BigDecimal("0.00"), result.percentChange)
        assertEquals(Trend.FLAT, result.trend)
    }

    @Test
    fun smallRatePairKeepsPrecision() {
        // Кросс-курс JPY → USD около 0.0063: с 4 знаками изменение потерялось бы
        val result = stats(point(16, "0.006250"), point(17, "0.006300"))

        assertEquals(BigDecimal("0.000050"), result.absoluteChange)
        assertEquals(BigDecimal("0.80"), result.percentChange)
        assertEquals(Trend.UP, result.trend)
    }

    @Test
    fun tieKeepsEarliestDate() {
        val result = stats(point(16, "20.00"), point(17, "19.00"), point(18, "19.00"), point(19, "20.00"))

        assertEquals(point(17, "19.00"), result.min)
        assertEquals(point(16, "20.00"), result.max)
    }
}
