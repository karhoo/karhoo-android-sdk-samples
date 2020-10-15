package com.karhoo.samples.networksdk.quotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.base.BaseFragment
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

class TripQuotesFragment : BaseFragment(),ErrorView {

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
        get_quotes_button.setOnClickListener {
            cancelVehicleCallback()
        }
        stop_button.setOnClickListener {
            cancelVehicleCallback()
        }

        supplier_list_widget.bindViewToData(requireActivity(), bookingStatusStateViewModel,
            categoriesViewModel, liveFleetsViewModel, bookingSupplierViewModel)
    }

    private fun showLoading() {
        loadingProgressBar?.show()
    }

    private fun hideLoading() {
        loadingProgressBar?.hide()
    }

    private fun initAvailability() {
        availabilityProvider?.cleanup()
        availabilityProvider = KarhooAvailability(
            KarhooApi.quotesService,
            KarhooUISDK.analytics, categoriesViewModel, liveFleetsViewModel,
            bookingStatusStateViewModel, this).apply {
            setErrorView(this@TripQuotesFragment)
            setAllCategory(getString(com.karhoo.uisdk.R.string.all_category))
            supplier_list_widget.bindAvailability(this)
        }
    }

    override fun onStop() {
        availabilityProvider?.cleanup()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        initAvailability()
    }

    private fun createPlanningObservable() =
        androidx.lifecycle.Observer<BookingStatus> {
            cancelVehicleCallback()
        }

    private fun cancelVehicleCallback() {
        hideLoading()
        stop_button?.visibility = View.INVISIBLE
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

    override fun dismissSnackbar() {
        supplier_list_widget.setSupplierListVisibility()
    }

    override fun showErrorDialog(stringId: Int) {
        TODO("Not yet implemented")
    }

    override fun showSnackbar(snackbarConfig: SnackbarConfig) {
        TODO("Not yet implemented")
    }

    override fun showTopBarNotification(stringId: Int) {
        TODO("Not yet implemented")
    }

    override fun showTopBarNotification(value: String) {
        TODO("Not yet implemented")
    }
}