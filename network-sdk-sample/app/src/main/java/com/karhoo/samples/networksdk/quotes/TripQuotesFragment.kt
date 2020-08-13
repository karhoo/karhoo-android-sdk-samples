package com.karhoo.samples.networksdk.quotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.planning.BookingPlanningStateViewModel
import com.karhoo.samples.networksdk.planning.BookingStatus
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.model.QuoteListV2
import com.karhoo.sdk.api.model.QuoteV2
import com.karhoo.sdk.api.model.QuotesSearch
import com.karhoo.sdk.api.network.observable.Observable
import com.karhoo.sdk.api.network.observable.Observer
import com.karhoo.sdk.api.network.response.Resource
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_trip_quotes.get_quotes_button
import kotlinx.android.synthetic.main.fragment_trip_quotes.loadingProgressBar
import kotlinx.android.synthetic.main.fragment_trip_quotes.quotes_list
import kotlinx.android.synthetic.main.fragment_trip_quotes.stop_button

class TripQuotesFragment : BaseFragment(), QuotesCategoriesSection.ClickListener {

    private lateinit var bookingQuoteStateViewModel: BookingQuoteStateViewModel
    private lateinit var bookingPlanningStateViewModel: BookingPlanningStateViewModel
    private var vehiclesObserver: Observer<Resource<QuoteListV2>>? = null
    private var vehiclesObservable: Observable<QuoteListV2>? = null
    private var availableVehicles: Map<String, List<QuoteV2>> = mutableMapOf()
    private val sectionAdapter = SectionedRecyclerViewAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trip_quotes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        get_quotes_button.setOnClickListener {
            cancelVehicleCallback()
            requestVehicleAvailability(bookingPlanningStateViewModel.currentState)
        }
        stop_button.setOnClickListener {
            cancelVehicleCallback()
        }

        sectionAdapter.addSection(QuotesCategoriesSection())
        quotes_list.apply {
            adapter = sectionAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun showLoading() {
        loadingProgressBar?.show()
    }

    private fun hideLoading() {
        loadingProgressBar?.hide()
    }

    private fun createPlanningObservable() =
            androidx.lifecycle.Observer<BookingStatus> { bookingStatus ->
                cancelVehicleCallback()
                requestVehicleAvailability(bookingStatus)
            }

    private fun requestVehicleAvailability(bookingStatus: BookingStatus?) {
        stop_button?.visibility = View.INVISIBLE
        showLoading()
        bookingStatus?.pickup?.let { bookingStatusPickup ->
            bookingStatus.destination?.let { bookingStatusDestination ->
                vehiclesObserver = quotesCallback()
                vehiclesObserver?.let { observer ->
                    vehiclesObservable = KarhooApi.quotesService
                            .quotesV2(QuotesSearch(origin = bookingStatusPickup,
                                                   destination = bookingStatusDestination))
                            .observable().apply { subscribe(observer) }
                }
            }
        }
    }

    private fun cancelVehicleCallback() {
        hideLoading()
        stop_button?.visibility = View.INVISIBLE
        vehiclesObserver?.let { vehiclesObservable?.apply { unsubscribe(it) } }
    }

    private fun quotesCallback() = object : Observer<Resource<QuoteListV2>> {
        override fun onValueChanged(value: Resource<QuoteListV2>) {
            when (value) {
                is Resource.Success -> updateVehicles(value.data)
                is Resource.Failure -> toastErrorMessage(value.error)
            }
        }
    }

    private fun updateVehicles(data: QuoteListV2) {
        stop_button?.visibility = View.VISIBLE
        availableVehicles = data.categories

        sectionAdapter.removeAllSections()

        val allQuotes = data.categories
        for ((category, quotes) in allQuotes) {
            val quotesCategories = QuotesCategoriesSection()
            quotesCategories.apply {
                header = category
                itemList = quotes
                clickListener = this@TripQuotesFragment
            }
            sectionAdapter.addSection(quotesCategories)
        }
        quotes_list?.adapter = sectionAdapter
    }

    override fun onItemRootViewClicked(section: QuotesCategoriesSection,
                                       itemAdapterPosition: Int) {
        cancelVehicleCallback()
        bookingQuoteStateViewModel.process(
                BookingSupplierViewContract.BookingSupplierEvent.SupplierItemClicked(
                        section.itemList[itemAdapterPosition]
                                                                                    )
                                          )
    }

    companion object {
        @JvmStatic
        fun newInstance(owner: LifecycleOwner,
                        bookingPlanningStateViewModel: BookingPlanningStateViewModel,
                        bookingQuoteStateViewModel: BookingQuoteStateViewModel) = TripQuotesFragment().apply {
            this.bookingPlanningStateViewModel = bookingPlanningStateViewModel
            this.bookingQuoteStateViewModel = bookingQuoteStateViewModel
            bookingPlanningStateViewModel.viewStates().observe(owner, createPlanningObservable())
        }
    }
}