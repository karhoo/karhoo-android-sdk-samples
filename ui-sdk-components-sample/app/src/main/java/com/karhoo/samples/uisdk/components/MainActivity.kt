package com.karhoo.samples.uisdk.components

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.karhoo.samples.uisdk.components.base.BaseFragment
import com.karhoo.samples.uisdk.components.booking.TripBookingFragment
import com.karhoo.samples.uisdk.components.configuration.ConfigurationFragment
import com.karhoo.samples.uisdk.components.configuration.ConfigurationStateViewModel
import com.karhoo.samples.uisdk.components.configuration.ConfigurationViewContract
import com.karhoo.samples.uisdk.components.customviews.CustomisedViewsFragment
import com.karhoo.samples.uisdk.components.planning.TripPlanningFragment
import com.karhoo.samples.uisdk.components.quotes.TripQuotesFragment
import com.karhoo.samples.uisdk.components.tracking.TripTrackingFragment
import com.karhoo.uisdk.screen.booking.booking.quotes.BookingQuotesViewModel
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatusStateViewModel
import com.karhoo.uisdk.screen.booking.domain.bookingrequest.BookingRequestStateViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var pages: List<BaseFragment>
    private val headers = listOf(
        R.string.sign_in_header,
        R.string.plan_trip_header,
        R.string.quotes_header,
        R.string.book_trip_header,
        R.string.track_trip_header
    )
    private val NUM_PAGES = headers.size

    private val bookingRequestStateViewModel: BookingRequestStateViewModel by lazy {
        ViewModelProvider(this).get(BookingRequestStateViewModel::class.java)
    }

    private val configurationStateViewModel: ConfigurationStateViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationStateViewModel::class.java)
    }

    private val bookingStatusStateViewModel: BookingStatusStateViewModel by lazy {
        ViewModelProvider(this).get(BookingStatusStateViewModel::class.java)
    }

    private val bookingSupplierViewModel: BookingQuotesViewModel by lazy {
        ViewModelProvider(this).get(BookingQuotesViewModel::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pages = listOf(
            ConfigurationFragment.newInstance(configurationStateViewModel),
            TripPlanningFragment.newInstance(this, bookingStatusStateViewModel),
            TripQuotesFragment.newInstance(
                this,
                bookingStatusStateViewModel,
                bookingSupplierViewModel
            ),
            TripBookingFragment.newInstance(
                this,
                bookingStatusStateViewModel,
                bookingSupplierViewModel,
                bookingRequestStateViewModel
            ),
            TripTrackingFragment.newInstance(this, bookingRequestStateViewModel)
        )

        val pagerAdapter = ScreenSlidePagerAdapter(this)

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

        bookingStatusStateViewModel.viewStates().observe(this, Observer {
            if (it.pickup != null && it.destination != null) {
                pager.currentItem = 2
            }
        })

        bookingSupplierViewModel.viewStates().observe(this, Observer {
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

    override fun onBackPressed() {
        if(this::viewPager.isInitialized) {
            if (viewPager.currentItem == 0) {
                // If the user is currently looking at the first step, allow the system to handle the
                // Back button. This calls finish() on this activity and pops the back stack.
                super.onBackPressed()
            } else {
                // Otherwise, select the previous step.
                viewPager.currentItem = viewPager.currentItem - 1
            }
        } else {
            super.onBackPressed()
        }
    }

    private inner class ScreenSlidePagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment = pages[position]

    }
}