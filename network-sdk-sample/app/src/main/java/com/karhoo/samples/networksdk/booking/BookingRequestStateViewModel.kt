package com.karhoo.samples.networksdk.booking

import android.app.Application
import androidx.annotation.StringRes
import com.karhoo.samples.networksdk.base.state.BaseStateViewModel
import com.karhoo.sdk.api.model.TripInfo

class BookingRequestStateViewModel(application: Application) :
        BaseStateViewModel<BookingRequestStatus,
                BookingRequestViewContract.BookingRequestAction,
                BookingRequestViewContract.BookingRequestEvent>(application) {
    init {
        viewState = BookingRequestStatus(null, false)
    }

    // update the state by using a set of predefined contracts. Some of the event can trigger an
    // action to be performed (e.g. output of the widget)
    override fun process(viewEvent: BookingRequestViewContract.BookingRequestEvent) {
        super.process(viewEvent)
        when (viewEvent) {
            is BookingRequestViewContract.BookingRequestEvent.BookingSuccess ->
                updateBookingRequestStatus(viewEvent.tripInfo, viewEvent.isGuest)
            is BookingRequestViewContract.BookingRequestEvent.BookingError ->
                handleBookingError(viewEvent.stringId)
        }
    }

    private fun handleBookingError(@StringRes stringId: Int) {
        viewAction = BookingRequestViewContract.BookingRequestAction.HandleBookingError(stringId)
    }

    private fun updateBookingRequestStatus(tripInfo: TripInfo, guest: Boolean) {
        viewAction = BookingRequestViewContract.BookingRequestAction.WaitForTripAllocation
        viewState = BookingRequestStatus(tripInfo, guest)
    }
}