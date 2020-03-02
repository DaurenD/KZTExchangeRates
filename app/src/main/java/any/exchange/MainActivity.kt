package any.exchange

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import any.exchange.model.Quote
import any.exchange.views.ListQuotesFragment
import any.exchange.views.QuoteStatsFragment


class MainActivity : AppCompatActivity(), ListQuotesFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, ListQuotesFragment(), ListQuotesFragment.NAME)
            .commit()
    }

    override fun onQuoteSelected(quote: Quote) {
        supportFragmentManager.beginTransaction()
            .addToBackStack(ListQuotesFragment.NAME)
            .replace(R.id.fragmentContainer, QuoteStatsFragment.newInstance(quote.ticker), "QuoteStats")
            .commit()
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount > 1){
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}
