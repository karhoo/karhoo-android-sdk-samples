package com.karhoo.samples.networksdk.booking

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.braintreepayments.api.BraintreeFragment
import com.braintreepayments.api.ThreeDSecure
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.braintreepayments.api.interfaces.BraintreeErrorListener
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener
import com.braintreepayments.api.models.PaymentMethodNonce
import com.braintreepayments.api.models.ThreeDSecureRequest
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.SampleApplication
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.planning.BookingPlanningStateViewModel
import com.karhoo.samples.networksdk.planning.BookingStatus
import com.karhoo.samples.networksdk.quotes.BookingQuoteStateViewModel
import com.karhoo.samples.networksdk.quotes.QuoteListStatus
import com.karhoo.samples.networksdk.utils.CurrencyUtils
import com.karhoo.samples.networksdk.utils.orZero
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.datastore.user.SavedPaymentInfo
import com.karhoo.sdk.api.datastore.user.UserManager
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.sdk.api.model.CardType
import com.karhoo.sdk.api.model.PaymentsNonce
import com.karhoo.sdk.api.model.Quote
import com.karhoo.sdk.api.model.TripInfo
import com.karhoo.sdk.api.model.UserInfo
import com.karhoo.sdk.api.network.request.AddPaymentRequest
import com.karhoo.sdk.api.network.request.NonceRequest
import com.karhoo.sdk.api.network.request.PassengerDetails
import com.karhoo.sdk.api.network.request.Passengers
import com.karhoo.sdk.api.network.request.Payer
import com.karhoo.sdk.api.network.request.SDKInitRequest
import com.karhoo.sdk.api.network.request.TripBooking
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_trip_booking.book_button
import kotlinx.android.synthetic.main.fragment_trip_booking.change_button
import kotlinx.android.synthetic.main.fragment_trip_booking.fleet_name
import kotlinx.android.synthetic.main.fragment_trip_booking.loadingProgressBar
import kotlinx.android.synthetic.main.fragment_trip_booking.passenger_details_email
import kotlinx.android.synthetic.main.fragment_trip_booking.passenger_details_first_name
import kotlinx.android.synthetic.main.fragment_trip_booking.passenger_details_last_name
import kotlinx.android.synthetic.main.fragment_trip_booking.passenger_details_phone
import kotlinx.android.synthetic.main.fragment_trip_booking.payment_details
import kotlinx.android.synthetic.main.fragment_trip_booking.price
import kotlinx.android.synthetic.main.fragment_trip_booking.quote_id
import kotlinx.android.synthetic.main.fragment_trip_booking.selected_dropoff
import kotlinx.android.synthetic.main.fragment_trip_booking.selected_pickup
import java.util.Currency

class BraintreeTripBookingFragment : BaseFragment(), UserManager.OnUserPaymentChangedListener {

