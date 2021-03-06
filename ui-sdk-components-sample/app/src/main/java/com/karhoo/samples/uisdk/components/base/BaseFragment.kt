package com.karhoo.samples.uisdk.components.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.karhoo.sdk.api.KarhooError

open class BaseFragment : Fragment() {
    fun toastErrorMessage(error: KarhooError) {
        if(activity != null) {
            Toast.makeText(
                activity,
                error.userFriendlyMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun toastErrorMessage(resId: Int) {
        if(activity != null) {
            Toast.makeText(
                activity,
                resId,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}