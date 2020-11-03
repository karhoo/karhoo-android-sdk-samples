package com.karhoo.samples.networksdk.planning

import com.karhoo.sdk.api.model.LocationInfo

interface BookingPlanningContract {
    sealed class AddressBarEvent {

        data class PickUpAddressEvent(val address: LocationInfo?) : AddressBarEvent()

        data class DestinationAddressEvent(val address: LocationInfo?) : AddressBarEvent()

    }

    sealed class AddressBarActions {

        data class UpdateAddressLabel(
                val locationInfo: LocationInfo?,
                val addressType: AddressType
                                     ) : AddressBarActions()

    }

    enum class AddressType {
        ORIGIN,
        DESTINATION
    }
}
