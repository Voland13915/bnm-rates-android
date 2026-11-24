package md.bnm.rates.core

import java.math.BigDecimal
import java.math.RoundingMode

class UnknownCurrencyException(val code: String) : Exception("Нет курса НБМ для валюты $code")

/** Пересчёт между любыми двумя валютами через лей: курсы НБМ даны к MDL. */
object Converter {

    /** Знаков у кросс-курса: у НБМ 4 знака, деление на номинал и на второй курс добавляет точность. */
    const val RATE_SCALE = 6

    /** Сколько единиц [to] стоит одна единица [from]. */
    fun crossRate(rates: RateSet, from: String, to: String): BigDecimal =
        perUnit(rates, from).divide(perUnit(rates, to), RATE_SCALE, RoundingMode.HALF_UP)

    /** amount × курс [from] ÷ курс [to]; деление одно и последнее, чтобы не копить округления. */
    fun convert(amount: BigDecimal, from: String, to: String, rates: RateSet): BigDecimal =
        amount.multiply(perUnit(rates, from)).divide(perUnit(rates, to), 2, RoundingMode.HALF_UP)

    private fun perUnit(rates: RateSet, code: String): BigDecimal =
        rates.perUnit(code) ?: throw UnknownCurrencyException(code)
}
