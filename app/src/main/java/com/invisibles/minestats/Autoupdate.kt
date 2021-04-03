package com.invisibles.minestats

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import khttp.get
import java.io.File

class Autoupdate(private val context: Context) {

    private lateinit var localVersion: String
    private lateinit var onlineVersion: String

    private val apiUrl = context.getString(R.string.API_INVISIBLES)+"/api/version/?token=3f630123113c092da7f52b9670f64bb1300c3ec0"

    fun runCheck(){
        getAppVersion()
        Thread{
            checkNewVersions()
        }.start()

    }

    fun update(function: (version: String) -> Unit){
        Thread{
            if (isNewVersion()){
                val version = getOnlineVersion()
                function(version)
            }
        }.start()
    }

    private fun isNewVersion(): Boolean {
        getAppVersion()
        checkNewVersions()
        return localVersion != onlineVersion
    }

    private fun getOnlineVersion(): String {
        return onlineVersion
    }

    fun startUpdate(){
        Thread{
            checkNewVersionsAndUpdate()
        }.start()
    }

    private fun checkNewVersions() {
        val res = get(apiUrl).jsonObject

        if (res.has("version")){
            onlineVersion = res.getString("version")
        }
    }

    private fun getAppVersion(): String {
        localVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        Log.i("AUTOUPDATE", "LOCAL_VERSION: $localVersion")
        return localVersion
    }

    private fun checkNewVersionsAndUpdate(){
        val res = get(apiUrl).jsonObject

        if (res.has("version")){
            onlineVersion = res.getString("version")
            if (localVersion != onlineVersion){
                val url =  context.getString(R.string.API_INVISIBLES) + res.getString("url_for_download")
                downloadNewVersion(url)
            }
        }
    }

    private fun downloadNewVersion(url: String) {
        val filename = "app.apk"
        val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/$filename"
        val file = File(destination)

        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(url))
        request.setDescription("Download")
        request.setTitle("MineStatsDownload")

        request.setDestinationUri(Uri.parse("file://$destination"))

        val manager =  context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID = manager.enqueue(request)

        val onComplete = object: BroadcastReceiver(){
            override fun onReceive(context: Context, intent: Intent){
                val install = Intent(Intent.ACTION_VIEW)
                install.flags += Intent.FLAG_ACTIVITY_NEW_TASK
                install.flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                install.setDataAndType(manager.getUriForDownloadedFile(downloadID), "application/vnd.android.package-archive")
                context.startActivity(install)

                context.unregisterReceiver(this)
            }
        }

        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    }


}