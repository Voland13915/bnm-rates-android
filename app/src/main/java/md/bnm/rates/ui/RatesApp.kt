package md.bnm.rates.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import md.bnm.rates.data.RateRepository

private enum class Tab(val title: String, val icon: String) {
    CONVERTER("Конвертер", "⇄"),
    WEEK("Неделя", "7"),
    MONTH("Месяц", "30")
}

/** Три вкладки внизу экрана. ViewModel каждой вкладки живёт, пока открыто приложение. */
@Composable
fun RatesApp(repository: RateRepository) {
    var tab by rememberSaveable { mutableStateOf(Tab.CONVERTER) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.icon, fontWeight = FontWeight.Bold) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (tab) {
                Tab.CONVERTER -> ConverterScreen(viewModel(key = "converter") { ConverterViewModel(repository) })
                Tab.WEEK -> TrendScreen(viewModel(key = "week") { TrendViewModel(repository, Period.WEEK) })
                Tab.MONTH -> TrendScreen(viewModel(key = "month") { TrendViewModel(repository, Period.MONTH) })
            }
        }
    }
}
