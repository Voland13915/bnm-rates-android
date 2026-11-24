package md.bnm.rates.core

import java.math.BigDecimal
import java.math.RoundingMode

enum class Trend(val label: String) {
    UP("Рост"),
    DOWN("Падение"),
    FLAT("Без изменений")
}

/** Статистика курса за период для экранов «Неделя» и «Месяц». */
data class PeriodStats(
    val first: RatePoint,
    val last: RatePoint,
    val min: RatePoint,
    val max: RatePoint,
    val average: BigDecimal,
    /** Последний курс минус первый. */
    val absoluteChange: BigDecimal,
    /** Изменение в процентах от первого курса. */
    val percentChange: BigDecimal,
    val trend: Trend
) {
    companion object {
        /** Как у кросс-курса: у пар с маленьким курсом (JPY → USD ≈ 0.0063) 4 знаков мало. */
        const val SCALE = Converter.RATE_SCALE

        /**
         * null, если точек нет: из пустого набора статистику не посчитать.
         * Тенденция определяется по проценту изменения с двумя знаками — тем, что видит пользователь:
         * изменение меньше 0.01 % считается отсутствием изменений.
         */
        fun of(points: List<RatePoint>): PeriodStats? {
            if (points.isEmpty()) return null
            val sorted = points.sortedBy { it.date }
            val first = sorted.first()
            val last = sorted.last()

            // reduce оставляет первую точку при равенстве — минимум и максимум с самой ранней датой
            val min = sorted.reduce { a, b -> if (b.rate < a.rate) b else a }
            val max = sorted.reduce { a, b -> if (b.rate > a.rate) b else a }
            val sum = sorted.fold(BigDecimal.ZERO) { acc, point -> acc + point.rate }
            val average = sum.divide(BigDecimal(sorted.size), SCALE, RoundingMode.HALF_UP)

            val difference = last.rate - first.rate
            val change = difference.setScale(SCALE, RoundingMode.HALF_UP)
            val percent = difference.multiply(HUNDRED).divide(first.rate, 2, RoundingMode.HALF_UP)
            val trend = when (percent.signum()) {
                1 -> Trend.UP
                -1 -> Trend.DOWN
                else -> Trend.FLAT
            }
            return PeriodStats(first, last, min, max, average, change, percent, trend)
        }

        private val HUNDRED = BigDecimal(100)
    }
}
