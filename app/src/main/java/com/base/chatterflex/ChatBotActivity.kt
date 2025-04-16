package com.base.chatterflex

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatBotActivity : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout
    private lateinit var inputBox: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var tvChatterFlex: TextView
    private val client = OkHttpClient()

    private val GEMINI_API_KEY = "AIzaSyBL4aiEHR2mBeelotnAujMH8MlMCSYxUo0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)

        tvChatterFlex = findViewById(R.id.tvChatterFlex)
        tvChatterFlex.post {
            applyGradientToSubstring(tvChatterFlex, "Chatter")
        }

        messageContainer = findViewById(R.id.messageContainer)
        inputBox = findViewById(R.id.inputBox)
        sendButton = findViewById(R.id.sendButton)

        // Initial greeting
        addMessage("Hi there! I'm ChatterFlex Bot. Ask me anything!", isUser = false)

        sendButton.setOnClickListener {
            val userMessage = inputBox.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, true)
                inputBox.text.clear()
                fetchGeminiReply(userMessage)
            }
        }
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val view = layoutInflater.inflate(R.layout.item_message, null)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvtime = view.findViewById<TextView>(R.id.tvTime)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        tvtime.text = currentTime
        tvMessage.text = message

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16, 16, 16, 16)
            gravity = if (isUser) Gravity.START else Gravity.END
        }

        tvMessage.setBackgroundResource(if (isUser) R.drawable.bg_msg_left else R.drawable.bg_msg_right)
        messageContainer.addView(view, params)
    }

    private fun fetchGeminiReply(prompt: String) {
        Thread {
            try {
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$GEMINI_API_KEY"

                val json = JSONObject()
                val contents = JSONArray()
                val content = JSONObject()
                val parts = JSONArray()

                val systemMessage = """
                You are Chatter Flex, a helpful, friendly, and informative assistant. 
                You explain what Chatter Flex is, how it works, and you engage in casual conversation. 
                You never refer to Gemini or Google. Avoid formatting. Speak clearly and simply.
                
                User: $prompt
            """.trimIndent()

                parts.put(JSONObject().put("text", systemMessage))
                content.put("parts", parts)
                contents.put(content)

                json.put("contents", contents)

                val body = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    json.toString()
                )

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    runOnUiThread {
                        addMessage("Error: ${response.code} - ${response.message}", false)
                    }
                    return@Thread
                }

                val responseJson = JSONObject(responseBody)
                val candidateText = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                runOnUiThread {
                    addMessage(candidateText.trim(), isUser = false)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    addMessage("Failed to connect to Gemini: ${e.message}", isUser = false)
                }
            }
        }.start()
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
