package md.bnm.rates.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import md.bnm.rates.core.AmountInput
import md.bnm.rates.core.Converter
import md.bnm.rates.core.Currency
import md.bnm.rates.core.RateSet
import md.bnm.rates.core.UnknownCurrencyException
import md.bnm.rates.data.LoadedRates
import md.bnm.rates.data.RateRepository
import java.math.BigDecimal
import java.time.LocalDate

/** Показанный результат: всё, что нужно, чтобы объяснить, откуда число. */
data class ConversionResult(
    val amount: BigDecimal,
    val from: String,
    val to: String,
    val converted: BigDecimal,
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val fromCache: Boolean
)

data class ConverterState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val loaded: LoadedRates? = null,
    val amountText: String = "100",
    val from: String = "EUR",
    val to: String = RateSet.MDL,
    val amountError: String? = null,
    val result: ConversionResult? = null
) {
    val currencies: List<Currency>
        get() = loaded?.rates?.currencies() ?: emptyList()

    /** Кнопка активна, когда поле не пустое; корректность суммы проверяется по нажатию. */
    val canConvert: Boolean
        get() = loaded != null && amountText.isNotBlank()
}

class ConverterViewModel(private val repository: RateRepository) : ViewModel() {

    var state by mutableStateOf(ConverterState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(loading = true, loadError = null)
        viewModelScope.launch {
            state = try {
                val loaded = withContext(Dispatchers.IO) { repository.latest(LocalDate.now()) }
                state.copy(loading = false, loaded = loaded).recalculated()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.copy(loading = false, loadError = e.message ?: "Не удалось загрузить курсы НБМ")
            }
        }
    }

    fun onAmountChange(text: String) {
        state = state.copy(amountText = text, amountError = null, result = null)
    }

    fun onFromChange(code: String) {
        state = state.copy(from = code).recalculated()
    }

    fun onToChange(code: String) {
        state = state.copy(to = code).recalculated()
    }

    fun swap() {
        state = state.copy(from = state.to, to = state.from).recalculated()
    }

    fun convert() {
        state = when (val input = AmountInput.parse(state.amountText)) {
            is AmountInput.Invalid -> state.copy(amountError = input.message, result = null)
            is AmountInput.Valid -> state.withResult(input.amount)
        }
    }

    /** Уже показанный результат пересчитывается после смены валют или загрузки курсов. */
    private fun ConverterState.recalculated(): ConverterState {
        val shown = result ?: return this
        return withResult(shown.amount)
    }

    private fun ConverterState.withResult(amount: BigDecimal): ConverterState {
        val current = loaded ?: return this
        return try {
            val result = ConversionResult(
                amount = amount,
                from = from,
                to = to,
                converted = Converter.convert(amount, from, to, current.rates),
                rate = Converter.crossRate(current.rates, from, to),
                rateDate = current.rates.date,
                fromCache = current.offlineReason != null
            )
            copy(amountError = null, result = result)
        } catch (e: UnknownCurrencyException) {
            copy(amountError = e.message, result = null)
        }
    }
}
