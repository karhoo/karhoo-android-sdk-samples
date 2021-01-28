package com.karhoo.samples.networksdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.booking.AdyenTripBookingFragment
import com.karhoo.samples.networksdk.booking.BookingRequestStateViewModel
import com.karhoo.samples.networksdk.booking.BraintreeTripBookingFragment
import com.karhoo.samples.networksdk.booking.BraintreeTripBookingFragment.Companion.REQ_CODE_BRAINTREE
import com.karhoo.samples.networksdk.booking.BraintreeTripBookingFragment.Companion.REQ_CODE_BRAINTREE_GUEST
import com.karhoo.samples.networksdk.configuration.ConfigurationFragment
import com.karhoo.samples.networksdk.configuration.ConfigurationStateViewModel
import com.karhoo.samples.networksdk.configuration.ConfigurationViewContract
import com.karhoo.samples.networksdk.planning.BookingPlanningStateViewModel
import com.karhoo.samples.networksdk.planning.TripPlanningFragment
import com.karhoo.samples.networksdk.quotes.BookingQuoteStateViewModel
import com.karhoo.samples.networksdk.quotes.TripQuotesFragment
import com.karhoo.samples.networksdk.tracking.TripTrackingFragment
import com.karhoo.sdk.api.KarhooApi
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val NUM_PAGES = 5
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter
    lateinit var pages: MutableList<BaseFragment>
    private val headers = listOf(
        R.string.sign_in_header,
        R.string.plan_trip_header,
        R.string.quotes_header,
        R.string.book_trip_header,
        R.string.track_trip_header
    )
    private val bookingPlanningStateViewModel: BookingPlanningStateViewModel by lazy {
        ViewModelProvider(this).get(BookingPlanningStateViewModel::class.java)
    }
    private val bookingQuoteStateViewModel: BookingQuoteStateViewModel by lazy {
        ViewModelProvider(this).get(BookingQuoteStateViewModel::class.java)
    }

    private val bookingRequestStateViewModel: BookingRequestStateViewModel by lazy {
        ViewModelProvider(this).get(BookingRequestStateViewModel::class.java)
    }

    private val configurationStateViewModel: ConfigurationStateViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationStateViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pages = mutableListOf(
            ConfigurationFragment.newInstance(configurationStateViewModel),
            TripPlanningFragment.newInstance(this, bookingPlanningStateViewModel),
            TripQuotesFragment.newInstance(
                this,
                bookingPlanningStateViewModel,
                bookingQuoteStateViewModel
            ),
            BraintreeTripBookingFragment.newInstance(
                this,
                bookingPlanningStateViewModel,
                bookingQuoteStateViewModel,
                bookingRequestStateViewModel
            ),
            TripTrackingFragment.newInstance(this, bookingRequestStateViewModel)
        )

        pagerAdapter = ScreenSlidePagerAdapter(this).apply {
            data = pages
        }

        pager.adapter = pagerAdapter

        TabLayoutMediator(tab_layout, pager) { tab, position ->
            tab.text = getString(headers[position])
        }.attach()

        configurationStateViewModel.viewStates().observe(this, Observer {
            if (it.signedIn) {
                pager.currentItem = 1
            }
        })

        configurationStateViewModel.viewActions().observe(this, Observer {
            if (it is ConfigurationViewContract.ConfigurationAction.HandleBookingError) {
                pager.currentItem = 0
            }
        })

        bookingPlanningStateViewModel.viewStates().observe(this, Observer {
            if (it.pickup != null && it.destination != null) {
                pager.currentItem = 2
            }
        })

        bookingQuoteStateViewModel.viewStates().observe(this, Observer {
            if (it.selectedQuote != null) {
                pager.currentItem = 3
            }
        })

        bookingRequestStateViewModel.viewStates().observe(this, Observer {
            if (it.tripInfo != null) {
                pager.currentItem = 4
            }
        })
    }

    fun setPagerBookingFragment() {
        pages[3] = getBookingFragment()
//        pager.adapter?.notifyDataSetChanged()
//        pa
        pager.adapter?.notifyItemChanged(3)

//        val adap = pager.adapter
//        adap.
//        val isAdyen = pager.adapter?.notifyItemChanged(3)
        Log.d("PD36", "isAdyen")
    }

    private fun getBookingFragment(): BaseFragment {
        Log.d("PD36", "Provider id: ${KarhooApi.userStore.paymentProvider?.id}")
        return if (KarhooApi.userStore.paymentProvider?.id == "Adyen") {
            Log.d("PD36", "Create Adyen")
            AdyenTripBookingFragment.newInstance(
                this,
                bookingPlanningStateViewModel,
                bookingQuoteStateViewModel,
                bookingRequestStateViewModel
            )
        } else {
            Log.d("PD36", "Create Braintree")
            BraintreeTripBookingFragment.newInstance(
                this,
                bookingPlanningStateViewModel,
                bookingQuoteStateViewModel,
                bookingRequestStateViewModel
            )
        }
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE_BRAINTREE || requestCode == REQ_CODE_BRAINTREE_GUEST) {
            (pages[3] as BraintreeTripBookingFragment).onBraintreeActivityResult(
                requestCode,
                resultCode,
                data
            )
        } else if (requestCode == AdyenTripBookingFragment.REQ_CODE_ADYEN) {
            (pages[3] as AdyenTripBookingFragment).onAdyenActivityResult(
                requestCode,
                resultCode,
                data
            )
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        var data = mutableListOf(
            ConfigurationFragment.newInstance(configurationStateViewModel),
            TripPlanningFragment.newInstance(fa, bookingPlanningStateViewModel),
            TripQuotesFragment.newInstance(
                fa,
                bookingPlanningStateViewModel,
                bookingQuoteStateViewModel
            ),
            BraintreeTripBookingFragment.newInstance(
                fa,
                bookingPlanningStateViewModel,
                bookingQuoteStateViewModel,
                bookingRequestStateViewModel
            ),
            TripTrackingFragment.newInstance(
                fa,
                bookingRequestStateViewModel
            )
        )

        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment = data[position]

    }
}