package md.bnm.rates.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Сайт НБМ недоступен; [kind] — почему, текст сообщения — для пользователя. */
class SourceException(val kind: Kind, message: String, cause: Throwable? = null) : Exception(message, cause) {
    enum class Kind { NO_NETWORK, TIMEOUT, SERVER }
}

/** Откуда берётся XML с курсами НБМ на дату. */
interface RateSource {
    /** @throws SourceException если сайт недоступен или ответил ошибкой */
    fun fetch(date: LocalDate): String
}

/** Сайт Национального банка Молдовы: https://www.bnm.md/ru/official_exchange_rates?get_xml=1&date=dd.MM.yyyy */
class BnmHttpSource(private val baseUrl: String = DEFAULT_URL) : RateSource {

    override fun fetch(date: LocalDate): String {
        val connection = URL("$baseUrl?get_xml=1&date=${date.format(DATE_FORMAT)}").openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw SourceException(SourceException.Kind.SERVER, "Сайт НБМ ответил ошибкой $code")
            }
            return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: SocketTimeoutException) {
            throw SourceException(SourceException.Kind.TIMEOUT, "Сайт НБМ не ответил вовремя", e)
        } catch (e: IOException) {
            throw SourceException(SourceException.Kind.NO_NETWORK, "Нет соединения с сайтом НБМ", e)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Русская версия сайта: названия валют приходят на русском. */
        const val DEFAULT_URL = "https://www.bnm.md/ru/official_exchange_rates"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 10_000
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
