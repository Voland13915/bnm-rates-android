package md.bnm.rates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import md.bnm.rates.ui.RatesApp
import md.bnm.rates.ui.theme.RatesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as RatesApplication).repository
        setContent {
            RatesTheme {
                RatesApp(repository)
            }
        }
    }
}
