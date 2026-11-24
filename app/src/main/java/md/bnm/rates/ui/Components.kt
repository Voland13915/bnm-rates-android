package md.bnm.rates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import md.bnm.rates.core.Currency

/** Пара валют: «из» ⇄ «в». */
@Composable
fun CurrencyPairPicker(
    from: String,
    to: String,
    currencies: List<Currency>,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CurrencyPicker(from, currencies, onFromChange, Modifier.weight(1f))
        IconButton(onClick = onSwap) {
            Text("⇄", style = MaterialTheme.typography.titleLarge)
        }
        CurrencyPicker(to, currencies, onToChange, Modifier.weight(1f))
    }
}

/** Кнопка с кодом валюты и выпадающий список всех валют НБМ. */
@Composable
fun CurrencyPicker(
    selected: String,
    currencies: List<Currency>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = currencies.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$selected ▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.code} — ${currency.name}") },
                    onClick = {
                        expanded = false
                        onSelect(currency.code)
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Курсов нет совсем: что случилось и кнопка повтора. */
@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    MessageCard(
        message = message,
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        action = "Повторить",
        onAction = onRetry
    )
}

/** Показаны сохранённые данные: почему и кнопка обновления. */
@Composable
fun WarningCard(message: String, onRetry: () -> Unit) {
    MessageCard(
        message = message,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        action = "Обновить",
        onAction = onRetry
    )
}

@Composable
private fun MessageCard(message: String, container: Color, content: Color, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(action)
            }
        }
    }
}
