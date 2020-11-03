package com.karhoo.samples.networksdk.configuration

enum class AuthType constructor(val value: String) {
    USERNAME_PASSWORD("Username/password"),
    ADYEN_GUEST("Guest [A]"),
    BRAINTREE_GUEST("Guest [B]"),
    ADYEN_TOKEN("Token Exchange [A]"),
    BRAINTREE_TOKEN("Token Exchange [B]")
}
