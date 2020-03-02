package any.exchange.data

import any.exchange.model.Quote
import any.exchange.model.QuoteDao
import any.exchange.model.Rates
import any.exchange.service.QuotesEndpoint
import any.exchange.utils.QuoteUtils
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory


class RemoteQuotesDataStore {

    // localStore
    // remoteStore
    private val remoteClient = Retrofit.Builder()
        .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
            setLevel(
                HttpLoggingInterceptor.Level.BODY
            )
        }).build())
        .baseUrl(QuoteUtils.BASE_URL)
        .addConverterFactory(
            SimpleXmlConverterFactory.createNonStrict(Persister(AnnotationStrategy()))
        )
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    private val quotesEndpoint = remoteClient.create(QuotesEndpoint::class.java)


    fun getQuotes(date: String): Observable<Rates> {
        return quotesEndpoint.allQuotes(date)
    }

}