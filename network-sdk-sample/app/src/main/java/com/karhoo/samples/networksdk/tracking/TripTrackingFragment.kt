package com.karhoo.samples.networksdk.tracking

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.booking.BookingRequestStateViewModel
import com.karhoo.samples.networksdk.booking.BookingRequestStatus
import com.karhoo.samples.networksdk.booking.BookingRequestViewContract
import com.karhoo.samples.networksdk.utils.CurrencyUtils
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.model.DriverTrackingInfo
import com.karhoo.sdk.api.model.TripInfo
import com.karhoo.sdk.api.network.observable.Observable
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_trip_tracking.driver_eta
import kotlinx.android.synthetic.main.fragment_trip_tracking.fleet_name
import kotlinx.android.synthetic.main.fragment_trip_tracking.latitude
import kotlinx.android.synthetic.main.fragment_trip_tracking.loadingProgressBar
import kotlinx.android.synthetic.main.fragment_trip_tracking.longitude
import kotlinx.android.synthetic.main.fragment_trip_tracking.price
import kotlinx.android.synthetic.main.fragment_trip_tracking.quote_id
import kotlinx.android.synthetic.main.fragment_trip_tracking.registration_number
import kotlinx.android.synthetic.main.fragment_trip_tracking.selected_dropoff
import kotlinx.android.synthetic.main.fragment_trip_tracking.selected_pickup
import kotlinx.android.synthetic.main.fragment_trip_tracking.status
import kotlinx.android.synthetic.main.fragment_trip_tracking.stop_button
import kotlinx.android.synthetic.main.fragment_trip_tracking.track
import kotlinx.android.synthetic.main.fragment_trip_tracking.vehicle
import java.util.Currency

class TripTrackingFragment : BaseFragment() {

    private var tripDetailsObserver: com.karhoo.sdk.api.network.observable.Observer<Resource<TripInfo>>? =
            null
    private var tripDetailsObservable: Observable<TripInfo>? = null
    private lateinit var bookingRequestStateViewModel: BookingRequestStateViewModel

    private var driverPositionObserver: com.karhoo.sdk.api.network.observable.Observer<Resource<DriverTrackingInfo>>? =
            null
    private var driverTrackingInfoObservable: Observable<DriverTrackingInfo>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip_tracking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        track.setOnClickListener {
            trackTrip()
        }

        stop_button.setOnClickListener {
            unsubscribeObservers()
        }
    }

    private fun showLoading() {
        loadingProgressBar?.show()
    }

    private fun hideLoading() {
        loadingProgressBar?.hide()
    }

    private fun trackTrip() {
        unsubscribeObservers()
        bookingRequestStateViewModel.currentState.tripInfo?.let { trip ->
            observeTripInfo(tripId = trip.tripId)
            observeDriverPosition(tripIdentifier = trip.tripId)
        }
    }

    override fun onStop() {
        super.onStop()
        unsubscribeObservers()
    }

    override fun onStart() {
        super.onStart()
        trackTrip()
    }

    private fun createTripObservable(): Observer<BookingRequestStatus> {
        return Observer {
            it.tripInfo?.let { trip ->
                bindViewTripInfo(trip)
                unsubscribeObservers()
                observeTripInfo(tripId = trip.tripId)
                observeDriverPosition(tripIdentifier = trip.tripId)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindViewTripInfo(trip: TripInfo) {
        selected_pickup?.text = trip.origin?.displayAddress
        selected_dropoff?.text = trip.destination?.displayAddress

        quote_id?.text = trip.displayTripId
        fleet_name?.text = trip.fleetInfo?.name
        vehicle?.text = trip.vehicle?.driver?.firstName + " " + trip.vehicle?.driver?.lastName
        status?.text = trip.tripState.toString()
        registration_number?.text = trip.vehicle?.vehicleLicencePlate
        val selectedQuote = trip.quote
        selectedQuote?.let {
            val highPrice = selectedQuote.total
            val currency =
                    Currency.getInstance(selectedQuote.currency)
            price?.text = CurrencyUtils.intToPrice(currency, highPrice)
        }
    }

    private fun bindToBookingRequestOutputs(): Observer<BookingRequestViewContract.BookingRequestAction> {
        return Observer { actions ->
            when (actions) {
                is BookingRequestViewContract.BookingRequestAction.WaitForTripAllocation ->
                    waitForTripAllocation()
                is BookingRequestViewContract.BookingRequestAction.HandleBookingError ->
                    showErrorDialog(actions.stringId)
            }
        }
    }

    private fun showErrorDialog(stringId: Int) {
        toastErrorMessage(stringId)
    }

    private fun waitForTripAllocation() {
        Toast.makeText(context, "Waiting for Trip Allocation", Toast.LENGTH_LONG).show()
    }

    private fun observeTripInfo(tripId: String) {
        showLoading()
        tripDetailsObserver = object :
                com.karhoo.sdk.api.network.observable.Observer<Resource<TripInfo>> {
            override fun onValueChanged(value: Resource<TripInfo>) {
                when (value) {
                    is Resource.Success -> bindViewTripInfo(value.data)
                }
            }
        }

        tripDetailsObservable = KarhooApi.tripService.trackTrip(tripId).observable().apply {
            tripDetailsObserver?.let {
                subscribe(it, TRIP_INFO_UPDATE_PERIOD)
            }
        }
    }

    private fun observeDriverPosition(tripIdentifier: String) {
        showLoading()
        driverPositionObserver = object :
                com.karhoo.sdk.api.network.observable.Observer<Resource<DriverTrackingInfo>> {
            override fun onValueChanged(value: Resource<DriverTrackingInfo>) {
                when (value) {
                    is Resource.Success -> bindDriverPosition(value.data)
                }
            }
        }
        driverTrackingInfoObservable = KarhooApi.driverTrackingService.trackDriver(tripIdentifier).observable()
        driverPositionObserver?.let {
            driverTrackingInfoObservable?.subscribe(it, TRIP_INFO_UPDATE_PERIOD)
        }
    }

    private fun bindDriverPosition(data: DriverTrackingInfo) {
        latitude?.text = data.position?.latitude.toString()
        longitude?.text = data.position?.longitude.toString()
        driver_eta?.text = data.originEta.toString()
    }

    private fun unsubscribeObservers() {
        hideLoading()
        driverPositionObserver?.let {
            driverTrackingInfoObservable?.unsubscribe(it)
        }
        tripDetailsObserver?.let {
            tripDetailsObservable?.unsubscribe(it)
        }
    }

    companion object {
        const val TRIP_INFO_UPDATE_PERIOD = 30000L

        @JvmStatic
        fun newInstance(owner: FragmentActivity,
                        bookingRequestStateViewModel: BookingRequestStateViewModel) = TripTrackingFragment().apply {
            this.bookingRequestStateViewModel = bookingRequestStateViewModel
            bookingRequestStateViewModel.viewStates().observe(owner, createTripObservable())
            bookingRequestStateViewModel.viewActions().observe(owner, bindToBookingRequestOutputs())
        }
    }
}