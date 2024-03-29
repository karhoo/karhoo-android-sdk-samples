package com.karhoo.samples.networksdk.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.karhoo.samples.networksdk.BuildConfig
import com.karhoo.samples.networksdk.MainActivity
import com.karhoo.samples.networksdk.R
import com.karhoo.samples.networksdk.SampleApplication
import com.karhoo.samples.networksdk.base.BaseFragment
import com.karhoo.samples.networksdk.config.KarhooSandboxConfig
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.KarhooApi.userStore
import com.karhoo.sdk.api.KarhooError
import com.karhoo.sdk.api.model.AuthenticationMethod
import com.karhoo.sdk.api.network.request.UserLogin
import com.karhoo.sdk.api.network.response.Resource
import com.karhoo.uisdk.KarhooUISDK
import com.karhoo.uisdk.screen.booking.checkout.payment.AdyenPaymentManager
import com.karhoo.uisdk.screen.booking.checkout.payment.BraintreePaymentManager
import com.karhoo.uisdk.screen.booking.checkout.payment.adyen.AdyenPaymentView
import com.karhoo.uisdk.screen.booking.checkout.payment.braintree.BraintreePaymentView
import kotlinx.android.synthetic.main.fragment_configuration.*

class ConfigurationFragment : BaseFragment(), AdapterView.OnItemSelectedListener {
    private lateinit var configurationStateViewModel: ConfigurationStateViewModel
    private var braintreePaymentManager: BraintreePaymentManager = BraintreePaymentManager()
    private var adyenPaymentManager: AdyenPaymentManager = AdyenPaymentManager()

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

        adyenPaymentManager.paymentProviderView = AdyenPaymentView()
        braintreePaymentManager.paymentProviderView = BraintreePaymentView()

        auth_type_spinner.setSelection(0)
        setConfig(AuthenticationMethod.KarhooUser())

        sign_in_button.setOnClickListener {
            login()
        }

        non_karhoo_user_signin_button.setOnClickListener {
            getPaymentProvider()
        }

        sign_out_button.setOnClickListener {
            auth_type_spinner.setSelection(0)
            setConfig(AuthenticationMethod.KarhooUser())
            logout()
        }

        auth_type_spinner.visibility = LinearLayout.VISIBLE
        val loginTypeAdapter = ArrayAdapter<String>(this.requireContext(), android.R.layout
            .simple_spinner_dropdown_item, AuthType.values().map { it.value })
        with(auth_type_spinner) {
            adapter = loginTypeAdapter
            onItemSelectedListener = this@ConfigurationFragment
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val type = parent?.getItemAtPosition(position)

        handleLoginTypeSelection(type as String)
    }

    private fun handleLoginTypeSelection(loginType: String) {
        if (!userStore.isCurrentUserValid && loginType != AuthType.USERNAME_PASSWORD.value) {
            logout()
        }
        var isAdyen = false
        val authMethod: AuthenticationMethod = when (loginType) {
            AuthType.USERNAME_PASSWORD.value -> {
                AuthenticationMethod.KarhooUser()
            }
            AuthType.ADYEN_GUEST.value -> {
                isAdyen = true
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
                isAdyen = true
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
        setConfig(authMethod, isAdyen = isAdyen)
        updateUiForConfig(authMethod)
        activity?.title = "Network SDK [$loginType]"
    }

    private fun logout() {
        KarhooApi.userService.logout()
        updateView()
    }

    private fun updateView() {
        if (userStore.isCurrentUserValid) {
            setLoggedInView()
        } else {
            setLoggedOutView()
        }
    }

    private fun setLoggedInView() {
        val user = userStore.currentUser
        username.setText(user.email)
        val message = resources.getString(R.string.welcome_message)
        welcome_message.text = String.format(message, user.firstName)
        sign_in_button.visibility = GONE
        non_karhoo_user_card.visibility = GONE
        authentication_type_card.visibility = GONE
        sign_out_button.visibility = VISIBLE
    }

    private fun setLoggedOutView() {
        welcome_message.text = resources.getString(R.string.not_signed_in)
        authentication_type_card.visibility = VISIBLE
        sign_in_button.visibility = VISIBLE
        non_karhoo_user_card.visibility = GONE
        sign_out_button.visibility = GONE
    }

    private fun setConfig(authMethod: AuthenticationMethod, isAdyen: Boolean = false) {
        context?.applicationContext?.let { KarhooSandboxConfig(it, authMethod).apply {
            paymentManager = if(!isAdyen) braintreePaymentManager else adyenPaymentManager
        } }
            ?.let { KarhooUISDK.setConfiguration(it) }

        (requireContext().applicationContext as SampleApplication)
            .setConfiguration(KarhooSandboxConfig(this.requireContext(), authMethod).apply {
                paymentManager = if(!isAdyen) braintreePaymentManager else adyenPaymentManager
            })
    }

    private fun updateUiForConfig(authMethod: AuthenticationMethod) {
        when (authMethod) {
            is AuthenticationMethod.KarhooUser -> showKarhooUser()
            is AuthenticationMethod.Guest -> showNonKarhooUser()
            is AuthenticationMethod.TokenExchange -> showNonKarhooUser()
        }
    }

    private fun showKarhooUser() {
        karhoo_user_card.visibility = VISIBLE
        non_karhoo_user_card.visibility = GONE
    }

    private fun showNonKarhooUser() {
        karhoo_user_card.visibility = GONE
        non_karhoo_user_card.visibility = VISIBLE
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
                    updateView()
                    getPaymentProvider()
                }
                is Resource.Failure -> {
                    if (it.error == KarhooError.UserAlreadyLoggedIn) {
                        updateView()
                        getPaymentProvider()
                    } else {
                        toastErrorMessage(it.error)
                        ConfigurationViewContract.ConfigurationEvent.ConfigurationError(it.error)
                    }
                }
            }
        }
    }

    private fun getPaymentProvider() {
        KarhooApi.paymentsService.getPaymentProvider().execute {
            hideLoading()
            when (it) {
                is Resource.Success -> {
                    configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
                    (activity as MainActivity).setPagerBookingFragment()
                }
                is Resource.Failure -> {
                    if (it.error == KarhooError.UserAlreadyLoggedIn) {
                        configurationStateViewModel.process(ConfigurationViewContract.ConfigurationEvent.ConfigurationSuccess)
                        (activity as MainActivity).setPagerBookingFragment()
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