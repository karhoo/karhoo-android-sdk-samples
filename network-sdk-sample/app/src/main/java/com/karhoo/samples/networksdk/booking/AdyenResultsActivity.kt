package com.karhoo.samples.networksdk.booking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.karhoo.samples.networksdk.R

class AdyenResultActivity : AppCompatActivity() {
    companion object {
        const val RESULT_KEY = "payment_result"
        const val TYPE_KEY = "integration_type"

        fun start(context: Context, paymentResult: String) {
            val intent = Intent(context, AdyenResultActivity::class.java)
            intent.putExtra(RESULT_KEY, paymentResult)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val res = intent?.getStringExtra(RESULT_KEY) ?: "Processing"
        val data = Intent()
        data.putExtra(RESULT_KEY, res)
        setResult(RESULT_OK, data)

        finish()
    }
}
