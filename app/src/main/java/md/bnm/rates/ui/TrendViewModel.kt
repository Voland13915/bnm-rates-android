package md.bnm.rates.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import md.bnm.rates.core.Currency
import md.bnm.rates.core.PeriodStats
import md.bnm.rates.core.RateSet
import md.bnm.rates.data.History
import md.bnm.rates.data.RateRepository
import java.time.LocalDate

enum class Period(val days: Int, val title: String) {
    WEEK(7, "Неделя"),
    MONTH(30, "Месяц")
}

data class TrendState(
    val from: String = "EUR",
    val to: String = RateSet.MDL,
    val currencies: List<Currency> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val history: History? = null,
    val stats: PeriodStats? = null
)

class TrendViewModel(private val repository: RateRepository, val period: Period) : ViewModel() {

    var state by mutableStateOf(TrendState())
        private set

    private var job: Job? = null

    init {
        load()
    }

    fun onFromChange(code: String) {
        state = state.copy(from = code)
        load()
    }

    fun onToChange(code: String) {
        state = state.copy(to = code)
        load()
    }

    fun swap() {
        state = state.copy(from = state.to, to = state.from)
        load()
    }

    /** Предыдущая загрузка отменяется: результат для старой пары не должен перезаписать новый. */
    fun load() {
        job?.cancel()
        val from = state.from
        val to = state.to
        state = state.copy(loading = true, error = null)
        job = viewModelScope.launch {
            state = try {
                val history = withContext(Dispatchers.IO) {
                    repository.history(from, to, period.days, LocalDate.now())
                }
                state.copy(
                    loading = false,
                    history = history,
                    stats = PeriodStats.of(history.points),
                    currencies = history.currencies.ifEmpty { state.currencies }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.copy(
                    loading = false,
                    error = e.message ?: "Не удалось загрузить курсы НБМ",
                    history = null,
                    stats = null
                )
            }
        }
    }
}
