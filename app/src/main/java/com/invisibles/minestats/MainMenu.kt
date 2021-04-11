package com.invisibles.minestats

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.forEach
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.invisibles.minestats.Graphics.GraphItem
import com.invisibles.minestats.Graphics.GraphicHash
import org.json.JSONArray
import kotlin.collections.ArrayList
import com.invisibles.minestats.Graphics.GraphLine
import com.invisibles.minestats.Services.*

const val curveTopMarginStatusOpen = 330f
const val curveTopMarginStatusClose = 105f

class MainMenu : AppCompatActivity() {

    //Init pers
    private lateinit var MAText: TextView
    private lateinit var miningAccount: String
    private lateinit var fifteenHashRate: TextView
    private lateinit var twentyFourHashrate: TextView
    private lateinit var numOfWorkersView: TextView
    private lateinit var binanceApi: BinanceAPI
    private lateinit var switchStatusForm: ImageButton
    private lateinit var statusForm: RelativeLayout
    private lateinit var statusBlock: RelativeLayout
    private lateinit var receivedForToday: TextView
    private lateinit var hashrateCurve: GraphicHash
    private lateinit var curveForm: RelativeLayout
    private lateinit var scrollView: ScrollView
    private lateinit var curveBlock: RelativeLayout
    private lateinit var switchCurveForm: ImageButton
    private lateinit var ethereumCost: TextView
    private lateinit var receivedForYesterday: TextView
    private lateinit var statusThread: Thread
    private lateinit var hashrateThread: Thread
    private lateinit var autoupdate: Autoupdate
    private lateinit var updateAllBlocks: RelativeLayout
    private lateinit var updateEmptySpace: RelativeLayout
    private lateinit var updateButton: Button
    private lateinit var updateVersion: TextView
    private lateinit var spin: ConstraintLayout
    private lateinit var spinEmptySpace: RelativeLayout
    private lateinit var storage: Storage
    private lateinit var mainNavbar: BottomNavigationView
    private lateinit var homeFragment: HomeStats
    private lateinit var mainMenu: Menu

    private var workersList: ArrayList<String> = arrayListOf()
    private var hashrateWorkers: JSONArray = JSONArray()
    private var hashrateWorkersPut = 0
    private var ethPrice = 0.toDouble()


    private var statusIsOpen = true
    private var curveIsOpen = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setupComponents()
        initMainNavbar()
        //setComponentsListener()
        //runUpdateInformation()
        //checkUpdates()
        //startServices()
        //ServiceState.setServiceState(this, ServiceStateID.STOPPED, ServicesName.PAYOUT_NOTIFY_SERVICE)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mainMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    private fun initMainNavbar(){
        mainNavbar.disableTooltip()

        setDefaultView()

