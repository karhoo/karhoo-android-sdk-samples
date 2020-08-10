package com.karhoo.samples.networksdk.quotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.karhoo.samples.networksdk.R
import com.karhoo.sdk.api.model.QuoteV2

class QuotesAdapter(private val quotes: List<QuoteV2>) :
    RecyclerView.Adapter<QuotesAdapter.ViewHolder>() {
    class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        val nameTextView = itemView.findViewById<TextView>(R.id.fleet_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuotesAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val contactView = inflater.inflate(R.layout.item_quote, parent, false)
        return ViewHolder(contactView)
    }

    override fun onBindViewHolder(viewHolder: QuotesAdapter.ViewHolder, position: Int) {
        val contact: QuoteV2 = quotes.get(position)
        val textView = viewHolder.nameTextView
        textView.text = contact.fleet.name
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return quotes.size
    }
}