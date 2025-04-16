package com.base.chatterflex

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnSpeech: LinearLayout
    private lateinit var btnListen: LinearLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvChatterFlex: TextView
    private lateinit var chatBotIcon: ImageView
    private lateinit var bbtn: ImageView
    private lateinit var ssid: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvGreeting = findViewById(R.id.tvGreeting)
        tvChatterFlex = findViewById(R.id.tvChatterFlex)
        btnSpeech = findViewById(R.id.btnSpeech)
        btnListen = findViewById(R.id.btnListen)
        chatBotIcon = findViewById(R.id.chatBotIcon)
        bbtn = findViewById(R.id.backbtn)
        ssid = findViewById(R.id.ssidText)

        // Gradient Text
        tvChatterFlex.post { applyGradientToSubstring(tvChatterFlex, "Chatter") }
        tvGreeting.post { applyGradientToSubstring(tvGreeting, "User") }

        btnSpeech.setOnClickListener {
            val intent = Intent(this,ConversationActivity::class.java)
            intent.putExtra("type","false")
            startActivity(intent)
        }

        bbtn.setOnClickListener{
            finish()
        }

        val ssidt = intent.getStringExtra("SSID")

        ssidt?.let {
            ssid.text = ssidt
        }


        btnListen.setOnClickListener {
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra("type","true")
            startActivity(intent)
        }

        chatBotIcon.setOnClickListener {
            startActivity(Intent(this, ChatBotActivity::class.java))
        }

    }

    override fun onInit(status: Int) {

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
                tp.shader = LinearGradient(0f, 0f, width, 0f, startColor, endColor, Shader
                    .TileMode.MIRROR)
            }
        }

        val spannable = SpannableString(fullText)
        spannable.setSpan(span, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannable, TextView.BufferType.SPANNABLE)
    }
}