        mainNavbar.setOnNavigationItemSelectedListener { item ->

            when (item.itemId){
                R.id.menu_home -> {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_frame, homeFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()


                }
            }

            true
        }
    }

    private fun BottomNavigationView.disableTooltip(){
        val content = getChildAt(0)
        if (content is ViewGroup){
            content.forEach {
                it.setOnLongClickListener {
                    return@setOnLongClickListener true
                }
            }
        }
    }

    private fun setDefaultView() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_frame, homeFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    private fun startServices() {
        if (ServiceState.getServiceState(this, ServicesName.PAYOUT_NOTIFY_SERVICE) == ServiceStateID.STOPPED){
            enqueueWork(this, Intent(this, PayoutIntentService::class.java))
        }
    }

    private fun checkUpdates() {
        autoupdate = Autoupdate(this)
        val function = { version: String ->
            runOnUiThread {
                updateAllBlocks.visibility = View.VISIBLE
                updateVersion.text = version
            }
        }

        autoupdate.update(function)
    }

    //Threading data updates
    private fun runUpdateInformation(){
        statusThread = Thread{
            while (true){
                loadInformation()
                Thread.sleep(60000)
            }
        }

        statusThread.start()

        hashrateThread = Thread{
            while (true){
                updateHashrateCurve()
                Thread.sleep(900000)
            }
        }

        //hashrateThread.start()
    }
    
    private fun loadInformation() {
        MAText.text = miningAccount

        binanceApi.get(mapOf("algo" to "ethash", "userName" to miningAccount), BinanceAPI.WORKERS_DATA){
            Log.i("API", it.toString())
            val data = it.getJSONObject("data")
            val workerDatas = data.getJSONArray("workerDatas")
            getWorkers(workerDatas)
            Log.i("API", workerDatas.toString())
            val avgHashRate = getMaxHashate(workerDatas)
            val dayHashRate = getDayHashrate(workerDatas)
            var numOfWorkers = 0
            for (i in 0 until workerDatas.length()){
                val worker = workerDatas.getJSONObject(i)
                if (worker.getInt("status") == 1) numOfWorkers += 1
            }

            runOnUiThread {
                fifteenHashRate.text = avgHashRate
                twentyFourHashrate.text = dayHashRate
                numOfWorkersView.text = numOfWorkers.toString()
            }

        }

        this.ethPrice = 0.toDouble()
        binanceApi.get(mapOf(), BinanceAPI.PRICE_ETH_USDT, simpleRequest = true){
            this.ethPrice = it.getString("price").toDouble()
        }

        binanceApi.get(mapOf("algo" to "ethash", "userName" to miningAccount), BinanceAPI.STATISTIC_LIST){
            Log.i("API", it.toString())
            val data = it.getJSONObject("data")
            val profitToday = data.getJSONObject("profitToday")
            val profitYesterday = data.getJSONObject("profitYesterday")
            val ethQuantity = "0.00000000"

            if (profitToday.has("ETH")){
                var ethQuantityT = profitToday.getString("ETH")

                if (ethQuantityT.length < 10){
                    for (i in ethQuantityT.length until 10)
                        ethQuantityT += "0"
                }
                else {
                    ethQuantityT = ethQuantityT.substring(0,10)
                }

                while (ethPrice == 0.toDouble()){
                    Thread.sleep(100)
                }

                val ethereumPrice = "$ethPrice" +"$"
                val usdPriceETH = (ethQuantityT.toFloat() * ethPrice).toString().substring(0, 4)



                runOnUiThread {
                    //receivedForToday.text = "$ethQuantityT ETH ~ $usdPriceETH$"
                    //ethereumCost.text = ethereumPrice
                }
            }
            else{
                runOnUiThread {

                    receivedForToday.text = "$ethQuantity ETH"

                }
            }

            if (profitYesterday.has("ETH")){
                var ethQuantityY = profitYesterday.getString("ETH")

                if (ethQuantityY.length < 10){
                    for (i in ethQuantityY.length until 10)
                        ethQuantityY += "0"
                }
                else {
                    ethQuantityY = ethQuantityY.substring(0,10)
                }

                while (ethPrice == 0.toDouble()){
                    Thread.sleep(100)
                }
                val usdPriceETH = (ethQuantityY.toFloat() * ethPrice).toString().substring(0, 4)

                runOnUiThread {
                    //receivedForYesterday.text = "$ethQuantityY ETH ~ $usdPriceETH$"
                }
            }
            else {
                //receivedForYesterday.text = "$ethQuantity ETH"
            }

        }

    }

    private fun updateHashrateCurve(){
        while (workersList.isNullOrEmpty()){
            Thread.sleep(1000)
            }

        hashrateWorkers = JSONArray()
        hashrateWorkersPut = 0

        workersList.forEach {
            binanceApi.get(mapOf("algo" to "ethash", "userName" to miningAccount, "workerName" to it), BinanceAPI.WORKER_DETAIL){
                Log.i("API", it.toString())

                val data = it.getJSONArray("data").getJSONObject(0)
                val hashrateDatas = data.getJSONArray("hashrateDatas")

                Log.i("API", hashrateDatas.toString())
                if (hashrateWorkers.length() == 0){
                    for (i in 0 until hashrateDatas.length()){
                        val data = hashrateDatas.getJSONObject(i)
                        hashrateWorkers.put(data)
                    }
                } else {
                    for (i in 0 until hashrateDatas.length()){
                        val data = hashrateDatas.getJSONObject(i)
                        val hashrate = data.getString("hashrate")
                        val reject = data.getDouble("reject")
                        val data2 = hashrateWorkers.getJSONObject(i)
                        val hashrate2 = data2.getString("hashrate")
                        val reject2 = data2.getDouble("reject")
                        data2.put("hashrate", (hashrate.toInt()+hashrate2.toInt()).toString())
                        data2.put("reject", (reject+reject2).toString())
                    }
                }
                hashrateWorkersPut += 1

            }
        }


        while (hashrateWorkersPut != workersList.size){
            Thread.sleep(1000)
        }

        val graphData = getGraphData(hashrateWorkers)

        runOnUiThread {
            hashrateCurve.visibility = View.GONE
            hashrateCurve.setData(graphData)
            hashrateCurve.visibility = View.VISIBLE
    }
    }

    private fun getGraphData(hashrateDatas: JSONArray): ArrayList<GraphLine> {

        val array = ArrayList<GraphLine>()

        val line1 = GraphLine(name = "hashrate", color = getColor(R.color.yellow_light))
        val line2 = GraphLine(name = "reject", color = Color.GREEN, maxOfGraph = 100f)

        val arrayItems1 = ArrayList<GraphItem>()
        val arrayItems2 = ArrayList<GraphItem>()

        for (i in 0 until hashrateDatas.length()){
            val data = hashrateDatas.getJSONObject(i)
            val time = data.getLong("time")
            var hashrate = data.getString("hashrate").toFloat()
            hashrate /= 1000000f
            var reject = (data.getDouble("reject") * 100).toString()
            if (reject.length < 4){
                reject = "0.00"
            }

            reject = reject.substring(0, 4)

            val item1 = GraphItem(time, hashrate)
            val item2 = GraphItem(time, reject.toFloat())
            arrayItems1.add(item1)
            arrayItems2.add(item2)
        }

        line1.setItems(arrayItems1)
        line2.setItems(arrayItems2)
        array.add(line1)
        array.add(line2)
        return array
    }

    private fun getWorkers(workerDatas: JSONArray) {
        workersList = arrayListOf()
        for (workerID in 0 until workerDatas.length()){
            val worker = workerDatas.getJSONObject(workerID)
            workersList.add(worker.getString("workerName"))
        }
    }

    private fun getDayHashrate(workerDatas: JSONArray): String {
        val hashRates = arrayListOf<Double>()
        for (workerID in 0 until workerDatas.length()){
            val worker = workerDatas.getJSONObject(workerID)
            var workerHashRate = worker.getInt("dayHashRate")
            hashRates.add(workerHashRate.toDouble())
        }

        val day = hashRates.sum() / 1000000f
        Log.i("API", day.toString())
        return day.toString().substring(0, 5)+" MH/s"
    }

    private fun getMaxHashate(workerDatas: JSONArray): String {
        val hashRates = arrayListOf<Int>()
        for (workerID in 0 until workerDatas.length()){
            val worker = workerDatas.getJSONObject(workerID)
            val workerHashRate = worker.getInt("hashRate")
            hashRates.add(workerHashRate)
        }

        Log.i("API", hashRates.toString())

        val avg = hashRates.sum() / 1000000f
        Log.i("API", avg.toString())
        if (avg == 0f) return "0.0 MH/s"
        else return avg.toString().substring(0, 5)+" MH/s"
    }

    private fun setupComponents() {
        storage = Storage(this)
        homeFragment = HomeStats(this)
        //MAText = findViewById(R.id.mining_account_text)
        //miningAccount = storage.getValue("miningAccount")
        //fifteenHashRate = findViewById(R.id.fift_text)
        //twentyFourHashrate = findViewById(R.id.twenty_text)
        //numOfWorkersView = findViewById(R.id.workers_text)
        //binanceApi = BinanceAPI(this)
        mainNavbar = findViewById(R.id.main_navbar)
        //switchStatusForm = findViewById(R.id.switch_status_form)
        //statusForm = findViewById(R.id.status_form)
        //statusBlock = findViewById(R.id.status_block)
        //receivedForToday = findViewById(R.id.received_for_today)
        //scrollView = findViewById(R.id.scrollView)
        //hashrateCurve = findViewById(R.id.hashrate_curve)
        //curveBlock = findViewById(R.id.curve_block)
        //switchCurveForm = findViewById(R.id.switch_hashrate_form)
        //curveForm = findViewById(R.id.curve_form)
        //ethereumCost = findViewById(R.id.ethereum_cost)
        //receivedForYesterday = findViewById(R.id.received_for_yesterday)
        //autoupdate = Autoupdate(this)
        //updateAllBlocks = findViewById(R.id.update_all_block)
        //updateEmptySpace = findViewById(R.id.update_block_empty_space)
        //updateButton = findViewById(R.id.update_button)
        //updateVersion = findViewById(R.id.update_new_version)
        //spin = findViewById(R.id.main_spin)
        //spinEmptySpace = findViewById(R.id.spin_background)

        //autoupdate.runCheck()
    }

    private fun setComponentsListener(){
        switchStatusForm.setOnClickListener{

            val height_curve = curveBlock.layoutParams.height
            val width_curve = curveBlock.layoutParams.width


            if (statusIsOpen){
                statusIsOpen = false

                Utils.getValueAnimation(0f, -25f){
                    val value = it.animatedValue as Int
                    switchStatusForm.rotation = value.toFloat()
                    switchStatusForm.requestLayout()
                }.start()

                Utils.getValueAnimation(250f, 25f, function = {
                    val value = it.animatedValue as Int
                    statusBlock.layoutParams.height = value
                    statusBlock.requestLayout()
                }).start()

                Utils.getValueAnimation(curveTopMarginStatusOpen, curveTopMarginStatusClose){
                    val value = it.animatedValue.toString()
                    val params = RelativeLayout.LayoutParams(width_curve, height_curve)
                    params.marginStart = Utils.dps(30f)
                    params.marginEnd = Utils.dps(30f)
                    params.topMargin = value.toInt()
                    curveBlock.layoutParams = params
                    curveBlock.requestLayout()
                }.start()


            }
            else{
                statusIsOpen = true

                Utils.getValueAnimation(-25f, 0f){
                    val value = it.animatedValue as Int
                    switchStatusForm.rotation = value.toFloat()
                    switchStatusForm.requestLayout()
                }.start()


                Utils.getValueAnimation(25f, 250f, function = {
                    val value = it.animatedValue as Int
                    statusBlock.layoutParams.height = value
                    statusBlock.requestLayout()
                }).start()

                Utils.getValueAnimation(curveTopMarginStatusClose, curveTopMarginStatusOpen, function = {
                    val value = it.animatedValue.toString()
                    val params = RelativeLayout.LayoutParams(width_curve, height_curve)
                    params.marginStart = Utils.dps(30f)
                    params.marginEnd = Utils.dps(30f)
                    params.topMargin = value.toInt()
                    curveBlock.layoutParams = params
                    curveBlock.requestLayout()
                }).start()

            }

        }

        switchCurveForm.setOnClickListener {

            if (curveIsOpen){
                curveIsOpen = false
                curveForm.visibility = View.GONE

                Utils.getValueAnimation(0f, -25f){
                    val value = it.animatedValue as Int
                    switchCurveForm.rotation = value.toFloat()
                    switchCurveForm.requestLayout()
                }.start()


                Utils.getValueAnimation(250f, 25f, function = {
                    val value = it.animatedValue as Int
                    curveBlock.layoutParams.height = value
                    curveBlock.requestLayout()
                }).start()

            }
            else{
                curveIsOpen = true


                Utils.getValueAnimation(-25f, 0f){
                    val value = it.animatedValue as Int
                    switchCurveForm.rotation = value.toFloat()
                    switchCurveForm.requestLayout()
                }.start()

                Utils.getValueAnimation(25f, 250f, function = {
                    val value = it.animatedValue as Int

                    if (value == Utils.dps(250f)){
                        curveForm.visibility = View.VISIBLE
                    }

                    curveBlock.layoutParams.height = value
                    curveBlock.requestLayout()
                }).start()

            }

        }

        updateEmptySpace.setOnClickListener{

        }

        updateButton.setOnClickListener{
            autoupdate.startUpdate()
            updateAllBlocks.visibility = View.GONE
            spin.visibility = View.VISIBLE
        }

        spinEmptySpace.setOnClickListener {

        }

    }

}