package com.karhoo.samples.networksdk.quotes

import com.karhoo.sdk.api.model.Quote

interface BookingSupplierViewContract {
    sealed class BookingSupplierEvent {
        data class SupplierItemClicked(val quote: Quote) : BookingSupplierEvent()
        object Availability : BookingSupplierEvent()
        object Error : BookingSupplierEvent()
    }

    sealed class BookingSupplierAction {
        object HideError : BookingSupplierAction()
        object ShowError : BookingSupplierAction()
        data class ShowBookingRequest(val quote: Quote) : BookingSupplierAction()
    }
}