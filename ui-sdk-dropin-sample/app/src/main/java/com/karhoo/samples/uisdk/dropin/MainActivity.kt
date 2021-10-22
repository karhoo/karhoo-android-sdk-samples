package com.karhoo.samples.uisdk.dropin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.karhoo.samples.uisdk.dropin.config.GuestConfig
import com.karhoo.samples.uisdk.dropin.config.TokenExchangeConfig
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.model.Position
import com.karhoo.sdk.api.network.request.PassengerDetails
import com.karhoo.sdk.api.network.response.Resource
import com.karhoo.uisdk.KarhooUISDK
import com.karhoo.uisdk.screen.booking.BookingActivity
import com.karhoo.uisdk.screen.booking.domain.address.JourneyInfo

class MainActivity : AppCompatActivity() {

    private lateinit var loadingProgressBar: View

    val passengerDetails = PassengerDetails(firstName = "Passenger",
        lastName = "Details", email = "passenger+email@karhoo.com",
        phoneNumber = "+15005550006", locale = "en")
    val journeyInfo = JourneyInfo(
        origin = Position(latitude = 51.5166777, longitude = -0.1791215),
        destination = Position(latitude = 51.5010249, longitude = -0.1268513), date = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        KarhooApi.userService.logout()

        loadingProgressBar = findViewById<View>(R.id.loadingSpinner)

        findViewById<Button>(R.id.bookTripButtonGuest).setOnClickListener {
            showLoading()

            applyGuestConfig()

            goToBooking()
        }

        findViewById<Button>(R.id.bookTripButtonGuestWithPassenger).setOnClickListener {
            showLoading()

            applyGuestConfig()

            goToBookingWithPassenger()
        }

        findViewById<Button>(R.id.bookTripButtonGuestWithPassengerWithJourney).setOnClickListener {
            showLoading()

            applyGuestConfig()

            goToBookingWithPassengerAndJourney()
        }

        findViewById<Button>(R.id.bookTripButtonTokenExchange).setOnClickListener {
            showLoading()

            applyTokenExchangeConfig()

            loginTokenExchange(withPassenger = false)
        }

        findViewById<Button>(R.id.bookTripButtonTokenExchangeWithPassenger).setOnClickListener {
            showLoading()

            applyTokenExchangeConfig()

            loginTokenExchange(withPassenger = true)
        }
    }

    private fun applyTokenExchangeConfig() {
        KarhooUISDK.apply {
            setConfiguration(
                TokenExchangeConfig(
                    applicationContext
                )
            )
        }
    }

    private fun applyGuestConfig() {
        KarhooUISDK.apply {
            setConfiguration(
                GuestConfig(
                    applicationContext
                )
            )
        }
    }

    private fun loginTokenExchange(withPassenger: Boolean) {
        KarhooApi.userService.logout()
        val token: String = BuildConfig.BRAINTREE_AUTH_TOKEN
        KarhooApi.authService.login(token).execute { result ->
            when (result) {
                is Resource.Success -> {
                    if (withPassenger) {
                        goToBookingWithPassenger()
                    } else {
                        goToBooking()
                    }
                }
                is Resource.Failure -> toastErrorMessage(result.error)
            }
        }
    }

    private fun goToBooking() {
        val builder = BookingActivity.Builder.builder
            .initialLocation(null)
        startActivity(builder.build(this))
        hideLoading()
    }

    private fun goToBookingWithPassengerAndJourney() {
        val builder = BookingActivity.Builder.builder
            .initialLocation(null)
        startActivity(builder
            .journeyInfo(journeyInfo)
            .passengerDetails(passengerDetails)
            .comments("Some comment")
            .build(this))
        hideLoading()
    }

    private fun goToBookingWithPassenger() {
        val builder = BookingActivity.Builder.builder
            .initialLocation(null)
        startActivity(builder
            .passengerDetails(passengerDetails)
            .comments("Some comment")
            .build(this))
        hideLoading()
    }

    private fun toastErrorMessage(error: KarhooError) {
        Toast.makeText(
            this,
            error.userFriendlyMessage,
            Toast.LENGTH_LONG
        ).show()
        hideLoading()
    }

    private fun showLoading() {
        loadingProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingProgressBar.visibility = View.INVISIBLE
    }
}