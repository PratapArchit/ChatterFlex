package com.base.chatterflex

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val chatterText = findViewById<TextView>(R.id.chatterText)
        val flexText = findViewById<TextView>(R.id.flexText)

        // Apply gradient to only the "Chatter" part
        chatterText.post {
            val paint = chatterText.paint
            val width = paint.measureText("Chatter")

            val shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    0xFF3E92FF.toInt(), // Blue
                    0xFF8A2BE2.toInt()  // Purple
                ),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            chatterText.invalidate()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, WifiSelectionActivity::class.java))
            finish()
        }, 2000)
    }
}
