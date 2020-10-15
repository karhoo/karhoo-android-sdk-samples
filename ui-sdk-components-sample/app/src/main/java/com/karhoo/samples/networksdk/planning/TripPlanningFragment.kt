package com.karhoo.samples.networksdk.planning

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.uisdk.base.address.AddressCodes
import com.karhoo.uisdk.screen.booking.address.addressbar.AddressBarViewContract
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatus
import com.karhoo.uisdk.screen.booking.domain.address.BookingStatusStateViewModel
import kotlinx.android.synthetic.main.fragment_trip_planning.*

class TripPlanningFragment : BaseFragment() {

    private lateinit var bookingStatusStateViewModel: BookingStatusStateViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_planning, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialiseViews()
        initialiseViewListeners()
    }

    private fun initialiseViews() {
        address_bar_widget.watchBookingStatusState(
            this.requireActivity(),
            bookingStatusStateViewModel
        )
    }

    private fun initialiseViewListeners() {
        bookingStatusStateViewModel.viewActions().observe(this, bindToAddressBarWidgetOutputs())
    }

    private fun bindToAddressBarWidgetOutputs(): Observer<in AddressBarViewContract.AddressBarActions> {
        return Observer { actions ->
            when (actions) {
                is AddressBarViewContract.AddressBarActions.ShowAddressActivity ->
                    startActivityForResult(actions.intent, actions.addressCode)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
            when (requestCode) {
                AddressCodes.PICKUP -> address_bar_widget.onActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
                AddressCodes.DESTINATION -> address_bar_widget.onActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun bindToAddressBarOutputs(): Observer<BookingStatus> {
        return Observer { bookingStatus ->
            bookingStatus?.let {
                it.pickup?.let { pickup ->
                    selected_pickup?.text = pickup.displayAddress
                } ?: run {
                    selected_pickup?.text = ""
                }

                it.destination?.let { dropoff ->
                    selected_drop_off?.text = dropoff.displayAddress
                } ?: run {
                    selected_drop_off?.text = ""
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            owner: LifecycleOwner,
            bookingStatusStateViewModel: BookingStatusStateViewModel
        ): TripPlanningFragment = TripPlanningFragment().apply {
            this.bookingStatusStateViewModel = bookingStatusStateViewModel
            bookingStatusStateViewModel.viewActions()
                .observe(owner, bindToAddressBarWidgetOutputs())
            bookingStatusStateViewModel.viewStates().observe(owner, bindToAddressBarOutputs())
        }
    }
}