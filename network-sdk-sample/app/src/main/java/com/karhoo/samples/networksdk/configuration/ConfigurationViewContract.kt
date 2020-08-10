package com.karhoo.samples.networksdk.configuration

import com.karhoo.sdk.api.KarhooError

interface ConfigurationViewContract {

    sealed class ConfigurationEvent {
        object ConfigurationSuccess : ConfigurationEvent()
        data class ConfigurationError(val error: KarhooError) : ConfigurationEvent()
    }

    sealed class ConfigurationAction {
        data class HandleBookingError(val error: KarhooError) : ConfigurationAction()
    }
}