package md.bnm.rates.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BusinessDaysTest {

    private val friday = LocalDate.of(2026, 11, 20)
    private val saturday = LocalDate.of(2026, 11, 21)
    private val sunday = LocalDate.of(2026, 11, 22)
    private val monday = LocalDate.of(2026, 11, 23)
    private val tuesday = LocalDate.of(2026, 11, 24)

    @Test
    fun weekendMapsToFriday() {
        assertEquals(friday, BusinessDays.latest(saturday))
        assertEquals(friday, BusinessDays.latest(sunday))
        assertEquals(monday, BusinessDays.latest(monday))
    }

    @Test
    fun previousSkipsWeekend() {
        assertEquals(friday, BusinessDays.previous(monday))
        assertEquals(monday, BusinessDays.previous(tuesday))
    }

    @Test
    fun weekHasFiveBusinessDays() {
        assertEquals(
            listOf(LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 19), friday, monday, tuesday),
            BusinessDays.inLastDays(tuesday, 7)
        )
    }

    @Test
    fun monthIsThirtyCalendarDays() {
        val days = BusinessDays.inLastDays(tuesday, 30)

        assertEquals(22, days.size)
        assertEquals(LocalDate.of(2026, 10, 26), days.first())
        assertEquals(tuesday, days.last())
    }
}
