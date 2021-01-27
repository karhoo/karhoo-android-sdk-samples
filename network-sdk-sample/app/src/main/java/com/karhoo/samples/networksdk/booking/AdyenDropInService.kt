package com.karhoo.samples.networksdk.booking

import com.adyen.checkout.dropin.service.CallResult
import com.adyen.checkout.dropin.service.DropInService
import com.adyen.checkout.redirect.RedirectComponent
import com.karhoo.sdk.api.KarhooApi
import com.karhoo.sdk.api.network.response.Resource
import org.json.JSONObject

class AdyenDropInService : DropInService() {

    override fun makePaymentsCall(paymentComponentData: JSONObject): CallResult {
        clearTransactionId()
        getAdyenPayments(paymentComponentData, RedirectComponent.getReturnUrl(this))
        return CallResult(CallResult.ResultType.WAIT, "")
    }

    override fun makeDetailsCall(actionComponentData: JSONObject): CallResult {
        val tripId = this.getSharedPreferences(TRIP_ID, MODE_PRIVATE)
            .getString(TRIP_ID, "")
        getAdyenPaymentDetails(actionComponentData, tripId)
        return CallResult(CallResult.ResultType.WAIT, "")
    }

    private fun storeTripId(tripId: String) {
        val sharedPref = this.getSharedPreferences(TRIP_ID, MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(TRIP_ID, tripId)
            commit()
        }
    }

    private fun clearTransactionId() {
        this.getSharedPreferences(TRIP_ID, MODE_PRIVATE).edit().clear().commit()
    }

    private fun handleResult(callResult: CallResult) {
        asyncCallback(callResult)
    }

    private fun getAdyenPayments(paymentComponentData: JSONObject, returnUrl: String) {
        clearTransactionId()
        val requestString = createPaymentRequestString(paymentComponentData, returnUrl)
        KarhooApi.paymentsService.getAdyenPayments(requestString).execute { result ->
            when (result) {
                is Resource.Success -> {
                    result.data.let { result ->
                        //TODO Find a better way to store / pass through the transaction id
                        val tripId = result.getString(TRIP_ID)
                        storeTripId(tripId)
                        result.optJSONObject(PAYLOAD)?.let { payload ->
                            handleResult(handlePaymentRequestResult(payload, tripId))
                        } ?: handleResult(handlePaymentRequestResult(result, tripId))
                    }
                }
                is Resource.Failure -> {
                    handleResult(CallResult(CallResult.ResultType.ERROR, result.error
                        .userFriendlyMessage))
                }
            }
        }
    }

    private fun getAdyenPaymentDetails(actionComponentData: JSONObject, tripId: String?) {

        tripId?.let {
            val request = JSONObject()
            request.put(TRIP_ID, tripId)
            request.put(PAYMENTS_PAYLOAD, actionComponentData)

            KarhooApi.paymentsService.getAdyenPaymentDetails(request.toString()).execute { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data.let {
                            handleResult(handlePaymentRequestResult(it, tripId))
                        }
                    }
                    is Resource.Failure -> {
                        handleResult(CallResult(CallResult.ResultType.ERROR, result.error
                            .userFriendlyMessage))
                    }
                }
            }
        } ?: handleResult(CallResult(CallResult.ResultType.ERROR, "Invalid transactionId"))
    }

    private fun handlePaymentRequestResult(response: JSONObject, transactionId: String?): CallResult {
        return try {
            if (response.has(ACTION)) {
                CallResult(CallResult.ResultType.ACTION, response.getString(ACTION))
            } else {
                transactionId?.let {
                    response.put(TRIP_ID, transactionId)
                    CallResult(CallResult.ResultType.FINISHED, response.toString())
                } ?: CallResult(CallResult.ResultType.ERROR, "Invalid transaction id")
            }
        } catch (e: Exception) {
            CallResult(CallResult.ResultType.ERROR, e.toString())
        }
    }

    private fun createPaymentRequestString(paymentComponentData: JSONObject, returnUrl: String):
            String {

        val payload = JSONObject()
        for (name in paymentComponentData.keys()) {
            val obj = paymentComponentData.get(name)
            if (obj !is String) {
                payload.put(name, obj)
            } else if (obj.isNotBlank()) {
                payload.put(name, obj)
            }
        }
        payload.put(RETURN_URL, returnUrl)
        payload.put(CHANNEL, "Android")

        val additionalData = JSONObject()
        additionalData.put(ALLOW_3DS, ALLOW_3DS_TRUE)
        payload.put(ADDITIONAL_DATA, additionalData)

        val request = JSONObject()
        request.put(PAYMENTS_PAYLOAD, payload)
        request.put(RETURN_URL_SUFFIX, "")

        return request.toString()
    }


    companion object {
        const val ACTION = "action"
        const val ALLOW_3DS = "allow3DS2"
        const val ALLOW_3DS_TRUE = "true"
        const val ADDITIONAL_DATA = "additionalData"
        const val CHANNEL = "channel"
        const val PAYLOAD = "payload"
        const val PAYMENTS_PAYLOAD = "payments_payload"
        const val RETURN_URL = "returnUrl"
        const val RETURN_URL_SUFFIX = "return_url_suffix"
        const val TRIP_ID = "trip_id"
    }
}