package com.base.chatterflex

import android.app.Activity
import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ConversationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var messageContainer: LinearLayout
    private lateinit var tvHeader: TextView
    private lateinit var tvChatterFlex: TextView
    private lateinit var btnMic: ImageView
    private lateinit var bbtn: ImageView
    private lateinit var status: TextView
    private var isSpeakingOrListening = false


    private val REQUEST_SPEECH_INPUT = 200
    private val apiUrl = "https://chatterflex.onrender.com/getmessage"

    private var lastBotMessage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        tvHeader = findViewById(R.id.tvHeader)
        tvChatterFlex = findViewById(R.id.tvChatterFlex)
        messageContainer = findViewById(R.id.messageContainer)
        btnMic = findViewById(R.id.btnMic)
        bbtn = findViewById(R.id.navMenu)
        status = findViewById(R.id.status)

        // Gradient for "Chatter"
        tvChatterFlex.post {
            applyGradientToSubstring(tvChatterFlex, "Chatter")
            applyGradientToSubstring(tvHeader, "Conversation")
        }

        bbtn.setOnClickListener{
            finish()
        }

        // TTS initialization
        tts = TextToSpeech(this, this)

        // Initial messages
        val type = intent.getStringExtra("type")

        type?.let {
            if(it == "true"){
                startVoiceInput()
            }else{
                fetchFromApi()
            }
        }

        // Voice input button
        btnMic.setOnClickListener {
            startVoiceInput()
        }

        // Start periodic API polling
        startPollingApi()
    }

    private fun startPollingApi() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isSpeakingOrListening) {
                    fetchFromApi()
                }
                handler.postDelayed(this, 7000)
            }
        }, 0)
    }

    private fun fetchFromApi() {
        runOnUiThread {
            status.visibility = View.VISIBLE
            status.text = "Detecting Gestures......"
        }

        Thread {
            try {
                val request = Request.Builder().url(apiUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@Thread
                    val json = JSONObject(responseBody)
                    val message = json.getString("message")

                    if (message != lastBotMessage) {
                        lastBotMessage = message
                        runOnUiThread {
                            addMessage(message, isUser = false)
                            speakOut(message)
                            status.visibility = View.GONE
                        }
                    } else {
                        runOnUiThread {
                            status.text = ""
                        }
                    }
                } else {
                    runOnUiThread {
                        status.text = "Failed to fetch message"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "API Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    status.text = "Error fetching gesture"
                }
            }
        }.start()
    }

    private fun startVoiceInput() {
        runOnUiThread {
            status.visibility = View.VISIBLE
            status.text = "Listening to user..."
        }
        isSpeakingOrListening = true


        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")

        try {
            startActivityForResult(intent, REQUEST_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            runOnUiThread {
                status.text = "Speech recognition not available"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SPEECH_INPUT && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                val spokenText = result[0]
                addMessage(spokenText, isUser = true)
            }
        }
        isSpeakingOrListening = false
        status.visibility = View.GONE
    }


    private fun addMessage(message: String, isUser: Boolean) {
        val view = layoutInflater.inflate(R.layout.item_message, null)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)

        tvMessage.text = message

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        tvTime.text = currentTime

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16, 16, 16, 16)
            gravity = if (isUser) android.view.Gravity.START else android.view.Gravity.END
        }

        tvMessage.setBackgroundResource(if (isUser) R.drawable.bg_msg_left else R.drawable.bg_msg_right)
        messageContainer.addView(view, params)
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            } else {
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakOut(text: String) {
        if (isTtsReady) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                @Suppress("DEPRECATION")
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
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
                tp.shader = LinearGradient(0f, 0f, width, 0f, startColor, endColor, Shader.TileMode.CLAMP)
            }
        }

        val spannable = SpannableString(fullText)
        spannable.setSpan(span, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannable, TextView.BufferType.SPANNABLE)
    }
}
