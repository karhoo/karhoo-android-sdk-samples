package com.karhoo.samples.uisdk.components.quotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.karhoo.samples.uisdk.components.R
import com.karhoo.samples.uisdk.components.base.BaseFragment
import com.karhoo.sdk.api.model.QuoteList
import com.karhoo.sdk.api.network.observable.Observable
import com.karhoo.sdk.api.network.observable.Observer
import com.karhoo.sdk.api.network.response.Resource
import com.karhoo.uisdk.screen.booking.booking.quotes.BookingQuotesViewModel
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatus
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatusStateViewModel
import com.karhoo.uisdk.screen.booking.domain.quotes.LiveFleetsViewModel
import com.karhoo.uisdk.screen.booking.quotes.category.CategoriesViewModel
import kotlinx.android.synthetic.main.fragment_trip_quotes.*

class TripQuotesFragment : BaseFragment() {

    private lateinit var bookingQuotesViewModel: BookingQuotesViewModel
    private lateinit var bookingStatusStateViewModel: BookingStatusStateViewModel

    private var vehiclesObserver: Observer<Resource<QuoteList>>? = null
    private var vehiclesObservable: Observable<QuoteList>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_quotes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quotes_list_widget.bindViewToData(
            requireActivity(),
            bookingStatusStateViewModel,
            bookingQuotesViewModel
        )
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        quotes_list_widget.initAvailability(requireActivity())
    }

    private fun createPlanningObservable() =
        androidx.lifecycle.Observer<BookingStatus> {
            cancelVehicleCallback()
        }

    private fun cancelVehicleCallback() {
        vehiclesObserver?.let { vehiclesObservable?.apply { unsubscribe(it) } }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingStatusStateViewModel: BookingStatusStateViewModel,
            bookingQuotesViewModel: BookingQuotesViewModel
        ) = TripQuotesFragment().apply {
            this.bookingStatusStateViewModel = bookingStatusStateViewModel
            this.bookingQuotesViewModel = bookingQuotesViewModel
            bookingStatusStateViewModel.viewStates().observe(owner, createPlanningObservable())
        }
    }
}