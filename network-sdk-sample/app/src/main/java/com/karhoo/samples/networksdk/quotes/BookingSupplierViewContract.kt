package com.karhoo.uisdk.screen.booking.booking.supplier

import com.karhoo.sdk.api.model.QuoteV2

interface BookingSupplierViewContract {
    sealed class BookingSupplierEvent {
        data class SupplierItemClicked(val quote: QuoteV2) : BookingSupplierEvent()
        object Availability : BookingSupplierEvent()
        object Error : BookingSupplierEvent()
    }

    sealed class BookingSupplierAction {
        object HideError : BookingSupplierAction()
        object ShowError : BookingSupplierAction()
        data class ShowBookingRequest(val quote: QuoteV2) : BookingSupplierAction()
    }
}