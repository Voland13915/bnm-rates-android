package md.bnm.rates.core

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * НБМ устанавливает курс только на рабочие дни. На субботу и воскресенье сайт отдаёт курс пятницы,
 * подставляя в ответ запрошенную дату, поэтому выходные мы не запрашиваем вовсе.
 */
object BusinessDays {

    fun isBusinessDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY

    /** Последний рабочий день не позже [date]: для субботы и воскресенья — пятница. */
    fun latest(date: LocalDate): LocalDate {
        var day = date
        while (!isBusinessDay(day)) day = day.minusDays(1)
        return day
    }

    fun previous(date: LocalDate): LocalDate = latest(date.minusDays(1))

    /** Рабочие дни за последние [days] календарных дней, включая [today], по возрастанию даты. */
    fun inLastDays(today: LocalDate, days: Int): List<LocalDate> {
        require(days > 0) { "days must be positive" }
        return (days - 1 downTo 0).map { today.minusDays(it.toLong()) }.filter { isBusinessDay(it) }
    }
}
