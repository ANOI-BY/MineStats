package com.invisibles.minestats.Services

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import androidx.core.app.JobIntentService
import com.invisibles.minestats.*

fun enqueueWork(context: Context, work: Intent){
    JobIntentService.enqueueWork(context, PayoutIntentService::class.java, 1000, work)
}

class PayoutIntentService: JobIntentService() {

    private lateinit var storage: Storage
    private lateinit var miningAccount: String
    private lateinit var notifyPayout: Notification
    private lateinit var binanceApi: BinanceAPI

    companion object {
        const val REQUEST_TIMEOUT = 1 * 60 * 1000L
    }

    private var isWork = false
    private var waitingForPayment = false
    private var handler = Looper.getMainLooper()

    override fun onHandleWork(intent: Intent) {
        if (!isWork){
            startService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceState.setServiceState(this, ServiceStateID.STOPPED, ServicesName.PAYOUT_NOTIFY_SERVICE)
    }

    override fun onStopCurrentWork(): Boolean {
        ServiceState.setServiceState(this, ServiceStateID.STOPPED, ServicesName.PAYOUT_NOTIFY_SERVICE)
        return super.onStopCurrentWork()
    }

    private fun startService(){
        isWork = true
        ServiceState.setServiceState(this, ServiceStateID.STARTED, ServicesName.PAYOUT_NOTIFY_SERVICE)
        initData()


        while (isWork){
            dataHandler()
            Thread.sleep(REQUEST_TIMEOUT)
        }

    }

    private fun initData(){
        storage = Storage(this)
        miningAccount = storage.getValue("miningAccount", "null")
        notifyPayout = Notification(this)
        binanceApi = BinanceAPI(this)
    }

    private fun dataHandler() {
        binanceApi.get(
            mapOf("algo" to "ethash", "userName" to miningAccount),
            BinanceAPI.STATISTIC_LIST
        ) { res ->
            val data = res.getJSONObject("data")
            Log.i("PayoutService", "request... $waitingForPayment")

            val profitYesterday = data.getJSONObject("profitYesterday")

            if (profitYesterday.has("ETH")) {
                val ethQuantity = profitYesterday.getString("ETH")

                if (waitingForPayment) {
                    waitingForPayment = false
                    sendNotify(ethQuantity)
                }
            } else {
                waitingForPayment = true
            }

        }
    }

    private fun sendNotify(ethQuantity: String) {
        Looper.prepare()
        val intent = Intent(this, MainActivity::class.java)
        handler.run {
            notifyPayout.create(
                "Payout",
                "Payment made, received: $ethQuantity ETH",
                intent,
                R.drawable.ic_baseline_payments_24,
                Notification.PAYOUT_NOTIFICATION
            )
        }
    }

}