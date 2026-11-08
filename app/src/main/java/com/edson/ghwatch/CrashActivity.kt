package com.edson.ghwatch

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "Sem detalhes do erro"

        val text = TextView(this).apply {
            this.text = trace
            textSize = 11f
            setTextColor(Color.WHITE)
            setTextIsSelectable(true)
            setPadding(24, 24, 24, 24)
        }
        val copy = Button(this).apply {
            this.text = "Copiar erro"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("erro", trace))
                Toast.makeText(this@CrashActivity, "Copiado", Toast.LENGTH_SHORT).show()
            }
        }
        val scroll = ScrollView(this).apply { addView(text) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(0, 96, 0, 0)
            addView(copy)
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
    }
}
