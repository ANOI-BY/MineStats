package com.invisibles.minestats

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BinanceAPI(val context: Context) {

    companion object {
        const val WORKERS_DATA = 1
        const val SERVER_TIME = 2
        const val STATISTIC_LIST = 3
        const val PRICE_ETH_USDT = 4
        const val WORKER_DETAIL = 5
        const val SUCCESS_CODE = 0
    }

    private var TIME_OFFSET = 0
    private var API_URL = context.getString(R.string.API_BINANCE_URL)
    private var secretKey: String
    private var publicKey: String

    init {
        setTimeOffset()
        val storage = Storage(context)
        secretKey = storage.getValue("secretAPI")
        publicKey = storage.getValue("publicAPI")
    }


    fun get(args: Map<String, String>, method: Int, simpleRequest: Boolean = false, Function: (JSONObject) -> Unit) {

        val thread = Thread {

            var url = getURLFromMethod(method)

            if (!simpleRequest){
                while (TIME_OFFSET == 0){
                    Thread.sleep(100)
                }
                val timestamp = System.currentTimeMillis() + TIME_OFFSET
                val queryArgs = args + mapOf("timestamp" to timestamp.toString())
                val queryString = formatterArgs(queryArgs)
                val signature = signature(queryString)
                url = "${url}?${queryString}&signature=${signature}"
            }

            val request = khttp.get(url, mapOf("X-MBX-APIKEY" to publicKey))

            val jsObj = request.jsonObject
            Function(jsObj)
        }
        thread.start()
    }

    private fun formatterArgs(args: Map<String, String>): String {
        var string = ""
        for ((key, item) in args) {
            string += "${key}=${item}&"
        }
        return string.substring(0, string.length - 1)
    }

    private fun getURLFromMethod(method: Int): String {
        return when (method) {
            WORKERS_DATA -> "$API_URL/sapi/v1/mining/worker/list"
            SERVER_TIME -> "$API_URL/api/v3/time"
            STATISTIC_LIST -> "$API_URL/sapi/v1/mining/statistics/user/status"
            PRICE_ETH_USDT -> "$API_URL/api/v3/ticker/price?symbol=ETHUSDT"
            WORKER_DETAIL -> "$API_URL/sapi/v1/mining/worker/detail"
            else -> ""

        }
    }

    private fun signature(queryString: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)
        return sha256Hmac.doFinal(queryString.toByteArray()).toHexString()
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun setTimeOffset() {
        val queue = Volley.newRequestQueue(context)
        val url = getURLFromMethod(SERVER_TIME)
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            {
                val serverTime = it.getLong("serverTime")
                val sub = serverTime - System.currentTimeMillis()
                TIME_OFFSET = sub.toInt()
                Log.i("API", TIME_OFFSET.toString())
            },
            { error ->
                Log.e("Main", "Request error: ${error.message}")
            })

        queue.add(request)
    }

}


/*
        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                Function(response)
            },
            { error ->
                Log.e("Main", "Request error: ${error}")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-MBX-APIKEY"] = publicKey
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }
        }
 */