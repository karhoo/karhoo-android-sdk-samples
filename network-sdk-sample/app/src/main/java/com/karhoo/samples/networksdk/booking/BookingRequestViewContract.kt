package com.karhoo.samples.networksdk.booking

import androidx.annotation.StringRes
import com.karhoo.sdk.api.model.TripInfo

interface BookingRequestViewContract {

    sealed class BookingRequestEvent {
        data class BookingSuccess(val tripInfo: TripInfo, val isGuest: Boolean) :
                BookingRequestEvent()

        data class BookingError(@StringRes val stringId: Int) : BookingRequestEvent()
    }

    sealed class BookingRequestAction {
        object WaitForTripAllocation : BookingRequestAction()
        data class HandleBookingError(@StringRes val stringId: Int) : BookingRequestAction()
    }
}