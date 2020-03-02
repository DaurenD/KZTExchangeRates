package any.exchange.model

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import java.util.*

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey
    val id: String,
    val date: Long,
    val ticker: String,
    val fullName: String,
    val price: Double,
    val change: Double,
    val quantity: Int = 1
)

@Dao
interface QuoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuote(quote: Quote): Completable

    @Query("SELECT * FROM quotes WHERE date = :date")
    fun getDayQuotes(date: Long): List<Quote>


    @Query("SELECT * FROM quotes")
    fun getAllQuotes(): List<Quote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(quotes: List<Quote>)

    @Query("SELECT * FROM quotes")
    fun getQuotesByOne(): Flowable<List<Quote>>

    @Query("SELECT * FROM quotes WHERE ticker = :ticker ORDER BY date desc")
    fun getQuotesBy(ticker: String): Flowable<List<Quote>>


}