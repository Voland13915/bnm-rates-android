package md.bnm.rates.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Курс одной валюты из ответа НБМ: [value] леев за [nominal] единиц. */
data class CurrencyRate(
    val code: String,
    val name: String,
    val nominal: Int,
    val value: BigDecimal
) {
    /** Сколько леев стоит одна единица валюты (у JPY, HUF и др. номинал 100). */
    val perUnit: BigDecimal
        get() = value.divide(BigDecimal(nominal), SCALE, RoundingMode.HALF_UP)

    companion object {
        const val SCALE = 10
    }
}

data class Currency(val code: String, val name: String)

/** Все курсы НБМ на одну дату. Лея в ответе НБМ нет: это базовая валюта с курсом 1. */
data class RateSet(val date: LocalDate, val rates: Map<String, CurrencyRate>) {

    val isEmpty: Boolean
        get() = rates.isEmpty()

    /** Курс одной единицы валюты в леях; для неизвестной валюты — null. */
    fun perUnit(code: String): BigDecimal? =
        if (code == MDL) BigDecimal.ONE else rates[code]?.perUnit

    /** Валюты для выбора: лей, евро, доллар, затем остальные в порядке НБМ. */
    fun currencies(): List<Currency> {
        val result = mutableListOf(Currency(MDL, MDL_NAME))
        PREFERRED.mapNotNullTo(result) { code -> rates[code]?.let { Currency(it.code, it.name) } }
        rates.values.filter { it.code !in PREFERRED }.mapTo(result) { Currency(it.code, it.name) }
        return result
    }

    companion object {
        const val MDL = "MDL"
        const val MDL_NAME = "Молдавский лей"
        private val PREFERRED = listOf("EUR", "USD")
    }
}

/** Курс валютной пары на одну дату — точка графика. */
data class RatePoint(val date: LocalDate, val rate: BigDecimal)