    private var locale: String = "GB"
    private lateinit var braintreeSDKToken: String
    private lateinit var bookingQuoteStateViewModel: BookingQuoteStateViewModel
    private lateinit var bookingPlanningStateViewModel: BookingPlanningStateViewModel
    private lateinit var bookingRequestStateViewModel: BookingRequestStateViewModel
    private var quote: Quote? = null
    private lateinit var config: KarhooSDKConfiguration
    private var paymentsNonce: PaymentsNonce? = null

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
        bookingPlanningStateViewModel.currentState.let {
            bindAddresses(it)
        }
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
        sdkInitFor(::handleChangeCard)
    }

    private fun bookTrip() {
        sdkInitFor(::getNonce)
    }

    private fun sdkInitFor(func: (braintreeSDKToken: String) -> Unit) {
        toastErrorMessage("Initialise Payment SDK (client-token)")
        showLoading()
        val organisationId = getOrganisationId()
        val sdkInitRequest = SDKInitRequest(
            organisationId = organisationId,
            currency = quote?.price?.currencyCode.orEmpty()
        )
        KarhooApi.paymentsService.initialisePaymentSDK(sdkInitRequest).execute { result ->
            hideLoading()
            when (result) {
                is Resource.Success -> func(result.data.token)
                is Resource.Failure -> toastErrorMessage(result.error)
            }
        }
    }

    private fun getOrganisationId() =
        if (isGuest()) {
            (config.authenticationMethod() as AuthenticationMethod.Guest).organisationId
        } else {
            KarhooApi.userStore.currentUser.organisations.first().id
        }

    private fun handleChangeCard(braintreeSDKToken: String) {
        this.braintreeSDKToken = braintreeSDKToken
        val dropInRequest: DropInRequest = DropInRequest().clientToken(braintreeSDKToken)
        val requestCode = if (isGuest()) REQ_CODE_BRAINTREE_GUEST else REQ_CODE_BRAINTREE
        (context as Activity).startActivityForResult(dropInRequest.getIntent(context), requestCode)
    }

    private fun getNonce(braintreeSDKToken: String) {
        toastErrorMessage("Get Nonce (get payment method)")
        if (isGuest()) {
            paymentsNonce?.let {
                threeDSecureNonce(
                    braintreeSDKToken,
                    it,
                    quotePriceToAmount(quote)
                )
            }
        } else {
            val user = KarhooApi.userStore.currentUser
            val nonceRequest = NonceRequest(
                payer = getPayerDetails(user),
                organisationId = user.organisations.first().id
            )
            showLoading()
            KarhooApi.paymentsService.getNonce(nonceRequest).execute { result ->
                hideLoading()
                when (result) {
                    is Resource.Success -> threeDSecureNonce(
                        braintreeSDKToken,
                        result.data,
                        quotePriceToAmount(quote)
                    )
                    is Resource.Failure -> showPaymentDialog(braintreeSDKToken)
                }
            }
        }
    }

    private fun threeDSecureNonce(
        braintreeSDKToken: String,
        paymentsNonce: PaymentsNonce,
        amount: String
    ) {
        showLoading()
        bindCardDetails(
            SavedPaymentInfo(
                lastFour = paymentsNonce.lastFour,
                cardType = paymentsNonce.cardType
            )
        )

        val braintreeFragment = BraintreeFragment
            .newInstance(context as AppCompatActivity, braintreeSDKToken)

        braintreeFragment.addListener(object : PaymentMethodNonceCreatedListener {
            override fun onPaymentMethodNonceCreated(paymentMethodNonce: PaymentMethodNonce?) {
                hideLoading()
                toastErrorMessage("[Braintree] Received Payment Method Nonce from Braintree")
                passBackThreeDSecuredNonce(
                    paymentMethodNonce,
                    getPassengerDetails(),
                    ""
                )
            }
        })

        braintreeFragment.addListener(
            object : BraintreeErrorListener {
                override fun onError(error: Exception?) {
                    hideLoading()
                    toastErrorMessage("[Braintree] Received Error from Braintree")
                    showPaymentDialog(braintreeSDKToken)
                }
            })

        val threeDSecureRequest = ThreeDSecureRequest()
            .nonce(paymentsNonce.nonce)
            .amount(amount)
            .versionRequested(ThreeDSecureRequest.VERSION_2)

        ThreeDSecure.performVerification(braintreeFragment, threeDSecureRequest)
        { request, lookup ->
            toastErrorMessage("[Braintree] Continues the 3DS verification")
            ThreeDSecure.continuePerformVerification(braintreeFragment, request, lookup)
        }
    }

    private fun passBackBraintreeSDKNonce(braintreeSDKNonce: String) {
        showLoading()
        toastErrorMessage("Add Payment Method")

        val user = KarhooApi.userStore.currentUser
        val addPaymentRequest = AddPaymentRequest(
            payer = getPayerDetails(user),
            organisationId = user.organisations.first().id,
            nonce = braintreeSDKNonce
        )

        KarhooApi.paymentsService.addPaymentMethod(addPaymentRequest).execute { result ->
            hideLoading()
            when (result) {
                is Resource.Success -> bindCardDetails(KarhooApi.userStore.savedPaymentInfo)
                is Resource.Failure -> toastErrorMessage(
                    result.error
                )
            }
        }
    }

    private fun getPayerDetails(user: UserInfo): Payer =
        Payer(
            id = user.userId,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName
        )

    private fun isGuest() = config.authenticationMethod() is AuthenticationMethod.Guest

    private fun bindCardDetails(savedPaymentInfo: SavedPaymentInfo?) {
        payment_details?.text = savedPaymentInfo?.lastFour
    }

    fun passBackThreeDSecuredNonce(
        threeDSNonce: PaymentMethodNonce?, passengerDetails:
        PassengerDetails?, comments: String
    ) {
        toastErrorMessage("All good, lets book the trip")
        showLoading()
        val nonce = threeDSNonce?.nonce.orEmpty()

        passengerDetails?.let {
            KarhooApi.tripService.book(
                TripBooking(
                    nonce = nonce,
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
            val user = KarhooApi.userStore.currentUser
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

    fun showPaymentDialog(braintreeSDKToken: String) {
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

    private fun showPaymentUI(braintreeSDKToken: String) {
        toastErrorMessage("[Braintree] Show Braintree Dialog")
        val dropInRequest = DropInRequest().clientToken(braintreeSDKToken)
        this.braintreeSDKToken = braintreeSDKToken
        (context as Activity).startActivityForResult(
            dropInRequest.getIntent(context),
            REQ_CODE_BRAINTREE
        )
    }

    fun onBraintreeActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        hideLoading()
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val braintreeResult =
                data.getParcelableExtra<DropInResult>(DropInResult.EXTRA_DROP_IN_RESULT)
            when (requestCode) {
                REQ_CODE_BRAINTREE -> {
                    toastErrorMessage("Received confirmation from Braintree")
                    passBackBraintreeSDKNonce(braintreeSDKToken)
                }
                REQ_CODE_BRAINTREE_GUEST -> {
                    braintreeResult.let { dropInResult ->
                        dropInResult?.paymentMethodNonce?.let {
                            this.paymentsNonce = convertToPaymentsNonce(it)
                            payment_details?.text = it.description
                        }
                    }
                }
            }
        }
    }

    private fun quotePriceToAmount(quote: Quote?): String {
        val currency = Currency.getInstance(quote?.price?.currencyCode?.trim())
        return CurrencyUtils.intToPriceNoSymbol(currency, quote?.price?.highPrice.orZero())
    }

    private fun convertToPaymentsNonce(paymentMethodNonce: PaymentMethodNonce): PaymentsNonce? {
        return PaymentsNonce(
            paymentMethodNonce.nonce,
            CardType.fromString(paymentMethodNonce.typeLabel),
            paymentMethodNonce.description
        )
    }

    override fun onSavedPaymentInfoChanged(userPaymentInfo: SavedPaymentInfo?) {
        toastErrorMessage("Payment Info Changed")
        bindCardDetails(userPaymentInfo)
    }

    companion object {
        const val REQ_CODE_BRAINTREE = 301
        const val REQ_CODE_BRAINTREE_GUEST = 302

        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingPlanningStateViewModel: BookingPlanningStateViewModel,
            bookingQuoteStateViewModel: BookingQuoteStateViewModel,
            bookingRequestStateViewModel: BookingRequestStateViewModel
        ) = BraintreeTripBookingFragment().apply {
            this.bookingPlanningStateViewModel = bookingPlanningStateViewModel
            this.bookingQuoteStateViewModel = bookingQuoteStateViewModel
            this.bookingRequestStateViewModel = bookingRequestStateViewModel
            bookingPlanningStateViewModel.viewStates().observe(owner, createPlanningObservable())
            bookingQuoteStateViewModel.viewStates().observe(owner, createQuoteObservable())
        }
    }
}