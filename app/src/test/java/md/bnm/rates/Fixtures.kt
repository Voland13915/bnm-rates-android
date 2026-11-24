package md.bnm.rates

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Тестовые данные: настоящие ответы НБМ из resources и сгенерированные ответы в том же формате. */
object Fixtures {

    /** Ответ НБМ за пятницу 20.11.2026. */
    val WEEKDAY: String get() = read("bnm-2026-11-20.xml")

    /** Ответ НБМ за воскресенье 22.11.2026: курсы пятницы, а в атрибуте Date — воскресенье. */
    val WEEKEND: String get() = read("bnm-2026-11-22.xml")

    fun read(name: String): String =
        Fixtures::class.java.classLoader!!.getResource(name)!!.readText(Charsets.UTF_8)

    /** Ответ в формате НБМ: [rates] — код валюты и курс за одну единицу. */
    fun xml(date: LocalDate, rates: Map<String, String>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<ValCurs Date=\"${date.format(DATE_FORMAT)}\" name=\"Официальный курс\">\n")
        for ((code, value) in rates) {
            append("  <Valute ID=\"1\"><NumCode>000</NumCode><CharCode>$code</CharCode>")
            append("<Nominal>1</Nominal><Name>$code</Name><Value>$value</Value></Valute>\n")
        }
        append("</ValCurs>\n")
    }

    /** Ответ без курсов: так НБМ отвечает на дату, на которую курс не установлен. */
    fun empty(date: LocalDate): String = xml(date, emptyMap())

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
}
