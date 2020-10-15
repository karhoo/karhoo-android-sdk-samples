package com.karhoo.samples.networksdk.config

import android.content.Context
import android.graphics.drawable.Drawable
import com.karhoo.samples.networksdk.R
import com.karhoo.sdk.analytics.AnalyticProvider
import com.karhoo.sdk.api.KarhooEnvironment
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.uisdk.KarhooUISDKConfiguration

class SampleConfigModule(private val context: Context) : ConfigContract.Module {

    override fun karhooUserConfiguration(): KarhooUISDKConfiguration {
        return KarhooConfig(context)
    }

    class KarhooConfig(private val context: Context) : KarhooUISDKConfiguration {

        override fun context(): Context {
            return context
        }

        override fun environment(): KarhooEnvironment {
            val STAGING_AUTH_HOST = "https://sso.stg.karhoo.net"
            val STAGING_GUEST_HOST = "https://public-api.stg.karhoo.net"
            val STAGING_HOST = "https://rest.stg.karhoo.net"
            return KarhooEnvironment.Custom(host = STAGING_HOST,
                                            authHost = STAGING_AUTH_HOST,
                                            guestHost = STAGING_GUEST_HOST)
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