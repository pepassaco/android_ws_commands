package com.example.wscommands

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : ComponentActivity() {
    private lateinit var hostInput: TextInputEditText
    private lateinit var portInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)

        // Load current settings
        val prefs = getSharedPreferences("WebSocketSettings", Context.MODE_PRIVATE)
        hostInput.setText(prefs.getString("host", "10.0.2.2"))
        portInput.setText(prefs.getString("port", "8080"))

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            if (saveSettings()) {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun saveSettings(): Boolean {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        if (host.isEmpty() || port.isEmpty()) {
            return false
        }

        val prefs = getSharedPreferences("WebSocketSettings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("host", host)
            putString("port", port)
            apply()
        }
        return true
    }
}