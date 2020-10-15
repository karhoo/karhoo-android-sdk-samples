package com.karhoo.samples.uisdk.components.config

import com.karhoo.uisdk.KarhooUISDKConfiguration

class ConfigContract {

    interface Module {
        fun karhooUserConfiguration(): KarhooUISDKConfiguration
    }
}