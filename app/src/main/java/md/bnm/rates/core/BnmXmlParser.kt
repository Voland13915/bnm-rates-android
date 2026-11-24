package md.bnm.rates.core

import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.IOException
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/** Ответ НБМ не удалось разобрать: это не XML или в нём нет обязательных полей. */
class MalformedResponseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Разбирает XML официальных курсов НБМ:
 * корень `ValCurs Date="dd.MM.yyyy"`, в нём элементы `Valute` с CharCode, Nominal, Name, Value.
 * Ответ без единого `Valute` — не ошибка, а «курсов на эту дату нет» ([RateSet.isEmpty]).
 */
class BnmXmlParser {

    fun parse(xml: String): RateSet {
        if (xml.isBlank()) throw MalformedResponseException("Сайт НБМ вернул пустой ответ")
        // DOCTYPE запрещён заранее: так защита от XXE одинакова на Android и на обычной JVM
        if (xml.contains("<!DOCTYPE", ignoreCase = true)) {
            throw MalformedResponseException("Ответ НБМ содержит недопустимый DOCTYPE")
        }

        val root = try {
            newBuilder().parse(InputSource(StringReader(xml))).documentElement
        } catch (e: SAXException) {
            throw MalformedResponseException("Сайт НБМ вернул некорректные данные", e)
        } catch (e: IOException) {
            throw MalformedResponseException("Сайт НБМ вернул некорректные данные", e)
        }

        if (root == null || root.tagName != "ValCurs") {
            throw MalformedResponseException("В ответе НБМ нет списка курсов")
        }

        val rates = LinkedHashMap<String, CurrencyRate>()
        val valutes = root.getElementsByTagName("Valute")
        for (i in 0 until valutes.length) {
            val rate = parseValute(valutes.item(i) as Element)
            rates[rate.code] = rate
        }
        return RateSet(parseDate(root.getAttribute("Date")), rates)
    }

    private fun parseDate(text: String): LocalDate = try {
        LocalDate.parse(text.trim(), DATE_FORMAT)
    } catch (e: DateTimeParseException) {
        throw MalformedResponseException("В ответе НБМ некорректная дата «$text»", e)
    }

    private fun parseValute(element: Element): CurrencyRate {
        val code = childText(element, "CharCode")
        val name = childText(element, "Name")
        val nominal = childText(element, "Nominal").toIntOrNull()?.takeIf { it > 0 }
            ?: throw MalformedResponseException("Некорректный номинал у валюты $code")
        val value = childText(element, "Value").replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?: throw MalformedResponseException("Некорректный курс у валюты $code")
        return CurrencyRate(code, name, nominal, value)
    }

    private fun childText(element: Element, tag: String): String {
        val nodes = element.getElementsByTagName(tag)
        val text = if (nodes.length > 0) nodes.item(0).textContent.trim() else ""
        if (text.isEmpty()) throw MalformedResponseException("У валюты нет поля $tag")
        return text
    }

    private fun newBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isExpandEntityReferences = false
        // На Android часть этих флагов не поддерживается — тогда достаточно запрета DOCTYPE выше
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true)
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false)
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false)
        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(RethrowingErrorHandler)
        return builder
    }

    private fun trySetFeature(factory: DocumentBuilderFactory, name: String, value: Boolean) {
        try {
            factory.setFeature(name, value)
        } catch (ignored: ParserConfigurationException) {
        }
    }

    /** Без него парсер печатает «[Fatal Error]» в консоль перед исключением. */
    private object RethrowingErrorHandler : ErrorHandler {
        override fun warning(e: SAXParseException) {}
        override fun error(e: SAXParseException) = throw e
        override fun fatalError(e: SAXParseException) = throw e
    }

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
