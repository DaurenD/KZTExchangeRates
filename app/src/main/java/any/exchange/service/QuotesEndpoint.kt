package any.exchange.service

import any.exchange.model.Rates
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.collections.ArrayList

interface QuotesEndpoint {

    @GET("rss/get_rates.cfm")
    fun allQuotes(@Query("fdate") forDate: String): Observable<Rates>

}