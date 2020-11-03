# Introduction
Android Samples for Karhoo Network SDKs

# Getting Started with the sample app
The demo app require that you add your own set of API keys:

Create a file in the app directory called secure.properties (this file should NOT be under version control to protect your API key). You need to ensure the correct environment is setup for `KarhooSDKConfiguration`
For more details refer to the `build.gradle` and `ConfigurationFragment.kt` files. 

* Add the API keys and configurations to app/secure.properties. You can also take a look at the app/secure.properties.template as an example.
    * Add Guest configuration for your account in order to enable the guest checkout journey
    * Add Token Exchange configuration for your account in order to enable the token exchange journey
    * Add Staging environment configuration in order to be able to use Staging environment
* Build and run
