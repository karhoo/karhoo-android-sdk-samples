package com.karhoo.samples.networksdk.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.karhoo.sdk.api.KarhooError

open class BaseFragment : Fragment() {
    fun toastErrorMessage(error: KarhooError) {
        if (isVisible) {
            context.let {
                Toast.makeText(
                        requireContext(),
                        error.userFriendlyMessage,
                        Toast.LENGTH_LONG
                              ).show()
            }
        }
    }

    fun toastErrorMessage(resId: Int) {
        if (isVisible) {
            context.let {
                Toast.makeText(
                        requireContext(),
                        resId,
                        Toast.LENGTH_LONG
                              ).show()
            }
        }
    }

    fun toastErrorMessage(message: String) {
        if (isVisible) {
            context.let {
                Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                              ).show()
            }
        }
    }
}