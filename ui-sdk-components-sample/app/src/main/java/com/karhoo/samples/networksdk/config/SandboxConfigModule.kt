package com.karhoo.samples.networksdk.config

import android.content.Context
import android.graphics.drawable.Drawable
import com.karhoo.samples.networksdk.R
import com.karhoo.sdk.analytics.AnalyticProvider
import com.karhoo.sdk.api.KarhooEnvironment
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.uisdk.KarhooUISDKConfiguration

class SandboxConfigModule(private val context: Context) : ConfigContract.Module {

    override fun karhooUserConfiguration(): KarhooUISDKConfiguration {
        return KarhooConfig(context)
    }

    class KarhooConfig(private val context: Context) : KarhooUISDKConfiguration {

        override fun context(): Context {
            return context
        }

        override fun environment(): KarhooEnvironment {
            return KarhooEnvironment.Sandbox()
        }

        override fun analyticsProvider(): AnalyticProvider? {
            return null
        }

        override fun authenticationMethod(): AuthenticationMethod {
            return AuthenticationMethod.KarhooUser()
        }

        override fun logo(): Drawable? {
            return context.getDrawable(R.drawable.uisdk_ic_labs)
        }
    }
}