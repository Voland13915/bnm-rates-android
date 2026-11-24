package md.bnm.rates

import android.app.Application
import md.bnm.rates.data.BnmHttpSource
import md.bnm.rates.data.FileRateCache
import md.bnm.rates.data.RateRepository
import java.io.File

/** Создаёт единственный репозиторий: кэш ответов НБМ лежит в личной папке приложения. */
class RatesApplication : Application() {

    val repository: RateRepository by lazy {
        RateRepository(BnmHttpSource(), FileRateCache(File(filesDir, "rates")))
    }
}
