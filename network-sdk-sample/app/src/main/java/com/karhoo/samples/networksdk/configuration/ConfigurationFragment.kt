package com.karhoo.samples.networksdk.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.model.UserInfo
import com.karhoo.sdk.api.network.request.UserLogin
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_configuration.*

class ConfigurationFragment : BaseFragment() {
    private lateinit var configurationStateViewModel: ConfigurationStateViewModel
    var userInfo: UserInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_configuration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sign_in_button.setOnClickListener {
            userInfo?.run {
                val message = resources.getString(R.string.welcome_message)
                welcome_message.text = String.format(message, "")
                configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
            } ?: run {
                login()
            }
        }

        guest_checkou_button.setOnClickListener {
            Toast.makeText(context, "Not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun login() {
        val username = username.text.toString()
        val password = password.text.toString()
        val loginRequest = UserLogin(email = username, password = password)
        KarhooApi.userService.loginUser(loginRequest).execute {
            when (it) {
                is Resource.Success -> {
                    userInfo = it.data
                    val message = resources.getString(R.string.welcome_message)
                    welcome_message.text = String.format(message, userInfo?.firstName)
                    configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
                }
                is Resource.Failure -> {
                    if (it.error == KarhooError.UserAlreadyLoggedIn) {
                        userInfo = UserInfo()
                        val message = resources.getString(R.string.welcome_message)
                        welcome_message.text = String.format(message, "")
                        configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
                    } else {
                        toastErrorMessage(it.error)
                        ConfigurationViewContract.ConfigurationEvent.ConfigurationError(it.error)
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            configurationStateViewModel: ConfigurationStateViewModel
        ) =
            ConfigurationFragment().apply {
                this.configurationStateViewModel = configurationStateViewModel
            }
    }
}