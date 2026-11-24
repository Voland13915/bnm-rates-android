package md.bnm.rates.data

import md.bnm.rates.Fixtures
import md.bnm.rates.core.RateSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

class RateRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var cache: FileRateCache
    private val source = FakeSource()

    private val wednesday = LocalDate.of(2026, 11, 18)
    private val thursday = LocalDate.of(2026, 11, 19)
    private val friday = LocalDate.of(2026, 11, 20)
    private val sunday = LocalDate.of(2026, 11, 22)
    private val monday = LocalDate.of(2026, 11, 23)
    private val tuesday = LocalDate.of(2026, 11, 24)

    /** Сайт НБМ в памяти: ответы по датам, по умолчанию — пустой ответ; можно «выключить сеть». */
    private class FakeSource : RateSource {
        val responses = HashMap<LocalDate, String>()
        val calls = ArrayList<LocalDate>()
        var failure: SourceException? = null

        override fun fetch(date: LocalDate): String {
            calls.add(date)
            failure?.let { throw it }
            return responses[date] ?: Fixtures.empty(date)
        }
    }

    @Before
    fun setUp() {
        dir = folder.newFolder("rates")
        cache = FileRateCache(dir)
    }

    private fun repository() = RateRepository(source, cache)

    private fun eurDay(date: LocalDate, eur: String) {
        source.responses[date] = Fixtures.xml(date, mapOf("EUR" to eur, "USD" to "18.0000"))
    }

    private fun goOffline() {
        source.failure = SourceException(SourceException.Kind.NO_NETWORK, "Нет соединения с сайтом НБМ")
    }

    private fun eur(rates: RateSet) = rates.rates.getValue("EUR").value

    // --- latest: курсы для конвертера ---

    @Test
    fun latestFetchesAndCaches() {
        eurDay(tuesday, "20.1000")

        val loaded = repository().latest(tuesday)

        assertEquals(BigDecimal("20.1000"), eur(loaded.rates))
        assertNull(loaded.offlineReason)
        assertTrue(File(dir, "2026-11-24.xml").isFile)
    }

    @Test
    fun cachedDateIsNotRequestedAgain() {
        eurDay(tuesday, "20.1000")
        repository().latest(tuesday)
        source.calls.clear()

        repository().latest(tuesday)

        assertTrue(source.calls.isEmpty())
    }

    @Test
    fun weekendRequestsFriday() {
        source.responses[friday] = Fixtures.WEEKDAY

        val loaded = repository().latest(sunday)

        assertEquals(listOf(friday), source.calls)
        assertEquals(friday, loaded.rates.date)
    }

    @Test
    fun emptyResponseFallsBackToPreviousBusinessDay() {
        eurDay(friday, "20.1267")

        val loaded = repository().latest(monday)

        assertEquals(listOf(monday, friday), source.calls)
        assertEquals(friday, loaded.rates.date)
        assertNull(loaded.offlineReason)
    }

    @Test
    fun offlineUsesLatestCachedRatesAndSaysWhy() {
        eurDay(friday, "20.1267")
        repository().latest(friday)
        goOffline()

        val loaded = repository().latest(tuesday)

        assertEquals(friday, loaded.rates.date)
        assertEquals("Нет соединения с сайтом НБМ", loaded.offlineReason)
    }

    @Test
    fun offlineWithoutCacheIsUnavailable() {
        goOffline()

        val error = assertThrows(RatesUnavailableException::class.java) { repository().latest(tuesday) }
        assertTrue(error.message!!.startsWith("Нет соединения с сайтом НБМ."))
    }

    @Test
    fun malformedResponseFallsBackToCache() {
        eurDay(friday, "20.1267")
        repository().latest(friday)
        source.responses[tuesday] = "<html>maintenance</html>"

        val loaded = repository().latest(tuesday)

        assertEquals(friday, loaded.rates.date)
        assertEquals("В ответе НБМ нет списка курсов", loaded.offlineReason)
    }

    @Test
    fun corruptedCacheFileIsRequestedAgain() {
        File(dir, "2026-11-24.xml").writeText("<ValCurs Date=\"24.11")
        eurDay(tuesday, "20.1000")

        val loaded = repository().latest(tuesday)

        assertEquals(listOf(tuesday), source.calls)
        assertEquals(BigDecimal("20.1000"), eur(loaded.rates))
    }

    @Test
    fun nothingPublishedForTenBusinessDaysIsUnavailable() {
        assertThrows(RatesUnavailableException::class.java) { repository().latest(tuesday) }
        assertEquals(RateRepository.LOOKBACK_DAYS, source.calls.size)
    }

    // --- history: экраны «Неделя» и «Месяц» ---

    @Test
    fun weekHistoryHasOnePointPerBusinessDay() {
        eurDay(wednesday, "20.00")
        eurDay(thursday, "20.10")
        eurDay(friday, "20.20")
        eurDay(monday, "20.30")
        eurDay(tuesday, "20.40")

        val history = repository().history("EUR", "MDL", 7, tuesday)

        assertEquals(listOf(wednesday, thursday, friday, monday, tuesday), history.points.map { it.date })
        assertEquals(0, BigDecimal("20.40").compareTo(history.points.last().rate))
        assertNull(history.warning)
        assertEquals(0, history.missingDays)
        assertEquals("MDL", history.currencies.first().code)
    }

    @Test
    fun secondLoadComesFromCache() {
        eurDay(friday, "20.20")
        repository().history("EUR", "MDL", 7, tuesday)
        source.calls.clear()
        goOffline()

        val history = repository().history("EUR", "MDL", 7, tuesday)

        // Пятница из кэша; остальные дни НБМ вернул пустыми — их нет в кэше, и запрос идёт снова,
        // но после первой сетевой ошибки остальные дни уже не запрашиваются
        assertEquals(listOf(friday), history.points.map { it.date })
        assertEquals(1, source.calls.size)
    }

    @Test
    fun crossPairIsComputedThroughLeu() {
        source.responses[friday] = Fixtures.WEEKDAY

        val history = repository().history("EUR", "USD", 7, tuesday)

        assertEquals(BigDecimal("1.099399"), history.points.single().rate)
    }

    @Test
    fun dayWithoutRatesIsSkippedWithoutWarning() {
        eurDay(friday, "20.20")
        eurDay(tuesday, "20.40")

        val history = repository().history("EUR", "MDL", 7, tuesday)

        assertEquals(listOf(friday, tuesday), history.points.map { it.date })
        assertNull(history.warning)
    }

    @Test
    fun offlineHistoryShowsCachedDaysAndStopsAfterFirstFailure() {
        eurDay(wednesday, "20.00")
        eurDay(thursday, "20.10")
        repository().history("EUR", "MDL", 2, thursday)
        source.calls.clear()
        goOffline()

        val history = repository().history("EUR", "MDL", 7, tuesday)

        assertEquals(listOf(wednesday, thursday), history.points.map { it.date })
        assertEquals("Нет соединения с сайтом НБМ", history.warning)
        assertEquals(3, history.missingDays)
        assertEquals(listOf(friday), source.calls)
    }

    @Test
    fun offlineHistoryWithoutCacheIsUnavailable() {
        goOffline()

        assertThrows(RatesUnavailableException::class.java) {
            repository().history("EUR", "MDL", 30, tuesday)
        }
    }

    @Test
    fun emptyPeriodOnlineGivesEmptyHistory() {
        val history = repository().history("EUR", "MDL", 7, tuesday)

        assertTrue(history.points.isEmpty())
        assertNull(history.warning)
    }

    @Test
    fun currencyMissingOnSomeDayIsSkipped() {
        eurDay(friday, "20.20")
        source.responses[tuesday] = Fixtures.xml(tuesday, mapOf("USD" to "18.0000"))

        val history = repository().history("EUR", "MDL", 7, tuesday)

        assertEquals(listOf(friday), history.points.map { it.date })
    }

    @Test
    fun malformedDayIsSkippedWithWarningButOthersLoad() {
        eurDay(friday, "20.20")
        source.responses[monday] = "<ValCurs"
        eurDay(tuesday, "20.40")

        val history = repository().history("EUR", "MDL", 7, tuesday)

        assertEquals(listOf(friday, tuesday), history.points.map { it.date })
        assertEquals("Сайт НБМ вернул некорректные данные", history.warning)
        assertEquals(1, history.missingDays)
    }
}
