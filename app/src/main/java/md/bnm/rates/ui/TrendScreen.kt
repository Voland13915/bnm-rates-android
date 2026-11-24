package md.bnm.rates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.bnm.rates.core.PeriodStats
import md.bnm.rates.core.Trend
import md.bnm.rates.data.History
import md.bnm.rates.ui.theme.TrendColors

/** Экран «Неделя» или «Месяц»: график, минимум, максимум, среднее, изменение и тенденция. */
@Composable
fun TrendScreen(viewModel: TrendViewModel) {
    val state = viewModel.state
    val period = viewModel.period
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("${period.title}: ${state.from} → ${state.to}", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Официальный курс НБМ за последние ${period.days} дней, по рабочим дням",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CurrencyPairPicker(
            from = state.from,
            to = state.to,
            currencies = state.currencies,
            onFromChange = viewModel::onFromChange,
            onToChange = viewModel::onToChange,
            onSwap = viewModel::swap
        )

        if (state.loading) LoadingRow("Загружаем курсы за ${period.days} дней…")

        val error = state.error
        val history = state.history
        when {
            error != null -> ErrorCard(error, viewModel::load)
            history != null -> HistoryContent(history, state.stats, state.to, viewModel::load)
        }
    }
}

@Composable
private fun HistoryContent(history: History, stats: PeriodStats?, quote: String, onRetry: () -> Unit) {
    val warning = history.warning
    if (warning != null) {
        val lastDate = history.points.lastOrNull()?.let { " Последние данные — на ${Format.date(it.date)}." } ?: ""
        WarningCard("$warning. Не загружено рабочих дней: ${history.missingDays}.$lastDate", onRetry)
    }

    if (stats == null) {
        Text("НБМ не опубликовал курс этой пары за период.", style = MaterialTheme.typography.bodyMedium)
        return
    }

    Card(Modifier.fillMaxWidth()) {
        LineChart(history.points, stats, Modifier.padding(16.dp))
    }
    StatsCard(stats, quote)
    Text(
        "Источник: Национальный банк Молдовы · рабочих дней: ${history.points.size} · " +
            "данные на ${Format.date(stats.last.date)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatsCard(stats: PeriodStats, quote: String) {
    val (symbol, trendColor) = when (stats.trend) {
        Trend.UP -> "▲" to TrendColors.up
        Trend.DOWN -> "▼" to TrendColors.down
        Trend.FLAT -> "■" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRow("Минимум", "${Format.rate(stats.min.rate)} (${Format.shortDate(stats.min.date)})")
            StatRow("Максимум", "${Format.rate(stats.max.rate)} (${Format.shortDate(stats.max.date)})")
            StatRow("Среднее", Format.rate(stats.average))
            StatRow(
                "Изменение",
                "${Format.signedRate(stats.absoluteChange)} $quote (${Format.percent(stats.percentChange)})",
                trendColor
            )
            StatRow("Тенденция", "$symbol ${stats.trend.label}", trendColor)
            StatRow("Период", "${Format.shortDate(stats.first.date)} – ${Format.shortDate(stats.last.date)}")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
