package com.karhoo.samples.networksdk.booking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.SampleApplication
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.planning.BookingPlanningStateViewModel
import com.karhoo.samples.networksdk.planning.BookingStatus
import com.karhoo.samples.networksdk.quotes.BookingQuoteStateViewModel
import com.karhoo.samples.networksdk.quotes.QuoteListStatus
import com.karhoo.samples.networksdk.utils.CurrencyUtils
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.sdk.api.model.Quote
import com.karhoo.sdk.api.model.TripInfo
import com.karhoo.sdk.api.network.request.PassengerDetails
import com.karhoo.uisdk.screen.booking.checkout.CheckoutActivity
import com.karhoo.uisdk.screen.booking.domain.address.BookingInfo
import com.karhoo.uisdk.screen.booking.domain.address.JourneyDetails
import kotlinx.android.synthetic.main.fragment_trip_booking.*
import org.joda.time.DateTime
import java.util.*

class TripBookingFragment : BaseFragment() {

    private var locale: String = "GB"
    private lateinit var bookingQuoteStateViewModel: BookingQuoteStateViewModel
    private lateinit var bookingPlanningStateViewModel: BookingPlanningStateViewModel
    private lateinit var bookingRequestStateViewModel: BookingRequestStateViewModel
    private var quote: Quote? = null
    private lateinit var config: KarhooSDKConfiguration

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        config = (requireContext().applicationContext as SampleApplication).karhooConfig
        book_button.setOnClickListener {
            bookTrip()
        }
        bookingPlanningStateViewModel.currentState.let {
            bindAddresses(it)
        }
        bindPassengerDetails()
    }

    override fun onResume() {
        super.onResume()
        bindPassengerDetails()
    }

    private fun createQuoteObservable(): Observer<QuoteListStatus> {
        return Observer {
            it?.let { quote ->
                val selectedQuote = quote.selectedQuote
                this.quote = selectedQuote
                selectedQuote?.let {
                    val highPrice = selectedQuote.price.highPrice
                    val lowPrice = selectedQuote.price.lowPrice
                    val currency =
                        selectedQuote.price.currencyCode.run { Currency.getInstance(selectedQuote.price.currencyCode) }
                            ?: Currency.getInstance("GBP")
                    price?.text = CurrencyUtils.intToRangedPrice(currency, lowPrice, highPrice)
                    quote_id?.text = selectedQuote.id
                    fleet_name?.text = selectedQuote.fleet.name
                }
            }
        }
    }

    private fun createPlanningObservable(): Observer<BookingStatus> {
        return Observer {
            it?.let { bookingStatus ->
                bindAddresses(bookingStatus)
            }
        }
    }

    private fun bindAddresses(bookingStatus: BookingStatus) {
        selected_pickup?.text = bookingStatus.pickup?.displayAddress
        selected_dropoff?.text = bookingStatus.destination?.displayAddress
    }

    private fun bookTrip() {
        val builder = CheckoutActivity.Builder()
            .quote(quote!!)
            .journeyDetails(
                JourneyDetails(
                    bookingPlanningStateViewModel.currentState.pickup,
                    bookingPlanningStateViewModel.currentState.destination,
                    DateTime()
                )
            )

        getPassengerDetails()?.let {
            builder.passengerDetails(it)
        }

        startActivityForResult(
            context?.let { builder.build(it) },
            REQ_CODE_BOOKING_REQUEST_ACTIVITY
        )

    }

    private val REQ_CODE_BOOKING_REQUEST_ACTIVITY = 304

    private fun isGuest() = config.authenticationMethod() is AuthenticationMethod.Guest

    private fun onTripBookSuccess(data: Intent?) {
        val tripInfo =
            data?.getParcelableExtra<TripInfo>(CheckoutActivity.BOOKING_CHECKOUT_PREBOOK_TRIP_INFO_KEY)
        tripInfo?.let {
            bookingRequestStateViewModel.process(
                BookingRequestViewContract.BookingRequestEvent
                    .BookingSuccess(tripInfo, isGuest())
            )
        }
    }

    private fun onTripBookFailure(data: Intent?) {
        val error =
            data?.extras?.get(CheckoutActivity.BOOKING_CHECKOUT_PREBOOK_TRIP_INFO_KEY) as KarhooError?
        error?.let {
            when (it) {
                KarhooError.CouldNotBookPaymentPreAuthFailed -> {
                    toastErrorMessage("Got CouldNotBookPaymentPreAuthFailed try again")
                    bookTrip()
                }
                else -> toastErrorMessage(it)
            }
        }
    }

    private fun bindPassengerDetails() {
        if (!isGuest()) {
            val user = KarhooApi.userStore.currentUser
            passenger_details_first_name.setText(user.firstName)
            passenger_details_last_name.setText(user.lastName)
            passenger_details_phone.setText(user.phoneNumber)
            passenger_details_email.setText(user.email)
            locale = user.locale
        }
    }

    private fun isPassengerDetailsValid(): Boolean {
        return !(passenger_details_first_name.text.isEmpty()
                && passenger_details_last_name.text.isEmpty()
                && passenger_details_email.text.isEmpty())
    }

    private fun getPassengerDetails(): PassengerDetails? {
        return if (isPassengerDetailsValid()) {
            PassengerDetails(
                firstName = passenger_details_first_name.text.toString(),
                lastName = passenger_details_last_name.text.toString(),
                phoneNumber = passenger_details_phone.text.toString(),
                email = passenger_details_email.text.toString(),
                locale = locale
            )
        } else {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE_BOOKING_REQUEST_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                onTripBookSuccess(data)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (data?.hasExtra(CheckoutActivity.BOOKING_CHECKOUT_ERROR_DATA) == true) {
                    onTripBookFailure(data)
                }
            } else {

            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingPlanningStateViewModel: BookingPlanningStateViewModel,
            bookingQuoteStateViewModel: BookingQuoteStateViewModel,
            bookingRequestStateViewModel: BookingRequestStateViewModel
        ) = TripBookingFragment().apply {
            this.bookingPlanningStateViewModel = bookingPlanningStateViewModel
            this.bookingQuoteStateViewModel = bookingQuoteStateViewModel
            this.bookingRequestStateViewModel = bookingRequestStateViewModel
            bookingPlanningStateViewModel.viewStates().observe(owner, createPlanningObservable())
            bookingQuoteStateViewModel.viewStates().observe(owner, createQuoteObservable())
        }
    }
}