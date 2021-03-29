package com.karhoo.samples.uisdk.components.customviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.karhoo.samples.uisdk.components.R
import com.karhoo.samples.uisdk.components.base.BaseFragment
import com.karhoo.sdk.api.datastore.user.SavedPaymentInfo
import com.karhoo.sdk.api.model.*
import com.karhoo.sdk.api.network.request.QuoteQTA
import com.karhoo.uisdk.screen.booking.booking.BookingPriceView
import com.karhoo.uisdk.screen.booking.booking.payment.BookingPaymentView
import com.karhoo.uisdk.screen.booking.booking.quotes.BookingQuotesView
import com.karhoo.uisdk.screen.booking.quotes.QuotesSortView
import com.karhoo.uisdk.util.GBP
import java.util.*

class CustomisedViewsFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customised_views, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookingPriceView: BookingPriceView = view.findViewById(R.id.bookingRequestPriceWidget)
        bookingPriceView.bindPrebook(CUSTOMISED_VIEWS_QUOTE, "15:04 25 Dec 2021", "ASAP", Currency.getInstance(GBP))

        val bookingPriceViewBold: BookingPriceView = view.findViewById(R.id.bookingRequestPriceWidgetBoldFonts)
        bookingPriceViewBold.bindPrebook(CUSTOMISED_VIEWS_QUOTE, "15:04 25 Dec 2021", "ASAP", Currency.getInstance(GBP))

        val bookingQuotesView: BookingQuotesView = view.findViewById(R.id.customBookingQuotesView)
        bookingQuotesView.bindViews(CUSTOMISED_VIEWS_QUOTE.fleet.logoUrl, CUSTOMISED_VIEWS_QUOTE.fleet.name!!, CUSTOMISED_VIEWS_QUOTE.vehicle.vehicleClass!!)
        bookingQuotesView.setCapacity(2,2)

        val bookingQuotesView2: BookingQuotesView = view.findViewById(R.id.customBookingQuotesView2)
        bookingQuotesView2.bindViews(CUSTOMISED_VIEWS_QUOTE.fleet.logoUrl, CUSTOMISED_VIEWS_QUOTE.fleet.name!!, CUSTOMISED_VIEWS_QUOTE.vehicle.vehicleClass!!)
        bookingQuotesView2.setCapacity(2,2)

        val bookingPaymentView: BookingPaymentView = view.findViewById(R.id.customBookingPaymentView)
        bookingPaymentView.bindPaymentDetails(SavedPaymentInfo("•••• 1234", CardType.VISA))

        val bookingPaymentView2: BookingPaymentView = view.findViewById(R.id.customBookingPaymentView2)
        bookingPaymentView2.bindPaymentDetails(SavedPaymentInfo("•••• 1234", CardType.VISA))

        val bookingPaymentView3: BookingPaymentView = view.findViewById(R.id.customBookingPaymentView3)
        bookingPaymentView3.bindPaymentDetails(null)

    }

    companion object {
        @JvmStatic
        fun newInstance() = CustomisedViewsFragment()

        val CUSTOMISED_VIEWS_QUOTE = Quote(id = "NTIxMjNiZDktY2M5OC00YjhkLWE5OGEtMTIyNDQ2ZDY5ZTc5O3NhbG9vbg==",
            quoteType = QuoteType.ESTIMATED,
            quoteSource = QuoteSource.FLEET,
            price = QuotePrice(currencyCode = "DEFAULT_CURRENCY",
                highPrice = 577,
                lowPrice = 577),
            fleet = FleetInfo("52123bd9-cc98-4b8d-a98a-122446d69e79",
                name = "iCabbi [Sandbox]",
                logoUrl = "https://cdn.karhoo.com/d/images/logos/52123bd9-cc98-4b8d-a98a-122446d69e79.png",
                description = "Some fleet description",
                phoneNumber = "+447904839920",
                termsConditionsUrl = "http://www.google.com"),
            pickupType = PickupType.CURBSIDE,
            vehicle = QuoteVehicle(vehicleClass = "Electric",
                vehicleQta = QuoteQTA(highMinutes = 30, lowMinutes = 1),
                luggageCapacity = 2,
                passengerCapacity = 2))
    }
}