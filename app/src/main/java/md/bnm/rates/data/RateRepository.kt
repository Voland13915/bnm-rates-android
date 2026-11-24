package md.bnm.rates.data

import md.bnm.rates.core.BnmXmlParser
import md.bnm.rates.core.BusinessDays
import md.bnm.rates.core.Converter
import md.bnm.rates.core.Currency
import md.bnm.rates.core.MalformedResponseException
import md.bnm.rates.core.RatePoint
import md.bnm.rates.core.RateSet
import java.io.IOException
import java.math.RoundingMode
import java.time.LocalDate

/** Курсов нет ни на сайте, ни в сохранённых данных. Текст — для пользователя. */
class RatesUnavailableException(message: String) : Exception(message)

/**
 * Курсы для конвертера.
 * @param offlineReason null — курсы на нужную дату; иначе сайт недоступен и это последние сохранённые курсы
 */
data class LoadedRates(val rates: RateSet, val offlineReason: String?)

/**
 * Курс пары по дням для экранов «Неделя» и «Месяц».
 * @param warning почему часть дней не загружена (нет сети, сбой сайта) или null, если загружено всё
 * @param missingDays сколько рабочих дней периода не удалось получить
 */
data class History(
    val points: List<RatePoint>,
    val currencies: List<Currency>,
    val warning: String?,
    val missingDays: Int
)

/**
 * Единственная точка доступа к курсам. Каждая дата сначала ищется в кэше, затем запрашивается у НБМ,
 * и непустой ответ сохраняется. Обычные методы без корутин: вызывать из фонового потока.
 */
class RateRepository(
    private val source: RateSource,
    private val cache: RateCache,
    private val parser: BnmXmlParser = BnmXmlParser()
) {

    /** Курсы на последний рабочий день не позже [today]; без сети — последние сохранённые. */
    fun latest(today: LocalDate): LoadedRates {
        var date = BusinessDays.latest(today)
        repeat(LOOKBACK_DAYS) {
            val rates = try {
                load(date)
            } catch (e: SourceException) {
                return fromCache(today, e.message)
            } catch (e: MalformedResponseException) {
                return fromCache(today, e.message)
            }
            if (rates != null) return LoadedRates(rates, offlineReason = null)
            // Пустой ответ: на эту дату курс не установлен (праздник) — берём предыдущий рабочий день
            date = BusinessDays.previous(date)
        }
        throw RatesUnavailableException("НБМ не опубликовал курсы за последние $LOOKBACK_DAYS рабочих дней")
    }

    /**
     * Курс [from] → [to] по рабочим дням за последние [days] календарных дней.
     * После первой сетевой ошибки остальные дни берутся только из кэша: без сети иначе
     * пришлось бы ждать таймаут на каждый день периода.
     */
    fun history(from: String, to: String, days: Int, today: LocalDate): History {
        val loaded = ArrayList<Pair<LocalDate, RateSet>>()
        var warning: String? = null
        var networkFailed = false
        var missing = 0

        for (date in BusinessDays.inLastDays(today, days)) {
            val cached = cached(date)
            if (cached != null) {
                loaded.add(date to cached)
                continue
            }
            if (networkFailed) {
                missing++
                continue
            }
            try {
                val rates = fetchAndSave(date)
                if (rates != null) loaded.add(date to rates)
            } catch (e: SourceException) {
                networkFailed = true
                warning = e.message
                missing++
            } catch (e: MalformedResponseException) {
                if (warning == null) warning = e.message
                missing++
            }
        }

        if (loaded.isEmpty() && networkFailed) {
            throw RatesUnavailableException("$warning. Сохранённых курсов за этот период нет.")
        }

        val points = loaded.mapNotNull { (date, rates) ->
            val fromRate = rates.perUnit(from)
            val toRate = rates.perUnit(to)
            if (fromRate == null || toRate == null) null
            else RatePoint(date, fromRate.divide(toRate, Converter.RATE_SCALE, RoundingMode.HALF_UP))
        }
        val currencies = loaded.lastOrNull()?.second?.currencies() ?: emptyList()
        return History(points, currencies, warning, missing)
    }

    /** Курсы на дату или null, если НБМ вернул пустой ответ. */
    private fun load(date: LocalDate): RateSet? = cached(date) ?: fetchAndSave(date)

    private fun fetchAndSave(date: LocalDate): RateSet? {
        val xml = source.fetch(date)
        val rates = parser.parse(xml)
        if (rates.isEmpty) return null
        try {
            cache.put(date, xml)
        } catch (e: IOException) {
            // Не удалось сохранить — курсы всё равно показываем, в следующий раз запросим снова
        }
        return rates
    }

    /** Повреждённый файл кэша считается отсутствующим: дата будет запрошена у НБМ заново. */
    private fun cached(date: LocalDate): RateSet? {
        val xml = cache.get(date) ?: return null
        return try {
            parser.parse(xml).takeIf { !it.isEmpty }
        } catch (e: MalformedResponseException) {
            null
        }
    }

    private fun fromCache(today: LocalDate, reason: String?): LoadedRates {
        var date = cache.latestOnOrBefore(today)
        while (date != null) {
            val rates = cached(date)
            if (rates != null) return LoadedRates(rates, reason ?: "Сайт НБМ недоступен")
            date = cache.latestOnOrBefore(date.minusDays(1))
        }
        throw RatesUnavailableException("$reason. Сохранённых курсов нет — подключитесь к интернету и повторите.")
    }

    companion object {
        const val LOOKBACK_DAYS = 10
    }
}
