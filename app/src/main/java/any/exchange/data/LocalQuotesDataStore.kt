package any.exchange.data

import any.exchange.model.Quote
import any.exchange.model.QuoteDao
import io.reactivex.Completable
import io.reactivex.Flowable

class LocalQuotesDataStore(private val quotesDao: QuoteDao) {

    fun getQuotes(date: Long): Flowable<List<Quote>> {
        println("local getQuotes: " + date)
        return Flowable.fromCallable { quotesDao.getDayQuotes(date) }
    }

    fun getQuoteByOne(): Flowable<List<Quote>> {
        return quotesDao.getQuotesByOne()
    }

    fun save(quote: Quote): Completable {
        println("saving quote: ${quote.date} : ${quote.ticker} : ${quote.price}")
        return quotesDao.insertQuote(quote)
    }

    fun insertAll(quotes: List<Quote>) {
        quotesDao.insertAll(quotes)
    }

    fun getQuotesBy(ticker: String): Flowable<List<Quote>> {
        return quotesDao.getQuotesBy(ticker)
    }
}