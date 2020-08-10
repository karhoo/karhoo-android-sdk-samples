package com.karhoo.samples.networksdk

import android.app.Application
import com.karhoo.samples.networksdk.config.ConfigContract
import com.karhoo.samples.networksdk.config.SampleConfigModule
import com.karhoo.sdk.api.KarhooApi

class SampleApplication : Application() {
    val module: ConfigContract.Module = SampleConfigModule(this)

    override fun onCreate() {
        super.onCreate()
        KarhooApi.setConfiguration(configuration = module.karhooUserConfiguration())
    }
}