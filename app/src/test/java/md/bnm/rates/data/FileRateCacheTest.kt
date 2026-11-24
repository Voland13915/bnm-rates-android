package md.bnm.rates.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class FileRateCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val friday = LocalDate.of(2026, 11, 20)
    private val monday = LocalDate.of(2026, 11, 23)

    @Test
    fun savedResponseSurvivesRestart() {
        val dir = folder.newFolder("rates")
        FileRateCache(dir).put(friday, "<ValCurs/>")

        // Новый объект над той же папкой — как после перезапуска приложения
        assertEquals("<ValCurs/>", FileRateCache(dir).get(friday))
    }

    @Test
    fun missingDateIsNull() {
        assertNull(FileRateCache(folder.newFolder()).get(friday))
    }

    @Test
    fun putReplacesExistingFileAndLeavesNoTemporaryFiles() {
        val dir = folder.newFolder()
        val cache = FileRateCache(dir)
        cache.put(friday, "old")
        cache.put(friday, "new")

        assertEquals("new", cache.get(friday))
        assertEquals(listOf("2026-11-20.xml"), dir.list()!!.toList())
    }

    @Test
    fun createsFolderIfMissing() {
        val dir = File(folder.root, "not/yet/created")
        FileRateCache(dir).put(friday, "x")

        assertEquals("x", FileRateCache(dir).get(friday))
    }

    @Test
    fun latestOnOrBeforeIgnoresLaterDatesAndForeignFiles() {
        val dir = folder.newFolder()
        val cache = FileRateCache(dir)
        cache.put(LocalDate.of(2026, 11, 18), "a")
        cache.put(friday, "b")
        cache.put(monday, "c")
        File(dir, "notes.txt").writeText("not a rate file")
        File(dir, "garbage.xml").writeText("not a date")

        assertEquals(friday, cache.latestOnOrBefore(LocalDate.of(2026, 11, 22)))
        assertEquals(monday, cache.latestOnOrBefore(monday))
        assertNull(cache.latestOnOrBefore(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun latestOnEmptyCacheIsNull() {
        assertNull(FileRateCache(File(folder.root, "absent")).latestOnOrBefore(monday))
    }
}
