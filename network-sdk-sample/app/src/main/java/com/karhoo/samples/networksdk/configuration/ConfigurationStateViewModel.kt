package com.karhoo.samples.networksdk.configuration

import android.app.Application
import com.karhoo.samples.networksdk.base.state.BaseStateViewModel
import com.karhoo.sdk.api.KarhooError

class ConfigurationStateViewModel(application: Application) :
    BaseStateViewModel<ConfigurationStatus,
            ConfigurationViewContract.ConfigurationAction, ConfigurationViewContract.ConfigurationEvent>
        (application) {
    init {
        viewState = ConfigurationStatus(false)
    }

    // update the state by using a set of predefined contracts. Some of the event can trigger an
    // action to be performed (e.g. output of the widget)
    override fun process(viewEvent: ConfigurationViewContract.ConfigurationEvent) {
        super.process(viewEvent)
        when (viewEvent) {
            is ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess ->
                updateConfigurationStatus(true)
            is ConfigurationViewContract.ConfigurationEvent.ConfigurationError ->
                handleBookingError(viewEvent.error)
        }
    }

    private fun handleBookingError(error: KarhooError) {
        viewState = ConfigurationStatus(false)
        viewAction = ConfigurationViewContract.ConfigurationAction.HandleBookingError(error)
    }

    private fun updateConfigurationStatus(signedIn: Boolean) {
        viewState = ConfigurationStatus(true)
    }
}