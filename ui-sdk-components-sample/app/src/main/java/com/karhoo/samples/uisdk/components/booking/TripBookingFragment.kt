package com.karhoo.samples.uisdk.components.booking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.karhoo.samples.uisdk.components.R
import com.karhoo.samples.uisdk.components.base.BaseFragment
import com.karhoo.sdk.api.model.Quote
import com.karhoo.uisdk.screen.booking.booking.payment.adyen.AdyenPaymentView
import com.karhoo.uisdk.screen.booking.booking.supplier.BookingSupplierViewModel
import com.karhoo.uisdk.screen.booking.booking.supplier.QuoteListStatus
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatusStateViewModel
import com.karhoo.uisdk.screen.booking.domain.bookingrequest.BookingRequestStateViewModel
import kotlinx.android.synthetic.main.fragment_trip_booking.*

class TripBookingFragment : BaseFragment() {

    private lateinit var bookingSupplierViewModel: BookingSupplierViewModel
    private lateinit var bookingStatusStateViewModel: BookingStatusStateViewModel
    private lateinit var bookingRequestStateViewModel: BookingRequestStateViewModel
    private var quote: Quote? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews()
    }

    private fun bindViews() {
        booking_request_widget.apply {
            bindViewToBookingStatus(requireActivity(), bookingStatusStateViewModel)
            bindViewToBookingRequest(requireActivity(), bookingRequestStateViewModel)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_BRAINTREE || requestCode == REQ_CODE_BRAINTREE_GUEST || requestCode == AdyenPaymentView.REQ_CODE_ADYEN
        ) {
            booking_request_widget.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun createQuoteObservable(): Observer<QuoteListStatus> {
        return Observer { it ->
            it.let { quote ->
                val selectedQuote = quote.selectedQuote
                this.quote = selectedQuote
                this.quote?.let {
                    booking_request_widget?.showBookingRequest(it, null)
                }
            }
        }
    }

    companion object {
        const val REQ_CODE_BRAINTREE = 301
        const val REQ_CODE_BRAINTREE_GUEST = 302

        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingStatusStateViewModel: BookingStatusStateViewModel,
            bookingSupplierViewModel: BookingSupplierViewModel,
            bookingRequestStateViewModel: BookingRequestStateViewModel
        ) = TripBookingFragment().apply {
            this.bookingStatusStateViewModel = bookingStatusStateViewModel
            this.bookingSupplierViewModel = bookingSupplierViewModel
            this.bookingRequestStateViewModel = bookingRequestStateViewModel
            bookingSupplierViewModel.viewStates().observe(owner, createQuoteObservable())
        }
    }
}