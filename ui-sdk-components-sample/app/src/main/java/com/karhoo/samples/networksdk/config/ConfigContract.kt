package com.karhoo.samples.networksdk.config

import com.karhoo.uisdk.KarhooUISDKConfiguration

class ConfigContract {

    interface Module {
        fun karhooUserConfiguration(): KarhooUISDKConfiguration
    }
}