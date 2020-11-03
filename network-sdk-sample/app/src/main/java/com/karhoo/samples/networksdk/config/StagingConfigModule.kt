package com.karhoo.samples.networksdk.config

import android.content.Context
import com.karhoo.samples.networksdk.BuildConfig
import com.karhoo.sdk.analytics.AnalyticProvider
import com.karhoo.sdk.api.KarhooEnvironment
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.model.AuthenticationMethod

class StagingConfigModule(private val context: Context) : ConfigContract.Module {

    override fun karhooUserConfiguration(authMethod: AuthenticationMethod): KarhooSDKConfiguration {
        return KarhooStagingConfig(context, authMethod)
    }

    class KarhooStagingConfig(
            private val context: Context,
            private val authMethod: AuthenticationMethod
                             ) : KarhooSDKConfiguration {

        override fun context(): Context {
            return context
        }

        override fun environment(): KarhooEnvironment {
            val STAGING_AUTH_HOST = BuildConfig.STAGING_AUTH_HOST
            val STAGING_GUEST_HOST = BuildConfig.STAGING_GUEST_HOST
            val STAGING_HOST = BuildConfig.STAGING_HOST
            return KarhooEnvironment.Custom(
                    host = STAGING_HOST,
                    authHost = STAGING_AUTH_HOST,
                    guestHost = STAGING_GUEST_HOST
                                           )
        }

        override fun analyticsProvider(): AnalyticProvider? {
            return null
        }

        override fun authenticationMethod(): AuthenticationMethod {
            return authMethod
        }
    }
}