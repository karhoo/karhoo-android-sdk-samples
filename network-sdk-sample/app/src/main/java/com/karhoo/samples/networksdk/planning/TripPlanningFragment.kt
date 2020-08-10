package com.karhoo.samples.networksdk.planning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.model.LocationInfo
import com.karhoo.sdk.api.model.Place
import com.karhoo.sdk.api.model.Places
import com.karhoo.sdk.api.model.Position
import com.karhoo.sdk.api.network.request.LocationInfoRequest
import com.karhoo.sdk.api.network.request.PlaceSearch
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_trip_planning.*
import java.util.*

class TripPlanningFragment : BaseFragment() {

    private var sessionToken: String = ""
    var pickUpLocationInfo: LocationInfo? = null
    var dropOffLocationInfo: LocationInfo? = null
    private lateinit var bookingPlanningStateViewModel: BookingPlanningStateViewModel

    fun getSessionToken(): String {
        if (sessionToken.isEmpty()) {
            sessionToken = UUID.randomUUID().toString()
        }
        return sessionToken
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_planning, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pickup_button.setOnClickListener {
            val placeSearch = setSearchQuery(origin_text.text.toString())
            requestAddresses(placeSearch, BookingPlanningContract.AddressType.ORIGIN)
        }

        dropoff_button.setOnClickListener {
            val placeSearch = setSearchQuery(destination_text.text.toString())
            requestAddresses(placeSearch, BookingPlanningContract.AddressType.DESTINATION)
        }

    }

    private fun showLoading() {
        loadingProgressBar?.show()
    }

    private fun hideLoading() {
        loadingProgressBar?.hide()
    }

    private fun bindToAddressBarOutputs(): Observer<BookingPlanningContract.AddressBarActions> {
        return Observer { actions ->
            when (actions) {
                is BookingPlanningContract.AddressBarActions.UpdateAddressLabel -> {
                    if (actions.addressType == BookingPlanningContract.AddressType.ORIGIN) {
                        selected_pickup.text = actions.locationInfo?.displayAddress
                    } else {
                        selected_dropoff.text = actions.locationInfo?.displayAddress
                    }
                }
            }
        }
    }

    private fun setSearchQuery(searchQuery: String): PlaceSearch {
        return PlaceSearch(
            position = Position(
                latitude = 0.0,
                longitude = 0.0
            ),
            query = searchQuery,
            sessionToken = getSessionToken()
        )
    }

    private fun setLocationInfoRequestQuery(placeId: String): LocationInfoRequest {
        return LocationInfoRequest(
            placeId = placeId,
            sessionToken = getSessionToken()
        )
    }

    private fun requestAddresses(placeSearch: PlaceSearch, type: BookingPlanningContract.AddressType) {
        if (placeSearch.query.length > 3) {
            showLoading()
            KarhooApi.addressService.placeSearch(placeSearch).execute { result ->
                hideLoading()
                when (result) {
                    is Resource.Success -> updatePlaces(placeSearch, result.data, type)
                    is Resource.Failure -> toastErrorMessage(result.error)
                }
            }
        }
    }

    private fun requestLocationInfo(place: Place, type: BookingPlanningContract.AddressType) {
        val locationInfoRequest = setLocationInfoRequestQuery(place.placeId)
        showLoading()
        KarhooApi.addressService.locationInfo(locationInfoRequest).execute { result ->
            hideLoading()
            when (result) {
                is Resource.Success -> updatePlace(result.data, type)
                is Resource.Failure -> toastErrorMessage(result.error)
            }
        }
    }

    private fun updatePlace(data: LocationInfo, type: BookingPlanningContract.AddressType) {
        if (type == BookingPlanningContract.AddressType.ORIGIN) {
            bookingPlanningStateViewModel.process(
                BookingPlanningContract.AddressBarEvent.PickUpAddressEvent(
                    data
                )
            )
            pickUpLocationInfo = data
            selected_pickup.text = data.displayAddress
        } else {
            bookingPlanningStateViewModel.process(
                BookingPlanningContract.AddressBarEvent.DestinationAddressEvent(
                    data
                )
            )
            dropOffLocationInfo = data
            selected_dropoff.text = data.displayAddress
        }
    }

    private fun updatePlaces(
        placeSearch: PlaceSearch,
        data: Places,
        type: BookingPlanningContract.AddressType
    ) {
        val builderSingle: AlertDialog.Builder = AlertDialog.Builder(context!!)
        builderSingle.setIcon(android.R.drawable.ic_menu_compass)
        builderSingle.setTitle("Select One Place: " + placeSearch.query)

        val arrayAdapter =
            ArrayAdapter<String>(context!!, android.R.layout.select_dialog_item)
        for (place: Place in data.locations) {
            arrayAdapter.add(place.displayAddress)
        }

        builderSingle.setNegativeButton("cancel") { dialog, _ -> dialog.dismiss() }

        builderSingle.setAdapter(arrayAdapter) { _, which ->
            val place = data.locations[which]
            requestLocationInfo(place, type)
        }
        builderSingle.show()
    }


    companion object {
        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingPlanningStateViewModel: BookingPlanningStateViewModel
        ): TripPlanningFragment = TripPlanningFragment().apply {
            this.bookingPlanningStateViewModel = bookingPlanningStateViewModel
            bookingPlanningStateViewModel.viewActions().observe(owner, bindToAddressBarOutputs())
        }
    }
}