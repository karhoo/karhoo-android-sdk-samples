package com.karhoo.samples.networksdk.config

import com.karhoo.sdk.api.KarhooSDKConfiguration

class ConfigContract {

    interface Module {
        fun karhooUserConfiguration(): KarhooSDKConfiguration
    }
}