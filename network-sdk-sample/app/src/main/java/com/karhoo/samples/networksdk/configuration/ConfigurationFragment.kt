package com.karhoo.samples.networksdk.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.karhoo.samples.networksdk.BuildConfig
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.SampleApplication
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.config.ConfigContract
import com.karhoo.samples.networksdk.config.SandboxConfigModule
import com.karhoo.samples.networksdk.config.StagingConfigModule
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooApi.userStore
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.sdk.api.model.UserInfo
import com.karhoo.sdk.api.network.request.UserLogin
import com.karhoo.sdk.api.network.response.Resource
import kotlinx.android.synthetic.main.fragment_configuration.auth_type_spinner
import kotlinx.android.synthetic.main.fragment_configuration.karhoo_user_card
import kotlinx.android.synthetic.main.fragment_configuration.loadingProgressBar
import kotlinx.android.synthetic.main.fragment_configuration.non_karhoo_user_card
import kotlinx.android.synthetic.main.fragment_configuration.non_karhoo_user_signin_button
import kotlinx.android.synthetic.main.fragment_configuration.password
import kotlinx.android.synthetic.main.fragment_configuration.sign_in_button
import kotlinx.android.synthetic.main.fragment_configuration.signout_button
import kotlinx.android.synthetic.main.fragment_configuration.username
import kotlinx.android.synthetic.main.fragment_configuration.welcome_message

class ConfigurationFragment : BaseFragment(), AdapterView.OnItemSelectedListener {
    private lateinit var configurationStateViewModel: ConfigurationStateViewModel
    lateinit var module: ConfigContract.Module
    var userInfo: UserInfo? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
                             ): View? {
        return inflater.inflate(R.layout.fragment_configuration, container, false)
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        module = SandboxConfigModule(this.requireContext())
        auth_type_spinner.setSelection(0)
        setConfig(AuthenticationMethod.KarhooUser())

        sign_in_button.setOnClickListener {
            onSignInButtonClick()
        }

        non_karhoo_user_signin_button.setOnClickListener {
            configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
        }

        signout_button.setOnClickListener {
            auth_type_spinner.setSelection(0)
            setConfig(AuthenticationMethod.KarhooUser())
            KarhooApi.userService.logout()
            userInfo = null
        }

        auth_type_spinner.visibility = LinearLayout.VISIBLE
        val loginTypeAdapter = ArrayAdapter<String>(this.requireContext(), android.R.layout
                .simple_spinner_dropdown_item, AuthType.values().map { it.value })
        with(auth_type_spinner) {
            adapter = loginTypeAdapter
            onItemSelectedListener = this@ConfigurationFragment
        }
    }

    private fun onSignInButtonClick() {
        userInfo?.run {
            val message = resources.getString(R.string.welcome_message)
            welcome_message.text = String.format(message, "")
            configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
        } ?: run {
            login()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val type = parent?.getItemAtPosition(position)

        handleLoginTypeSelection(type as String)
    }

    private fun handleLoginTypeSelection(loginType: String) {
        if (!userStore.isCurrentUserValid && loginType != AuthType.USERNAME_PASSWORD.value) {
            KarhooApi.userService.logout()
        }
        module = SandboxConfigModule(this.requireContext())
        val authMethod: AuthenticationMethod = when (loginType) {
            AuthType.USERNAME_PASSWORD.value -> {
                AuthenticationMethod.KarhooUser()
            }
            AuthType.ADYEN_GUEST.value -> {
                AuthenticationMethod.Guest(
                        identifier = BuildConfig.ADYEN_GUEST_CHECKOUT_IDENTIFIER,
                        referer = BuildConfig.GUEST_CHECKOUT_REFERER,
                        organisationId = BuildConfig.ADYEN_GUEST_CHECKOUT_ORGANISATION_ID
                                          )
            }
            AuthType.BRAINTREE_GUEST.value -> {
                AuthenticationMethod.Guest(
                        identifier = BuildConfig.BRAINTREE_GUEST_CHECKOUT_IDENTIFIER,
                        referer = BuildConfig.GUEST_CHECKOUT_REFERER,
                        organisationId = BuildConfig.BRAINTREE_GUEST_CHECKOUT_ORGANISATION_ID
                                          )
            }
            AuthType.ADYEN_TOKEN.value -> {
                AuthenticationMethod.TokenExchange(
                        clientId = BuildConfig.ADYEN_CLIENT_ID,
                        scope = BuildConfig.ADYEN_CLIENT_SCOPE
                                                  )
            }
            AuthType.BRAINTREE_TOKEN.value -> AuthenticationMethod.TokenExchange(
                    clientId = BuildConfig.BRAINTREE_CLIENT_ID,
                    scope = BuildConfig.BRAINTREE_CLIENT_SCOPE
                                                                                )
            else -> return
        }
        setConfig(authMethod)
        updateUiForConfig(authMethod)
        activity?.title = "Network SDK [$loginType]"
    }

    private fun setConfig(authMethod: AuthenticationMethod) {
        (requireContext().applicationContext as SampleApplication).setConfiguration(
                module.karhooUserConfiguration(
                        authMethod
                                              )
                                                                                   )

    }

    private fun updateUiForConfig(authMethod: AuthenticationMethod) {
        when (authMethod) {
            is AuthenticationMethod.KarhooUser -> showKarhooUser()
            is AuthenticationMethod.Guest -> showNonKarhooUser()
            is AuthenticationMethod.TokenExchange -> showNonKarhooUser()
        }
    }

    private fun showKarhooUser() {
        karhoo_user_card.visibility = View.VISIBLE
        non_karhoo_user_card.visibility = View.GONE
    }

    private fun showNonKarhooUser() {
        karhoo_user_card.visibility = View.GONE
        non_karhoo_user_card.visibility = View.VISIBLE
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    private fun login() {
        val username = username.text.toString()
        val password = password.text.toString()
        val loginRequest = UserLogin(email = username, password = password)
        showLoading()
        KarhooApi.userService.loginUser(loginRequest).execute {
            hideLoading()
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

    private fun showLoading() {
        loadingProgressBar?.show()
    }

    private fun hideLoading() {
        loadingProgressBar?.hide()
    }

    companion object {
        @JvmStatic
        fun newInstance(configurationStateViewModel: ConfigurationStateViewModel) =
                ConfigurationFragment().apply {
                    this.configurationStateViewModel = configurationStateViewModel
                }
    }
}