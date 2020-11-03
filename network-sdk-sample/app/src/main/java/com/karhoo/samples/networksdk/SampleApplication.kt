package com.karhoo.samples.networksdk

import android.app.Application
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooSDKConfiguration

class SampleApplication : Application() {
    lateinit var karhooConfig: KarhooSDKConfiguration

    fun setConfiguration(config: KarhooSDKConfiguration) {
        karhooConfig = config
        KarhooApi.setConfiguration(configuration = config)
    }
}