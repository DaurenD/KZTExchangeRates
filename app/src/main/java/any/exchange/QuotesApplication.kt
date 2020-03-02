package any.exchange

import android.app.Application
import any.exchange.data.AppDb
import any.exchange.data.LocalQuotesDataStore
import any.exchange.data.RemoteQuotesDataStore
import any.exchange.domain.GetQuotesListUseCase
import any.exchange.presentation.QuotesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class QuotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()



        val appModules = module {
            single { RemoteQuotesDataStore() }
            single { LocalQuotesDataStore(AppDb.getInstance(this@QuotesApplication).quotesDao()) }
            single { GetQuotesListUseCase(get(), get()) }
            single { QuotesViewModel(get()) }
        }

        startKoin {
            androidContext(this@QuotesApplication)
            modules(appModules)
        }
    }
}