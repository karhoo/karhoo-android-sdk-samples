package com.karhoo.samples.networksdk.quotes

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.karhoo.samples.networksdk.R
import com.karhoo.sdk.api.model.QuoteV2
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.utils.EmptyViewHolder


class QuotesCategoriesSection :
    Section(
        SectionParameters.builder()
            .itemResourceId(R.layout.item_quote)
            .headerResourceId(R.layout.item_quote_category)
            .build()
    ) {

    var itemList = listOf<QuoteV2>()
    var header = "Quotes"
    var clickListener: ClickListener? = null

    override fun getContentItemsTotal(): Int {
        return itemList.size // number of items of this section
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        // return a custom instance of ViewHolder for the items of this section
        return FleetViewHolder(view)
    }

    override fun onBindItemViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val itemHolder = holder as FleetViewHolder

        // bind your view here
        itemHolder.fleetName.text = itemList[position].fleet.name
        itemHolder.fleetName.setOnClickListener { v ->
            clickListener!!.onItemRootViewClicked(
                this,
                position
            )
        }
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        // return an empty instance of ViewHolder for the headers of this section
        val itemHolder = view.findViewById<TextView>(R.id.category)

        // bind your view here
        itemHolder.text = header
        return EmptyViewHolder(view)
    }

    internal inner class FleetViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val fleetName: TextView

        init {
            fleetName = itemView.findViewById<View>(R.id.fleet_name) as TextView
        }
    }

    interface ClickListener {
        fun onItemRootViewClicked(section: QuotesCategoriesSection, itemAdapterPosition: Int)
    }
}