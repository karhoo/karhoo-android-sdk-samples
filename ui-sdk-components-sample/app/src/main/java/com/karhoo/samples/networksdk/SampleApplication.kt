package com.karhoo.samples.networksdk

import android.app.Application
import com.karhoo.samples.networksdk.config.ConfigContract
import com.karhoo.samples.networksdk.config.SandboxConfigModule
import com.karhoo.uisdk.KarhooUISDK

class SampleApplication : Application() {
    val module: ConfigContract.Module = SandboxConfigModule(this)

    override fun onCreate() {
        super.onCreate()
        KarhooUISDK.setConfiguration(configuration = module.karhooUserConfiguration())
    }
}