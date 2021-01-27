package com.karhoo.samples.networksdk.configuration

enum class AuthType constructor(val value: String) {
    USERNAME_PASSWORD("Username/password"),
    ADYEN_GUEST("Guest Adyen"),
    BRAINTREE_GUEST("Guest Braintree"),
    ADYEN_TOKEN("Token Exchange Adyen"),
    BRAINTREE_TOKEN("Token Exchange Braintree")
}
