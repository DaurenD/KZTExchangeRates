package any.exchange.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import any.exchange.model.Quote
import any.exchange.model.QuoteDao

@Database(entities = [Quote::class], version = 1)
abstract class AppDb : RoomDatabase() {

    abstract fun quotesDao(): QuoteDao

    companion object {

        @Volatile private var INSTANCE: AppDb? = null

        fun getInstance(context: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                AppDb::class.java,  "quotes")
                .build()
    }
}