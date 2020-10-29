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
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.planning.BookingPlanningStateViewModel
import com.karhoo.samples.networksdk.planning.BookingStatus
import com.karhoo.samples.networksdk.quotes.BookingQuoteStateViewModel
import com.karhoo.samples.networksdk.quotes.QuoteListStatus
import com.karhoo.samples.networksdk.utils.CurrencyUtils
import com.karhoo.samples.networksdk.utils.orZero
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.datastore.user.SavedPaymentInfo
import com.karhoo.sdk.api.datastore.user.UserManager
import com.karhoo.sdk.api.model.PaymentsNonce
import com.karhoo.sdk.api.model.Quote
import com.karhoo.sdk.api.model.TripInfo
import com.karhoo.sdk.api.network.request.AddPaymentRequest
import com.karhoo.sdk.api.network.request.NonceRequest
import com.karhoo.sdk.api.network.request.PassengerDetails
import com.karhoo.sdk.api.network.request.Passengers
import com.karhoo.sdk.api.network.request.Payer
import com.karhoo.sdk.api.network.request.SDKInitRequest
import com.karhoo.sdk.api.network.request.TripBooking
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_trip_booking.book_button
import kotlinx.android.synthetic.main.fragment_trip_booking.fleet_name
import kotlinx.android.synthetic.main.fragment_trip_booking.loadingProgressBar
import kotlinx.android.synthetic.main.fragment_trip_booking.payment_details
import kotlinx.android.synthetic.main.fragment_trip_booking.price
import kotlinx.android.synthetic.main.fragment_trip_booking.quote_id
import kotlinx.android.synthetic.main.fragment_trip_booking.selected_dropoff
import kotlinx.android.synthetic.main.fragment_trip_booking.selected_pickup
import java.util.Currency

class TripBookingFragment : BaseFragment(), UserManager.OnUserPaymentChangedListener {

    private lateinit var braintreeSDKToken: String
    private lateinit var bookingQuoteStateViewModel: BookingQuoteStateViewModel
    private lateinit var bookingPlanningStateViewModel: BookingPlanningStateViewModel
    private lateinit var bookingRequestStateViewModel: BookingRequestStateViewModel
    private var quote: Quote? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trip_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        book_button.setOnClickListener {
            bookTrip()
        }
        bookingPlanningStateViewModel.currentState?.let {
            bindAddresses(it)
        }
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

    private fun bookTrip() {
        sdkInit()
    }

    private fun sdkInit() {
        toastErrorMessage("Initialise Payment SDK (client-token)")
        showLoading()
        val sdkInitRequest = SDKInitRequest(organisationId = KarhooApi.userStore.currentUser.organisations.first().id,
                                            currency = quote?.price?.currencyCode.orEmpty())
        KarhooApi.paymentsService.initialisePaymentSDK(sdkInitRequest).execute { result ->
            hideLoading()
            when (result) {
                is Resource.Success -> getNonce(result.data.token)
                is Resource.Failure -> toastErrorMessage(result.error)
            }
        }
    }

    private fun getNonce(braintreeSDKToken: String) {
        toastErrorMessage("Get Nonce (get payment method)")

        this.braintreeSDKToken = braintreeSDKToken
        val user = KarhooApi.userStore.currentUser
        val nonceRequest = NonceRequest(
                payer = Payer(id = user.userId,
                              email = user.email,
                              firstName = user.firstName,
                              lastName = user.lastName),
                organisationId = user.organisations.first().id
                                       )
        showLoading()
        KarhooApi.paymentsService.getNonce(nonceRequest).execute { result ->
            hideLoading()
            when (result) {
                is Resource.Success -> threeDSecureNonce(braintreeSDKToken,
                                                         result.data,
                                                         quotePriceToAmount(quote))
                is Resource.Failure -> showPaymentDialog(braintreeSDKToken)
            }
        }
    }

