package com.invisibles.minestats

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.json.JSONArray
import java.util.ArrayList

class HomeStats(private var appContext: Context) : Fragment(R.layout.fragment_home_stats) {

    private lateinit var miningAccountText: TextView
    private lateinit var fiftHashrateText: TextView
    private lateinit var twentyHashrate: TextView
    private lateinit var onlineWorkersText: TextView
    private lateinit var miningAccount: String
    private lateinit var storage: Storage
    private lateinit var binanceApi: BinanceAPI
    private lateinit var mainView: View
    private lateinit var fiftBlock: RelativeLayout
    private lateinit var twentyBlock: RelativeLayout
    private lateinit var workersBlock: RelativeLayout
    private lateinit var selectedBlock: RelativeLayout

    private var workersList = arrayListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainView = inflater.inflate(R.layout.fragment_home_stats, container, false)
        initComponents()
        initListeners()
        updateComponentsInfo()
        return mainView
    }

    private fun initListeners() {
        fiftBlock.setOnClickListener { changeColorBlock(it) }
        twentyBlock.setOnClickListener { changeColorBlock(it) }
        workersBlock.setOnClickListener { changeColorBlock(it) }

    }

    private fun changeColorBlock(changedView: View){
        selectedBlock.background = ContextCompat.getDrawable(appContext, R.drawable.other_block_radius)
        changeColorTextInBlock(R.color.text_gray_black)
        selectedBlock = changedView as RelativeLayout
        selectedBlock.background = ContextCompat.getDrawable(appContext, R.drawable.accent_block_radius)
        changeColorTextInBlock(R.color.white)

    }

    private fun changeColorTextInBlock(color: Int){
        for (i in 0 until 2){
            val child = selectedBlock.getChildAt(i) as TextView
            val textColor = ResourcesCompat.getColor(resources, color, null)
            child.setTextColor(textColor)
        }
    }

    private fun updateComponentsInfo() {
        val mainBlocks = Thread{
            while (true) {
                updateMainBlocks()
                Thread.sleep(1 * 60 * 1000)
            }
        }

        mainBlocks.start()
    }

    private fun updateMainBlocks(){
        Log.i("Main", miningAccount)
        miningAccountText.text = miningAccount

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

            activity!!.runOnUiThread {
                fiftHashrateText.text = avgHashRate
                twentyHashrate.text = dayHashRate
                onlineWorkersText.text = numOfWorkers.toString()
            }

        }

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

    private fun initComponents() {
        storage = Storage(appContext)
        binanceApi = BinanceAPI(appContext)
        miningAccountText = findViewByID(R.id.mining_account_text) as TextView
        fiftHashrateText = findViewByID(R.id.fift_text) as TextView
        twentyHashrate = findViewByID(R.id.twenty_text) as TextView
        onlineWorkersText = findViewByID(R.id.workers_text) as TextView
        miningAccount = storage.getValue("miningAccount")

        //Init Info Blocks
        fiftBlock = findViewByID(R.id.fift_block) as RelativeLayout
        twentyBlock = findViewByID(R.id.twenty_block) as RelativeLayout
        workersBlock = findViewByID(R.id.workers_block) as RelativeLayout
        selectedBlock = fiftBlock
    }

    private fun findViewByID(id: Int): View? {
        val el = mainView.findViewById<View>(id)
        return el
    }
}