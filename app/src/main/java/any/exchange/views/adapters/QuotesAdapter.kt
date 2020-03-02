package any.exchange.views.adapters

import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import any.exchange.R
import any.exchange.model.Quote
import any.exchange.utils.QuoteUtils
import any.exchange.views.ListQuotesFragment

class QuotesAdapter(
    val forStats: Boolean = false,
    var listener: ListQuotesFragment.OnFragmentInteractionListener? = null,
    val quotes: MutableList<Quote> = mutableListOf()
) :
    RecyclerView.Adapter<QuotesAdapter.QuoteHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quote, parent, false)
        return QuoteHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteHolder, position: Int) {
        holder.bind(quotes[position])
    }

    override fun getItemCount(): Int = quotes.size

    inner class QuoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tickerTv = itemView.findViewById<TextView>(R.id.tickerTv)
        private val fullnameTv = itemView.findViewById<TextView>(R.id.fullNameTv)
        private val priceTv = itemView.findViewById<TextView>(R.id.priceTv)
        private val changeTv = itemView.findViewById<TextView>(R.id.changeTv)
        private val dateTv = itemView.findViewById<TextView>(R.id.dateTv)

        fun bind(quote: Quote) {
            tickerTv.text = quote.ticker
            fullnameTv.text = quote.fullName
            priceTv.text = quote.price.toString()
            changeTv.text = quote.change.toString()
            if(forStats){
                dateTv.text = QuoteUtils.DATE_FORMAT.format(quote.date)
                dateTv.visibility = VISIBLE
            } else {
                dateTv.visibility = GONE
            }

            itemView.setOnClickListener {
                listener?.onQuoteSelected(quote)
            }
        }
    }
}