package com.base.chatterflex

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.wifi.WifiManager
import android.view.View

class WifiSelectionActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiListView: ListView
    private lateinit var connectButton: Button
    private var selectedSSID: String? = null
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_selection)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiListView = findViewById(R.id.wifiListView)
        connectButton = findViewById(R.id.connectButton)
        progressBar = findViewById(R.id.progressBar)


        // Apply gradient to "Chatter" in ChatterFlex
        val tvChatterFlex = findViewById<TextView>(R.id.tvChatterFlex)
        val tvDevice = findViewById<TextView>(R.id.tvDevice)
        tvChatterFlex.post {
            applyGradientToSubstring(tvChatterFlex, "Chatter")
            applyGradientToSubstring(tvDevice, "Device")
        }


        // Enable WiFi if disabled
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }


        checkAndRequestPermissions()

        wifiListView.choiceMode = ListView.CHOICE_MODE_SINGLE

        wifiListView.setOnItemClickListener { _, view, position, _ ->
            selectedSSID = wifiListView.getItemAtPosition(position) as String
            wifiListView.setItemChecked(position, true)

            // Highlight selected view
            view.isActivated = true

            // Make connect button purple
            connectButton.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.purple_500)
            )
        }

        connectButton.setOnClickListener {
            runOnUiThread { progressBar.visibility = View.VISIBLE }
            if (selectedSSID != null) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("SSID", selectedSSID)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a network", Toast.LENGTH_SHORT).show()
            }
            runOnUiThread { progressBar.visibility = View.GONE }
        }
    }

    private fun scanForWifi() {
        runOnUiThread { progressBar.visibility = View.VISIBLE }
        try {
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val results = wifiManager.scanResults
                    val ssidList = results.map { it.SSID }
                        .distinct()
                        .filter { it.isNotBlank() }

                    val adapter = ArrayAdapter(
                        context,
                        R.layout.list_item_wifi,
                        R.id.ssidText,
                        ssidList
                    )
                    wifiListView.adapter = adapter
                    runOnUiThread { progressBar.visibility = View.GONE }
                }
            }, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

            wifiManager.startScan()

        } catch (e: SecurityException) {
            Toast.makeText(this, "Security error during WiFi scan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        } else {
            scanForWifi()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanForWifi()
        } else {
            Toast.makeText(this, "Permission denied. Can't scan WiFi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyGradientToSubstring(textView: TextView, targetWord: String) {
        val fullText = textView.text.toString()
        val start = fullText.indexOf(targetWord)
        if (start == -1) return
        val end = start + targetWord.length

        val startColor = ContextCompat.getColor(this, R.color.gradientStart)
        val endColor = ContextCompat.getColor(this, R.color.gradientEnd)

        val span = object : CharacterStyle(), UpdateAppearance {
            override fun updateDrawState(tp: TextPaint) {
                val width = tp.measureText(fullText.substring(start, end))
                tp.shader = LinearGradient(
                    0f, 0f, width, 0f,
                    startColor, endColor,
                    Shader.TileMode.MIRROR
                )
            }
        }

        val spannable = SpannableString(fullText)
        spannable.setSpan(span, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannable, TextView.BufferType.SPANNABLE)
    }
}
