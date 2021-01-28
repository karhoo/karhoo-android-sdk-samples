package com.karhoo.samples.networksdk.booking

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.base.model.payments.Amount
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.braintreepayments.api.models.PaymentMethodNonce
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.SampleApplication
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.booking.AdyenResultActivity.Companion.RESULT_KEY
import com.karhoo.samples.networksdk.planning.BookingPlanningStateViewModel
import com.karhoo.samples.networksdk.planning.BookingStatus
import com.karhoo.samples.networksdk.quotes.BookingQuoteStateViewModel
import com.karhoo.samples.networksdk.quotes.QuoteListStatus
import com.karhoo.samples.networksdk.utils.CurrencyUtils
import com.karhoo.samples.networksdk.utils.orZero
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooApi.userStore
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.datastore.user.SavedPaymentInfo
import com.karhoo.sdk.api.datastore.user.UserManager
import com.karhoo.sdk.api.model.*
import com.karhoo.sdk.api.model.adyen.AdyenAmount
import com.karhoo.sdk.api.network.request.AdyenPaymentMethodsRequest
import com.karhoo.sdk.api.network.request.PassengerDetails
import com.karhoo.sdk.api.network.request.Passengers
import com.karhoo.sdk.api.network.request.TripBooking
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_trip_booking.*
import org.json.JSONObject
import java.util.*

class AdyenTripBookingFragment : BaseFragment(), UserManager.OnUserPaymentChangedListener {

    private var locale: String = "GB"
    private lateinit var braintreeSDKToken: String
    private lateinit var bookingQuoteStateViewModel: BookingQuoteStateViewModel
    private lateinit var bookingPlanningStateViewModel: BookingPlanningStateViewModel
    private lateinit var bookingRequestStateViewModel: BookingRequestStateViewModel
    private var quote: Quote? = null
    private lateinit var config: KarhooSDKConfiguration
    private var adyenKey: String = ""

