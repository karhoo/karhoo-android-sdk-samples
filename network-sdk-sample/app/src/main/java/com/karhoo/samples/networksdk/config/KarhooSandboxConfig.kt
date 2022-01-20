package com.karhoo.samples.networksdk.config

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.karhoo.samples.networksdk.R
import com.karhoo.sdk.analytics.AnalyticProvider
import com.karhoo.sdk.api.KarhooEnvironment
import com.karhoo.sdk.api.KarhooSDKConfiguration
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.uisdk.KarhooUISDKConfiguration

class KarhooSandboxConfig(
    private val context: Context,
    private val authMethod: AuthenticationMethod
) : KarhooUISDKConfiguration {

    override fun context(): Context {
        return context
    }

    override fun environment(): KarhooEnvironment {
        return KarhooEnvironment.Sandbox()
    }

    override fun logo(): Drawable? {
        return AppCompatResources.getDrawable(context, R.drawable.karhoo_wordmark)
    }

    override fun analyticsProvider(): AnalyticProvider? {
        return null
    }

    override fun authenticationMethod(): AuthenticationMethod {
        return authMethod
    }
}