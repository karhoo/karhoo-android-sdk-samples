package com.karhoo.samples.uisdk.components.quotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.karhoo.samples.uisdk.components.R
import com.karhoo.samples.uisdk.components.base.BaseFragment
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.model.QuoteList
import com.karhoo.sdk.api.network.observable.Observable
import com.karhoo.sdk.api.network.observable.Observer
import com.karhoo.sdk.api.network.response.Resource
import com.karhoo.uisdk.KarhooUISDK
import com.karhoo.uisdk.base.listener.ErrorView
import com.karhoo.uisdk.base.snackbar.SnackbarConfig
import com.karhoo.uisdk.screen.booking.booking.supplier.BookingSupplierViewModel
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatus
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatusStateViewModel
import com.karhoo.uisdk.screen.booking.domain.supplier.AvailabilityProvider
import com.karhoo.uisdk.screen.booking.domain.supplier.KarhooAvailability
import com.karhoo.uisdk.screen.booking.domain.supplier.LiveFleetsViewModel
import com.karhoo.uisdk.screen.booking.supplier.category.CategoriesViewModel
import kotlinx.android.synthetic.main.fragment_trip_quotes.*

class TripQuotesFragment : BaseFragment() {

    private lateinit var bookingSupplierViewModel: BookingSupplierViewModel
    private lateinit var bookingStatusStateViewModel: BookingStatusStateViewModel
    private lateinit var categoriesViewModel: CategoriesViewModel
    private lateinit var liveFleetsViewModel: LiveFleetsViewModel

    private var availabilityProvider: AvailabilityProvider? = null

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

        supplier_list_widget.bindViewToData(
            requireActivity(),
            bookingStatusStateViewModel,
            bookingSupplierViewModel
        )
    }

    override fun onStop() {
        availabilityProvider?.cleanup()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        supplier_list_widget.initAvailability(requireActivity())
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
            bookingSupplierViewModel: BookingSupplierViewModel,
            categoriesViewModel: CategoriesViewModel,
            liveFleetsViewModel: LiveFleetsViewModel
        ) = TripQuotesFragment().apply {
            this.bookingStatusStateViewModel = bookingStatusStateViewModel
            this.bookingSupplierViewModel = bookingSupplierViewModel
            this.categoriesViewModel = categoriesViewModel
            this.liveFleetsViewModel = liveFleetsViewModel
            bookingStatusStateViewModel.viewStates().observe(owner, createPlanningObservable())
        }
    }
}