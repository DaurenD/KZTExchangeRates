package any.exchange.domain

import any.exchange.data.LocalQuotesDataStore
import any.exchange.data.RemoteQuotesDataStore
import any.exchange.model.Quote
import any.exchange.model.Rates
import any.exchange.utils.QuoteUtils
import io.reactivex.*
import io.reactivex.schedulers.Schedulers

class GetQuotesListUseCase(
    private val remoteQuotes: RemoteQuotesDataStore,
    private val localQuotes: LocalQuotesDataStore
) {

    fun getLastDayQuotes(): Observable<Rates> {
        val rates = localQuotes.getQuotes(QuoteUtils.getCurrentDateLong()).switchMap {
            Observable.just(Rates().apply {
                items = it.map { quote -> Rates.Item().apply { fromQuote(quote) } }
                println("getLastDayQuotes: ${it.size}")
            }).toFlowable(BackpressureStrategy.LATEST)
        }.toObservable()


        return Observable.mergeDelayError(remoteQuotes.getQuotes(QuoteUtils.getCurrentDate()).doOnNext {
            println("got rates: ${it.items?.size}")
            val quotes = mutableListOf<Quote>()
            it.items?.forEach { respQuote ->
                quotes.add(
                    Quote(
                        id = QuoteUtils.getCurrentDate() + respQuote.title,
                        date = QuoteUtils.DATE_FORMAT.parse(it.date)!!.time,
                        ticker = respQuote.title,
                        fullName = respQuote.fullname,
                        price = respQuote.description.toDouble(),
                        quantity = respQuote.quant.toInt(),
                        change = respQuote.change.toDouble()
                    )
                )
            }
            localQuotes.insertAll(quotes)
        }.subscribeOn(Schedulers.io()), rates.subscribeOn(Schedulers.io()))
    }

    fun getLocalQuotes(): Flowable<List<Quote>> {
        return localQuotes.getQuoteByOne()
    }

    fun insertDummy(): Completable {
        val quote = Quote(
            id = "1",
            date = 1603087204000,
            ticker = "AAPL",
            fullName = "Apple Inc",
            price = 270.0,
            change = 0.0,
            quantity = 1
        )
        return localQuotes.save(quote)
    }

    fun getQuotesByTicker(ticker: String): Flowable<List<Quote>> {
        return localQuotes.getQuotesBy(ticker)
    }
}