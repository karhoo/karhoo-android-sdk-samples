package com.karhoo.samples.uisdk.components

import android.app.Application
import com.karhoo.samples.uisdk.components.config.ConfigContract
import com.karhoo.samples.uisdk.components.config.SandboxConfigModule
import com.karhoo.uisdk.KarhooUISDK

class SampleApplication : Application() {
    val module: ConfigContract.Module =
        SandboxConfigModule(this)

    override fun onCreate() {
        super.onCreate()
        KarhooUISDK.setConfiguration(configuration = module.karhooUserConfiguration())
    }
}