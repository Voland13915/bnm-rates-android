package md.bnm.rates.data

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Хранилище ответов НБМ по датам. Курс на прошедшую дату не меняется, поэтому хранится бессрочно. */
interface RateCache {
    fun get(date: LocalDate): String?

    fun put(date: LocalDate, xml: String)

    /** Самая поздняя сохранённая дата не позже [date] или null, если ничего не сохранено. */
    fun latestOnOrBefore(date: LocalDate): LocalDate?
}

/**
 * Один файл на дату: `2026-11-20.xml` в папке приложения. Переживает перезапуск приложения.
 * Запись через временный файл: при сбое посреди записи старый файл остаётся целым.
 */
class FileRateCache(private val dir: File) : RateCache {

    override fun get(date: LocalDate): String? {
        val file = fileFor(date)
        return if (file.isFile) file.readText(Charsets.UTF_8) else null
    }

    override fun put(date: LocalDate, xml: String) {
        dir.mkdirs()
        val temp = File.createTempFile(date.toString(), ".tmp", dir)
        try {
            temp.writeText(xml, Charsets.UTF_8)
            Files.move(temp.toPath(), fileFor(date).toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    override fun latestOnOrBefore(date: LocalDate): LocalDate? =
        (dir.listFiles() ?: emptyArray())
            .mapNotNull { dateOf(it) }
            .filter { !it.isAfter(date) }
            .sorted()
            .lastOrNull()

    private fun fileFor(date: LocalDate) = File(dir, "$date$SUFFIX")

    private fun dateOf(file: File): LocalDate? {
        if (!file.name.endsWith(SUFFIX)) return null
        return try {
            LocalDate.parse(file.name.removeSuffix(SUFFIX))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private companion object {
        const val SUFFIX = ".xml"
    }
}
