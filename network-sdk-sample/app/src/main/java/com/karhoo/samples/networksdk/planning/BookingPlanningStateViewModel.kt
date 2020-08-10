package com.karhoo.samples.networksdk.planning

import android.app.Application
import com.karhoo.samples.networksdk.base.state.BaseStateViewModel
import com.karhoo.sdk.api.model.LocationInfo

class BookingPlanningStateViewModel(application: Application) : BaseStateViewModel<BookingStatus,
        TripPlanningContract.AddressBarActions, TripPlanningContract.AddressBarEvent>(
    application
) {

    init {
        viewState = BookingStatus(null, null)
    }

    override fun process(viewEvent: TripPlanningContract.AddressBarEvent) {
        super.process(viewEvent)
        when (viewEvent) {
            is TripPlanningContract.AddressBarEvent.PickUpAddressEvent -> updatePickup(viewEvent.address)
            is TripPlanningContract.AddressBarEvent.DestinationAddressEvent -> updateDestination(
                viewEvent.address
            )
        }
    }

    private fun updatePickup(pickup: LocationInfo?) {
        viewState = BookingStatus(pickup, viewState.destination)
    }

    private fun updateDestination(destination: LocationInfo?) {
        viewState = BookingStatus(viewState.pickup, destination)
    }
}