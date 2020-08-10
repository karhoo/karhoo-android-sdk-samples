package com.karhoo.samples.networksdk.quotes

import android.app.Application
import com.karhoo.samples.networksdk.base.state.BaseStateViewModel
import com.karhoo.sdk.api.model.QuoteV2

class BookingQuoteStateViewModel(application: Application) :
        BaseStateViewModel<QuoteListStatus, BookingSupplierViewContract.BookingSupplierAction,
                BookingSupplierViewContract.BookingSupplierEvent>(application) {

    init {
        viewState = QuoteListStatus(null)
    }

    override fun process(viewEvent: BookingSupplierViewContract.BookingSupplierEvent) {
        super.process(viewEvent)
        when (viewEvent) {
            is BookingSupplierViewContract.BookingSupplierEvent.SupplierItemClicked ->
                showBookingRequest(viewEvent.quote)
            is BookingSupplierViewContract.BookingSupplierEvent.Availability -> setHideNoAvailability()
            is BookingSupplierViewContract.BookingSupplierEvent.Error ->
                setShowNoAvailability()
        }
    }

    private fun setShowNoAvailability() {
        viewAction = BookingSupplierViewContract.BookingSupplierAction.ShowError
    }

    private fun setHideNoAvailability() {
        viewAction = BookingSupplierViewContract.BookingSupplierAction.HideError
    }

    private fun showBookingRequest(selectedQuote: QuoteV2) {
        viewState = QuoteListStatus(selectedQuote)
        viewAction = BookingSupplierViewContract.BookingSupplierAction.ShowBookingRequest(selectedQuote)
    }
}