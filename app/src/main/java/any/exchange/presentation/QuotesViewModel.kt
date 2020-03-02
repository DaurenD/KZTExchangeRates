package any.exchange.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import any.exchange.domain.GetQuotesListUseCase
import any.exchange.model.Quote
import any.exchange.utils.QuoteUtils.DATE_FORMAT
import any.exchange.utils.QuoteUtils.getCurrentDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class QuotesViewModel(private val quotesListUseCase: GetQuotesListUseCase) : ViewModel() {

    private val quotes = MutableLiveData<List<Quote>>()

    private val disposables = CompositeDisposable()

    init {
        loadQuotes(getCurrentDate())
        localQuotes()
    }

    // get list of quotes against KZT
    fun quotes(): LiveData<List<Quote>> = quotes

    fun localQuotes() {
        disposables.add(
            quotesListUseCase.getLocalQuotes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    println("localQuotes: ${it.size}")
                }, {
                    println("error local quotes: $it")
                })
        )
    }

    fun quotesByTicker(ticker: String): LiveData<List<Quote>> {
        val result = MutableLiveData<List<Quote>>()
        disposables.add(
            quotesListUseCase.getQuotesByTicker(ticker)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    result.value = it
                }
        )
        return result
    }

    fun loadQuotes(date: String) {
        disposables.add(
            quotesListUseCase.getLastDayQuotes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val quotesList = ArrayList<Quote>()
                    it.items?.forEach { respQuote ->
                        quotesList.add(
                            Quote(
                                id = it.date + respQuote.title,
                                date = DATE_FORMAT.parse(date)!!.time,
                                ticker = respQuote.title,
                                fullName = respQuote.fullname,
                                price = respQuote.description.toDouble(),
                                quantity = respQuote.quant.toInt(),
                                change = respQuote.change.toDouble()
                            )
                        )
                    }
                    quotes.value = quotesList
                }, {
                    println("Error while getting quote: $it")
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

}