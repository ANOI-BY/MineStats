package com.invisibles.minestats

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode

private const val REQUEST_CODE = 101

class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var btnManual: Button
    private lateinit var welcomeForm: RelativeLayout
    private lateinit var qrcodeScanner: CodeScannerView
    private lateinit var codeScanner: CodeScanner
    private lateinit var qrcodeForm: RelativeLayout
    private lateinit var qrcodeBtnClose: Button
    private lateinit var spinLoader: ConstraintLayout
    private lateinit var manualBlock: RelativeLayout
    private lateinit var manualBottomBlock: RelativeLayout
    private lateinit var manualBlockSpaceEmpty: RelativeLayout
    private lateinit var manualCode: EditText
    private lateinit var manualBtnAuth: Button

    private var isScanning = false
    private var isSpinEnabled = false
    private var isModalOpen = false

    private val permissions = arrayOf(android.Manifest.permission.CAMERA,
        android.Manifest.permission.REQUEST_INSTALL_PACKAGES,
        android.Manifest.permission.REQUEST_DELETE_PACKAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.ACCESS_MEDIA_LOCATION,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAPIKeys()

        setupComponents()
        setupListener()
        setupPermissions()
    }

    private fun checkAPIKeys() {

        val storage = Storage(this)
        val pAPI = storage.getValue("publicAPI")
        val sAPI = storage.getValue("secretAPI")
        val mineAccount = storage.getValue("miningAccount")
        val isCorrect = storage.getValue("isCorrect").toBoolean()

        if (isCorrect){
            val intent = Intent(this, MainMenu::class.java)
            startActivity(intent)
            finish()
            return
        }
        Log.i("API", "pAPI: ${pAPI}\nsAPI: ${sAPI}\nmACC: ${mineAccount}")

        if (pAPI == Storage.emptyValue && sAPI == Storage.emptyValue && mineAccount == Storage.emptyValue){
            return
        }

        val api = BinanceAPI(this)
        api.get(mapOf("algo" to "ethash", "userName" to mineAccount), BinanceAPI.WORKERS_DATA){
            if (it.getInt("code") == BinanceAPI.SUCCESS_CODE){
                val intent = Intent(this, MainMenu::class.java)
                storage.writeValue("isCorrect", "true")
                startActivity(intent)
                finish()
            } else{
                runOnUiThread {
                    Toast.makeText(this, "invalid data", Toast.LENGTH_SHORT).show()
                    if (isSpinEnabled) spin()
                    qrcodeForm.visibility = View.GONE
                    welcomeForm.visibility = View.VISIBLE
                    isScanning = false
                }
            }
        }
    }

    private fun writeAPIData(qrcodeDecode: String){

        val arrayString = qrcodeDecode.split('.')
        if (arrayString.size == 3){
            val pAPI = arrayString[0]
            val sAPI = arrayString[1]
            val miningAccount = arrayString[2]

            val storage = Storage(this)

            storage.writeValue("publicAPI", pAPI)
            storage.writeValue("secretAPI", sAPI)
            storage.writeValue("miningAccount", miningAccount)

            checkAPIKeys()

        }
        else{
            spin()
            codeScanner.startPreview()
        }
    }

    private fun setupComponents(){

        btnScan = findViewById(R.id.btn_scan)
        btnManual = findViewById(R.id.btn_manual)
        welcomeForm = findViewById(R.id.welcome_form)
        qrcodeScanner = findViewById(R.id.qrcode_scaner)
        codeScanner = CodeScanner(this, qrcodeScanner)
        qrcodeForm = findViewById(R.id.qrcode_form)
        qrcodeBtnClose = findViewById(R.id.qrcode_close)
        spinLoader = findViewById(R.id.welcome_spin)
        manualBlock = findViewById(R.id.manual_md_block)
        manualBottomBlock = findViewById(R.id.manual_block)
        manualBlockSpaceEmpty = findViewById(R.id.manual_block_empty_space)
        manualCode = findViewById(R.id.manual_code)
        manualBtnAuth = findViewById(R.id.manual_send_code)

    }

    private fun setupListener(){

        btnScan.setOnClickListener {

            qrcodeForm.visibility = View.VISIBLE
            welcomeForm.visibility = View.GONE
            isScanning = true
            codeScanner.startPreview()
            codeScaner()

        }

        qrcodeScanner.setOnClickListener {
            codeScanner.startPreview()
        }

        qrcodeBtnClose.setOnClickListener {

            qrcodeForm.visibility = View.GONE
            welcomeForm.visibility = View.VISIBLE
            isScanning = false
        }

        btnManual.setOnClickListener{
            if (isModalOpen){
                isModalOpen = false

                manualBlockSpaceEmpty.startAnimation(Utils.getAlphaAnimation(1f, 0f))
                manualBlockSpaceEmpty.visibility = View.GONE


                Utils.getValueAnimation(200f, 0f){
                    val value = it.animatedValue as Int
                    manualBottomBlock.layoutParams.height = value
                    manualBottomBlock.requestLayout()
                }.start()

            } else{
                isModalOpen = true
                manualBlockSpaceEmpty.visibility = View.VISIBLE

                manualBlockSpaceEmpty.startAnimation(Utils.getAlphaAnimation(0f, 1f))
                Utils.getValueAnimationFloat(1f, 0f){
                    val  value = it.animatedValue as Float
                    manualBlockSpaceEmpty.alpha = value
                    manualBlockSpaceEmpty.requestLayout()
                }.start()

                Utils.getValueAnimation(0f, 200f){
                    val value = it.animatedValue as Int
                    manualBottomBlock.layoutParams.height = value
                    manualBottomBlock.requestLayout()
                }.start()
            }
        }

        manualBlockSpaceEmpty.setOnClickListener {
            isModalOpen = false
            manualBlockSpaceEmpty.visibility = View.GONE
            Utils.getValueAnimation(200f, 0f){
                val value = it.animatedValue as Int
                manualBottomBlock.layoutParams.height = value
                manualBottomBlock.requestLayout()
            }.start()
        }

        manualBottomBlock.setOnClickListener{

        }

        manualBtnAuth.setOnClickListener {
            val text = manualCode.text
            if (text.isNullOrEmpty()){
                manualCode.setError("The field cannot be empty")
            } else{

                Thread {

                    runOnUiThread {
                        spin()
                    }

                    val res = khttp.get("https://invisibles.space/api/mConnect/get/?code=$text")

                    val json = res.jsonObject

                    if (json.has("ERROR")){
                        runOnUiThread {
                            manualCode.setError("invalid code")
                            spin()
                        }

                    } else {
                        val storage = Storage(this)
                        storage.writeValue("publicAPI", json.getString("publicAPI"))
                        storage.writeValue("secretAPI", json.getString("privateAPI"))
                        storage.writeValue("miningAccount", json.getString("miningAccount"))
                        checkAPIKeys()
                    }

                }.start()

            }
        }

    }

    private fun codeScaner(){

        codeScanner.apply {

            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.CONTINUOUS
            isAutoFocusEnabled = true

            decodeCallback = DecodeCallback {
                runOnUiThread {
                    spin()
                    writeAPIData(it.text)
                    codeScanner.stopPreview()
                }
            }

            errorCallback = ErrorCallback {
                Log.e("Main", "Camera initialization error: ${it.message}")
            }

        }

    }

    private fun setupPermissions(){

        permissions.forEach { permissionClass ->

            val permission = ContextCompat.checkSelfPermission(this,
                permissionClass)

            if (permission != PackageManager.PERMISSION_GRANTED) makePermissionRequest()

        }

    }

    private fun makePermissionRequest() {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
    }

    private fun spin(){
        when (isSpinEnabled){
            true -> {
                spinLoader.visibility = View.GONE
                isSpinEnabled = false
            }
            false -> {
                spinLoader.visibility = View.VISIBLE
                isSpinEnabled = true
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        var showErrorMessage = false
        when(requestCode) {
            REQUEST_CODE -> {

                for (i in permissions.indices){
                    val permission = permissions[i]
                    val grantResult = grantResults[i]


                    when (permission){
                        android.Manifest.permission.CAMERA -> { if(!checkPermision(grantResult)) { showErrorMessage = true; Log.i("MAIN", "CAMERA") }}
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> { if(!checkPermision(grantResult)) { showErrorMessage = true; Log.i("MAIN", "WRITE_STORAGE") } }
                        android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> { if(!checkPermision(grantResult)) { showErrorMessage = true; Log.i("MAIN", "INSTALL") } }
                        android.Manifest.permission.REQUEST_DELETE_PACKAGES -> { if(!checkPermision(grantResult)) { showErrorMessage = true; Log.i("MAIN", "DELETE") } }
                        android.Manifest.permission.INTERNET -> { if(!checkPermision(grantResult)) { showErrorMessage = true; Log.i("MAIN", "INTERNET") } }
                    }

                    //if (showErrorMessage) Toast.makeText(this, "Give you the necessary permissions to make the app work correctly!", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }

    private fun checkPermision(grandResult: Int) = grandResult == PackageManager.PERMISSION_GRANTED

    override fun onBackPressed() {
        if (isScanning){
            qrcodeForm.visibility = View.GONE
            welcomeForm.visibility = View.VISIBLE
            isScanning = false
        }
        else{
            super.onBackPressed()
        }
    }
}