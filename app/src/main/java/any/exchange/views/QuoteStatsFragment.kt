package any.exchange.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import any.exchange.R
import any.exchange.presentation.QuotesViewModel
import any.exchange.views.adapters.QuotesAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TICKER = "ticker"
class QuoteStatsFragment : Fragment() {
    private var ticker: String? = null

    private val quotesViewModel: QuotesViewModel by viewModel()

    private val quotesAdapter = QuotesAdapter(forStats = true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            ticker = it.getString(TICKER)
        }

        quotesViewModel.quotesByTicker(ticker!!).observe(this, Observer {
            println("quotesByTicker: $it")
            quotesAdapter.quotes.clear()
            quotesAdapter.quotes.addAll(it)
            quotesAdapter.notifyDataSetChanged()
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quote_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.statsRv)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = quotesAdapter
    }

    companion object {
        @JvmStatic
        fun newInstance(tickcer: String) =
            QuoteStatsFragment().apply {
                arguments = Bundle().apply {
                    putString(TICKER, tickcer)
                }
            }
    }
}
