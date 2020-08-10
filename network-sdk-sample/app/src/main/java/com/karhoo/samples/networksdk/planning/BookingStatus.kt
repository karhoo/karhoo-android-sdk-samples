package com.karhoo.samples.networksdk.planning

import com.karhoo.sdk.api.model.LocationInfo

data class BookingStatus(
    var pickup: LocationInfo?,
    var destination: LocationInfo?
)