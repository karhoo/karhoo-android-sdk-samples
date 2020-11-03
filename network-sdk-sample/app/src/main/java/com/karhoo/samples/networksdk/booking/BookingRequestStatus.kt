package com.karhoo.samples.networksdk.booking

import com.karhoo.sdk.api.model.TripInfo

data class BookingRequestStatus(var tripInfo: TripInfo?, var isGuest: Boolean)