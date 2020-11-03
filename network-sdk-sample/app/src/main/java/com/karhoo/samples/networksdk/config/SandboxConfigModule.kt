package com.karhoo.samples.networksdk.config

import android.content.Context
import com.karhoo.sdk.analytics.AnalyticProvider
import com.karhoo.sdk.api.KarhooEnvironment
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.model.AuthenticationMethod

class SandboxConfigModule(private val context: Context) : ConfigContract.Module {

    override fun karhooUserConfiguration(authMethod: AuthenticationMethod): KarhooSDKConfiguration {
        return KarhooSandboxConfig(context, authMethod)
    }

    class KarhooSandboxConfig(
            private val context: Context,
            private val authMethod: AuthenticationMethod
                             ) : KarhooSDKConfiguration {

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
            return authMethod
        }
    }
}