    fun threeDSecureNonce(braintreeSDKToken: String, paymentsNonce: PaymentsNonce, amount: String) {
        showLoading()
        bindCardDetails(SavedPaymentInfo(lastFour = paymentsNonce.lastFour,
                                         cardType = paymentsNonce.cardType))

        val braintreeFragment = BraintreeFragment
                .newInstance(context as AppCompatActivity, braintreeSDKToken)

        braintreeFragment.addListener(object : PaymentMethodNonceCreatedListener {
            override fun onPaymentMethodNonceCreated(paymentMethodNonce: PaymentMethodNonce?) {
                hideLoading()
                toastErrorMessage("[Braintree] Received Payment Method Nonce from Braintree")
                passBackThreeDSecuredNonce(paymentMethodNonce,
                                           getPassengerDetails(),
                                           "")
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

    fun passBackBraintreeSDKNonce(braintreeSDKNonce: String) {
        showLoading()
        toastErrorMessage("Add Payment Method")

        val user = KarhooApi.userStore.currentUser
        val addPaymentRequest = AddPaymentRequest(
                payer = Payer(id = user.userId,
                              email = user.email,
                              firstName = user.firstName,
                              lastName = user.lastName),
                organisationId = user.organisations.first().id,
                nonce = braintreeSDKNonce
                                                 )

        KarhooApi.paymentsService.addPaymentMethod(addPaymentRequest).execute { result ->
            hideLoading()
            when (result) {
                is Resource.Success -> bindCardDetails(KarhooApi.userStore.savedPaymentInfo)
                is Resource.Failure -> toastErrorMessage(result.error)
            }
        }
    }

    private fun bindCardDetails(savedPaymentInfo: SavedPaymentInfo?) {
        payment_details?.text = savedPaymentInfo?.lastFour
    }

    fun passBackThreeDSecuredNonce(
            threeDSNonce: PaymentMethodNonce?, passengerDetails:
            PassengerDetails?, comments: String) {

        toastErrorMessage("All good, lets book the trip")
        showLoading()
        val nonce = threeDSNonce?.nonce.orEmpty()

        passengerDetails?.let {
            KarhooApi.tripService.book(TripBooking(nonce = nonce,
                                                   quoteId = quote?.id.orEmpty(),
                                                   passengers = Passengers(
                                                           additionalPassengers = 0,
                                                           passengerDetails = listOf(passengerDetails)
                                                                          ),
                                                   flightNumber = null,
                                                   comments = comments))
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
                        .BookingSuccess(tripInfo)
                                            )
    }

    fun onTripBookFailure(error: KarhooError) {
        when (error) {
            KarhooError.CouldNotBookPaymentPreAuthFailed -> {
                toastErrorMessage("Got CouldNotBookPaymentPreAuthFailed, need to show the payment dialog")
                showPaymentDialog(braintreeSDKToken)
            }
            KarhooError.InvalidRequestPayload -> toastErrorMessage(error)
            else -> toastErrorMessage(error)
        }
    }

    private fun getPassengerDetails(): PassengerDetails {
        val user = KarhooApi.userStore.currentUser
        return PassengerDetails(firstName = user.firstName,
                                lastName = user.lastName,
                                phoneNumber = user.phoneNumber,
                                email = user.email,
                                locale = user.locale)
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

    fun showPaymentUI(braintreeSDKToken: String) {
        toastErrorMessage("[Braintree] Show Braintree Dialog")
        val dropInRequest = DropInRequest().clientToken(braintreeSDKToken)
        this.braintreeSDKToken = braintreeSDKToken
        (context as Activity).startActivityForResult(dropInRequest.getIntent(context),
                                                     REQ_CODE_BRAINTREE)
    }

    private fun quotePriceToAmount(quote: Quote?): String {
        val currency = Currency.getInstance(quote?.price?.currencyCode?.trim())
        return CurrencyUtils.intToPriceNoSymbol(currency, quote?.price?.highPrice.orZero())
    }

    fun onBraintreeActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        hideLoading()
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            when (requestCode) {
                REQ_CODE_BRAINTREE -> {
                    toastErrorMessage("Received confirmation from Braintree")
                    val braintreeResult =
                            data.getParcelableExtra<DropInResult>(DropInResult.EXTRA_DROP_IN_RESULT)
                    passBackBraintreeSDKNonce(braintreeSDKToken)
                    //passBackThreeDSecuredNonce(braintreeResult?.paymentMethodNonce, getPassengerDetails(), "")
                }
            }
        } else if (requestCode == REQ_CODE_BRAINTREE) {
            //refreshPayments()
        }
    }

    override fun onSavedPaymentInfoChanged(userPaymentInfo: SavedPaymentInfo?) {
        toastErrorMessage("Payment Info Changed")
        bindCardDetails(userPaymentInfo)
    }

    companion object {
        const val REQ_CODE_BRAINTREE = 301

        @JvmStatic
        fun newInstance(owner: LifecycleOwner,
                        bookingPlanningStateViewModel: BookingPlanningStateViewModel,
                        bookingQuoteStateViewModel: BookingQuoteStateViewModel,
                        bookingRequestStateViewModel: BookingRequestStateViewModel) = TripBookingFragment().apply {
            this.bookingPlanningStateViewModel = bookingPlanningStateViewModel
            this.bookingQuoteStateViewModel = bookingQuoteStateViewModel
            this.bookingRequestStateViewModel = bookingRequestStateViewModel
            bookingPlanningStateViewModel.viewStates().observe(owner, createPlanningObservable())
            bookingQuoteStateViewModel.viewStates().observe(owner, createQuoteObservable())
        }
    }
}