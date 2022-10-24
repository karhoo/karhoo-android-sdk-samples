package com.karhoo.samples.networksdk.config

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.karhoo.samples.networksdk.R
import com.karhoo.sdk.analytics.AnalyticProvider
import com.karhoo.sdk.api.KarhooEnvironment
import com.karhoo.samples.networksdk.BuildConfig
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.uisdk.KarhooUISDKConfiguration
import com.karhoo.uisdk.screen.booking.checkout.payment.PaymentManager

class KarhooSandboxConfig(
    private val context: Context,
    private val authMethod: AuthenticationMethod
) : KarhooUISDKConfiguration {

    override lateinit var paymentManager: PaymentManager

    override fun context(): Context {
        return context
    }

    override fun environment(): KarhooEnvironment {
//        return KarhooEnvironment.Sandbox()
        return KarhooEnvironment.Custom(
            host = BuildConfig.STAGING_HOST,
            authHost = BuildConfig.STAGING_AUTH_HOST,
            guestHost = BuildConfig.STAGING_GUEST_HOST
        )
    }

    override fun logo(): Drawable? {
        return AppCompatResources.getDrawable(context, R.drawable.karhoo_wordmark)
    }

    var sdkAuthenticationRequired: ((callback: () -> Unit) -> Unit)? = null
    override suspend fun requireSDKAuthentication(callback: () -> Unit) {
        sdkAuthenticationRequired?.invoke(callback)
    }

    override fun analyticsProvider(): AnalyticProvider? {
        return null
    }

    override fun authenticationMethod(): AuthenticationMethod {
        return authMethod
    }
}