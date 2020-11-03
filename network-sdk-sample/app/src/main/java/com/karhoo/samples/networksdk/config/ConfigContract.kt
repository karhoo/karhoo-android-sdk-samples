package com.karhoo.samples.networksdk.config

import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.model.AuthenticationMethod

class ConfigContract {

    interface Module {
        fun karhooUserConfiguration(authMethod: AuthenticationMethod): KarhooSDKConfiguration
    }
}