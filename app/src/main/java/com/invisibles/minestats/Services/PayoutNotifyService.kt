package com.invisibles.minestats.Services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.invisibles.minestats.*

class PayoutNotifyService : Service() {

    private lateinit var storage: Storage
    private lateinit var miningAccount: String
    private lateinit var notifyPayout: Notification
    private lateinit var binanceApi: BinanceAPI

    private var isWork = false
    private var waitingForPayment = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        initData()

    }

    private fun initData() {
        storage = Storage(this)
        miningAccount = storage.getValue("miningAccount", "null")
        notifyPayout = Notification(this)
        binanceApi = BinanceAPI(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startCheckingPayout()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isWork = false
    }

    private fun startCheckingPayout(){
        isWork = true

        Thread{
            while (isWork){
                dataHandler()
                Thread.sleep(10000)
            }
        }.start()

    }

    private fun dataHandler(){
        binanceApi.get(mapOf("algo" to "ethash", "userName" to miningAccount), BinanceAPI.STATISTIC_LIST){ res ->
            val data = res.getJSONObject("data")
            val profitYesterday = data.getJSONObject("profitYesterday")

            if(profitYesterday.has("ETH")){
                val ethQuantity = profitYesterday.getString("ETH")

                if (waitingForPayment){
                    waitingForPayment = false
                    sendNotify(ethQuantity)
                }
            }
            else{
                waitingForPayment = true
            }

        }
    }

    private fun sendNotify(ethQuantity: String) {
        val intent = Intent(this, MainActivity::class.java)
        notifyPayout.create("Payout", "Payment made, received: $ethQuantity ETH", intent, R.drawable.ic_baseline_payments_24)

    }

}