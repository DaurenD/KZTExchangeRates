package any.exchange.views

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import any.exchange.R
import any.exchange.model.Quote
import any.exchange.presentation.QuotesViewModel
import any.exchange.views.adapters.QuotesAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel


class ListQuotesFragment : Fragment() {

    private val quotesViewModel: QuotesViewModel by viewModel()

    private var listener: OnFragmentInteractionListener? = null

    private val quotesAdapter = QuotesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quotesViewModel.quotes().observe(this, Observer {
            quotesAdapter.quotes.clear()
            quotesAdapter.quotes.addAll(it)
            quotesAdapter.notifyDataSetChanged()
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_list_quotes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.quotesRv)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        recyclerView.adapter = quotesAdapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
            quotesAdapter.listener = listener
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onQuoteSelected(quote: Quote)
    }

    companion object {
        const val NAME = "QuotesList"
    }
}
