package com.invisibles.minestats.Services

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.invisibles.minestats.*
import kotlinx.coroutines.*
import java.lang.Exception

class PayoutNotifyService : Service() {

    private lateinit var storage: Storage
    private lateinit var miningAccount: String
    private lateinit var notifyPayout: Notification
    private lateinit var binanceApi: BinanceAPI

    companion object {
        const val REQUEST_TIMEOUT = 1 * 10 * 1000L
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var isWork = false
    private var waitingForPayment = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        rebootService()
    }

    override fun onCreate() {
        super.onCreate()

        initData()
        val intent = Intent(this, MainActivity::class.java)
        notifyPayout.create(
            "Payout",
            "I'm alive :)",
            intent,
            R.drawable.ic_baseline_expand_more_24,
            Notification.PAYOUT_NOTIFICATION
        )

        val notify = createNotification()
        startForeground(1, notify)
    }

    private fun initData() {
        storage = Storage(this)
        miningAccount = storage.getValue("miningAccount", "null")
        notifyPayout = Notification(this)
        binanceApi = BinanceAPI(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("PayoutService", "Started")

        if (intent != null){
            val action = intent.action

        }

        if (!isWork) startCheckingPayout()
        return START_STICKY
    }

    private fun stopService(){
        Log.i("PayoutService", "Stoped")
        try {
            wakeLock?.let {
                if (it.isHeld){
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.i("PayoutService", "Service stopped without being started: ${e.message}")
        }
        isWork = false
        stopForeground(true)
        ServiceState.setServiceState(
            this,
            ServiceStateID.STOPPED,
            ServicesName.PAYOUT_NOTIFY_SERVICE
        )
    }

    private fun rebootService() {
        val restartServiceIntent =
            Intent(applicationContext, PayoutNotifyService::class.java).also {
                it.setPackage(packageName)
            }
        val restartServicePendingIntent =
            PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT)
        applicationContext.getSystemService(Context.ALARM_SERVICE)
        val alarmService =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
        Log.i("PayoutService", "Rebooting...")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("PayoutService", "Destroyed")
    }

    private fun startCheckingPayout() {
        isWork = true
        ServiceState.setServiceState(
            this,
            ServiceStateID.STARTED,
            ServicesName.PAYOUT_NOTIFY_SERVICE
        )

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }


        GlobalScope.launch(Dispatchers.IO){
            while (isWork){
                launch(Dispatchers.IO) {
                    dataHandler()
                }
                delay(REQUEST_TIMEOUT)
            }
        }


    }

    private fun dataHandler() {
        binanceApi.get(
            mapOf("algo" to "ethash", "userName" to miningAccount),
            BinanceAPI.STATISTIC_LIST
        ) { res ->
            val data = res.getJSONObject("data")
            Log.i("PayoutService", "request...")

            val profitYesterday = data.getJSONObject("profitYesterday")

            if (profitYesterday.has("ETH")) {
                val ethQuantity = profitYesterday.getString("ETH")

                if (waitingForPayment) {
                    waitingForPayment = false
                    sendNotify(ethQuantity)
                }
                waitingForPayment = true
            } else {
                waitingForPayment = true
            }

        }
    }

    private fun sendNotify(ethQuantity: String) {
        val intent = Intent(this, MainActivity::class.java)
        notifyPayout.create(
            "Payout",
            "Payment made, received: $ethQuantity ETH",
            intent,
            R.drawable.ic_baseline_payments_24,
            Notification.PAYOUT_NOTIFICATION
        )
    }

    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainMenu::class.java)
        val notify = notifyPayout.getNotification("Working", "MineStats was work", intent, R.drawable.ic_baseline_expand_more_24,Notification.PAYOUT_NOTIFICATION)
        val channel = notifyPayout.getChannel(Notification.PAYOUT_NOTIFICATION)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return notify
    }

}