    private var tripId: String = ""

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
        change_button.setOnClickListener {
            changeCard()
        }
        bindAddresses(bookingPlanningStateViewModel.currentState)
        bindPassengerDetails()
    }

    override fun onResume() {
        super.onResume()
        bindPassengerDetails()
    }

    private fun showLoading() {
        loadingProgressBar?.show()
    }

    private fun hideLoading() {
        loadingProgressBar?.hide()
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

    private fun changeCard() {
        //TODO Get price
        sdkInit()
    }

    private fun bookTrip() {
        passBackThreeDSecureNonce()
    }

    private fun passBackThreeDSecureNonce() {
        when {
            tripId.isNotBlank() -> {
                threeDSecureNonce(tripId)
            }
            else -> {
                //Handle error
            }
        }
    }

    private fun threeDSecureNonce(
        paymentsNonce: String
    ) {
        showLoading()
//        bindCardDetails(
//            SavedPaymentInfo(
//                lastFour = paymentsNonce.lastFour,
//                cardType = paymentsNonce.cardType
//            )
//        )
        passBackThreeDSecuredNonce(
            paymentsNonce,
            getPassengerDetails(),
            ""
        )
    }

    private fun isGuest() = config.authenticationMethod() is AuthenticationMethod.Guest

    private fun bindCardDetails(savedPaymentInfo: SavedPaymentInfo?) {
        payment_details?.text = savedPaymentInfo?.lastFour
    }

    private fun passBackThreeDSecuredNonce(
        threeDSNonce: String, passengerDetails:
        PassengerDetails?, comments: String
    ) {
        toastErrorMessage("All good, lets book the trip")
        showLoading()

        passengerDetails?.let {
            KarhooApi.tripService.book(
                TripBooking(
                    nonce = threeDSNonce,
                    quoteId = quote?.id.orEmpty(),
                    passengers = Passengers(
                        additionalPassengers = 0,
                        passengerDetails = listOf(passengerDetails)
                    ),
                    flightNumber = null,
                    comments = comments
                )
            )
                .execute { result ->
                    hideLoading()
                    when (result) {
                        is Resource.Success -> onTripBookSuccess(result.data)
                        is Resource.Failure -> onTripBookFailure(result.error)
                    }
                }
        }
    }

    private fun onTripBookSuccess(tripInfo: TripInfo) {
        bookingRequestStateViewModel.process(
            BookingRequestViewContract.BookingRequestEvent
                .BookingSuccess(tripInfo, isGuest())
        )
    }

    private fun onTripBookFailure(error: KarhooError) {
        when (error) {
            KarhooError.CouldNotBookPaymentPreAuthFailed -> {
                toastErrorMessage("Got CouldNotBookPaymentPreAuthFailed, need to show the payment dialog")
                showPaymentDialog(braintreeSDKToken)
            }
            KarhooError.InvalidRequestPayload -> toastErrorMessage(error)
            else -> toastErrorMessage(error)
        }
    }

    private fun bindPassengerDetails() {
        if (!isGuest()) {
            val user = userStore.currentUser
            passenger_details_first_name.setText(user.firstName)
            passenger_details_last_name.setText(user.lastName)
            passenger_details_phone.setText(user.phoneNumber)
            passenger_details_email.setText(user.email)
            locale = user.locale
        }
    }

    private fun getPassengerDetails(): PassengerDetails {
        return PassengerDetails(
            firstName = passenger_details_first_name.text.toString(),
            lastName = passenger_details_last_name.text.toString(),
            phoneNumber = passenger_details_phone.text.toString(),
            email = passenger_details_email.text.toString(),
            locale = locale
        )
    }

    private fun showPaymentDialog(braintreeSDKToken: String) {
        AlertDialog.Builder(context, R.style.DialogTheme)
            .setTitle(R.string.payment_issue)
            .setMessage(R.string.payment_issue_message)
            .setPositiveButton(R.string.add_card) { dialog, _ ->
                showPaymentUI(braintreeSDKToken)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun sdkInit() {
        KarhooApi.paymentsService.getAdyenPublicKey().execute { result ->
            when (result) {
                is Resource.Success -> {
                    result.data.let {
                        adyenKey = it.publicKey
                        getPaymentMethods()
                    }
                }
                is Resource.Failure -> Log.e("Error", "Adyen public key retrieval failed")
            }
        }
    }

    private fun showPaymentUI(paymentData: String) {
        toastErrorMessage("[Adyen] Show Adyen Dialog")
        val payments = JSONObject(paymentData)
        val paymentMethods = PaymentMethodsApiResponse.SERIALIZER.deserialize(payments)
        val dropInConfiguration: DropInConfiguration = getDropInConfig(requireContext())
        DropIn.startPayment(requireContext(), paymentMethods, dropInConfiguration)
    }

    private fun getPaymentMethods() {
        val amount = AdyenAmount(
            quote?.price?.currencyCode ?: DEFAULT_CURRENCY,
            quote?.price?.highPrice.orZero()
        )

        let {
            val request = AdyenPaymentMethodsRequest(amount = amount)
            KarhooApi.paymentsService.getAdyenPaymentMethods(request).execute { result ->
                when (result) {
                    is Resource.Success -> {
                        showPaymentUI(result.data)
                    }
                    is Resource.Failure -> Log.e("Error", "Adyen payment method retrieval failed")
                }
            }
        }
    }

    private fun getDropInConfig(context: Context): DropInConfiguration {
        val amount = Amount()
        amount.currency = quote?.price?.currencyCode ?: DEFAULT_CURRENCY
        amount.value = quote?.price?.highPrice.orZero()

        val environment = Environment.TEST

        val dropInIntent = Intent(context, AdyenResultActivity::class.java).apply {
            putExtra(AdyenResultActivity.TYPE_KEY, "drop-in")
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        }

        return DropInConfiguration.Builder(
            context, dropInIntent,
            AdyenDropInService::class.java
        )
            .setAmount(amount)
            .setEnvironment(environment)
            .setShopperLocale(Locale.getDefault())
            .addCardConfiguration(createCardConfig(context, adyenKey))
            .build()
    }

    private fun createCardConfig(context: Context, publicKey: String): CardConfiguration {
        return CardConfiguration.Builder(context, publicKey)
            .setShopperLocale(Locale.getDefault())
            .setHolderNameRequire(true)
            .setShowStorePaymentField(true)
            .build()
    }

    fun onAdyenActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        hideLoading()
        if (resultCode == AppCompatActivity.RESULT_OK && data == null) {
            //Handle error
        } else if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val dataString = data.getStringExtra(RESULT_KEY)
            val payload = JSONObject(dataString)
            when (payload.optString(RESULT_CODE, "")) {
                AUTHORISED -> {
                    this.tripId = payload.optString(AdyenDropInService.TRIP_ID, "")
                    updateCardDetails(paymentData = payload.optString(ADDITIONAL_DATA, null))
                }
                else -> Log.e("Error", "Adyen payment error")
            }
        } else {
            //Refresh screen
        }
    }

    private fun quotePriceToAmount(price: QuotePrice?): String {
        val currency = Currency.getInstance(price?.currencyCode?.trim())
        return CurrencyUtils.intToPriceNoSymbol(currency, price?.highPrice.orZero())
    }

    private fun convertToPaymentsNonce(paymentMethodNonce: PaymentMethodNonce): PaymentsNonce? {
        return PaymentsNonce(
            paymentMethodNonce.nonce,
            CardType.fromString(paymentMethodNonce.typeLabel),
            paymentMethodNonce.description
        )
    }

    private fun updateCardDetails(paymentData: String?) {
        paymentData?.let {
            val additionalData = JSONObject(paymentData)
            val newCardNumber = additionalData.optString(CARD_SUMMARY, "")
            val type = additionalData.optString(PAYMENT_METHOD, "")
            val savedPaymentInfo = CardType.fromString(type)?.let {
                SavedPaymentInfo(newCardNumber, it)
            } ?: SavedPaymentInfo(newCardNumber, CardType.NOT_SET)
            userStore.savedPaymentInfo = savedPaymentInfo
        }
    }

    override fun onSavedPaymentInfoChanged(userPaymentInfo: SavedPaymentInfo?) {
        toastErrorMessage("Payment Info Changed")
        bindCardDetails(userPaymentInfo)
    }

    companion object {
        const val ADDITIONAL_DATA = "additionalData"
        const val AUTHORISED = "Authorised"
        const val CARD_SUMMARY = "cardSummary"
        const val DEFAULT_CURRENCY = "GBP"
        const val PAYMENT_METHOD = "paymentMethod"
        const val RESULT_CODE = "resultCode"
        const val REQ_CODE_ADYEN = DropIn.DROP_IN_REQUEST_CODE

        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingPlanningStateViewModel: BookingPlanningStateViewModel,
            bookingQuoteStateViewModel: BookingQuoteStateViewModel,
            bookingRequestStateViewModel: BookingRequestStateViewModel
        ) = AdyenTripBookingFragment().apply {
            this.bookingPlanningStateViewModel = bookingPlanningStateViewModel
            this.bookingQuoteStateViewModel = bookingQuoteStateViewModel
            this.bookingRequestStateViewModel = bookingRequestStateViewModel
            bookingPlanningStateViewModel.viewStates().observe(owner, createPlanningObservable())
            bookingQuoteStateViewModel.viewStates().observe(owner, createQuoteObservable())
        }
    }
}