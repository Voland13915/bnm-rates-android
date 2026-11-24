package md.bnm.rates.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Единый формат чисел и дат на всех экранах. */
object Format {
    private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM")
    private val SMALL = BigDecimal("0.01")

    fun date(date: LocalDate): String = date.format(DATE)

    fun shortDate(date: LocalDate): String = date.format(SHORT_DATE)

    fun money(amount: BigDecimal): String = amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

    /** 4 знака, а для маленьких курсов (меньше 0.01) — 6, иначе они превратятся в нули. */
    fun rate(rate: BigDecimal): String {
        val scale = if (rate.abs() < SMALL) 6 else 4
        return rate.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }

    fun signedRate(change: BigDecimal): String = signed(change, rate(change))

    fun percent(percent: BigDecimal): String = signed(percent, percent.setScale(2, RoundingMode.HALF_UP).toPlainString()) + " %"

    private fun signed(value: BigDecimal, text: String) = if (value.signum() > 0) "+$text" else text
}
