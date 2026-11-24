package md.bnm.rates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun ConverterScreen(viewModel: ConverterViewModel) {
    val state = viewModel.state
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Конвертер валют", style = MaterialTheme.typography.headlineSmall)
        RatesStatus(state, onRetry = viewModel::load)

        OutlinedTextField(
            value = state.amountText,
            onValueChange = viewModel::onAmountChange,
            label = { Text("Сумма") },
            isError = state.amountError != null,
            supportingText = { state.amountError?.let { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        CurrencyPairPicker(
            from = state.from,
            to = state.to,
            currencies = state.currencies,
            onFromChange = viewModel::onFromChange,
            onToChange = viewModel::onToChange,
            onSwap = viewModel::swap
        )

        Button(
            onClick = viewModel::convert,
            enabled = state.canConvert,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Конвертировать")
        }

        state.result?.let { ResultCard(it) }
    }
}

/** Откуда курсы: загружаются, ошибка, сохранённые без сети или официальные на дату. */
@Composable
private fun RatesStatus(state: ConverterState, onRetry: () -> Unit) {
    val loaded = state.loaded
    val error = state.loadError
    when {
        state.loading -> LoadingRow("Загружаем курсы НБМ…")
        error != null -> ErrorCard(error, onRetry)
        loaded != null -> {
            val date = Format.date(loaded.rates.date)
            val offlineReason = loaded.offlineReason
            if (offlineReason != null) {
                WarningCard("$offlineReason. Используются сохранённые курсы НБМ на $date.", onRetry)
            } else {
                Text(
                    "Официальный курс НБМ на $date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loaded.rates.date != LocalDate.now()) {
                    Text(
                        "На выходные и праздники НБМ курс не устанавливает — действует курс на $date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: ConversionResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${Format.money(result.amount)} ${result.from} = ${Format.money(result.converted)} ${result.to}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Курс: 1 ${result.from} = ${Format.rate(result.rate)} ${result.to}",
                style = MaterialTheme.typography.bodyMedium
            )
            val source = "Источник: Национальный банк Молдовы, курс на ${Format.date(result.rateDate)}"
            Text(
                if (result.fromCache) "$source (сохранённые данные)" else source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
