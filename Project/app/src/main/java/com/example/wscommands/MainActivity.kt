package com.example.wscommands

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import java.util.concurrent.TimeUnit
import android.view.View
import android.graphics.Color
import android.content.Context
import android.content.Intent

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var webSocket: WebSocket
    private lateinit var statusIndicator: View
    private lateinit var responseText: TextInputEditText
    private var isConnected = false
    private var pingJob: java.util.Timer? = null
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Settings were saved, reconnect
            stopPing()
            webSocket.cancel()
            setupWebSocket()
            appendToResponse("Reconnecting with new settings...")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusIndicator = findViewById(R.id.statusIndicator)
        responseText = findViewById(R.id.responseText)

        setupWebSocket()
        setupButtons()
        setupReconnectButton()

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupReconnectButton() {
        findViewById<MaterialButton>(R.id.reconnectButton).setOnClickListener {
            stopPing()
            webSocket.cancel() // Close existing connection
            setupWebSocket()   // Create new connection
            appendToResponse("Attempting to reconnect...")
        }
    }

    private fun startPing() {
        stopPing() // Stop any existing ping job
        pingJob = java.util.Timer().apply {
            scheduleAtFixedRate(object : java.util.TimerTask() {
                override fun run() {
                    if (isConnected) {
                        webSocket.send("ping")
                        Log.d("WebSocket", "Ping sent")
                    }
                }
            }, 0, 10000) // 10 seconds interval
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun setupWebSocket() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(getWebSocketUrl())
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connection opened")
                runOnUiThread {
                    isConnected = true
                    statusIndicator.setBackgroundColor(Color.GREEN)
                    appendToResponse("Connected to server")
                    startPing() // Start pinging when connected
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received message: $text")
                if (text != "pong") { // Don't show ping-pong messages in the UI
                    runOnUiThread {
                        appendToResponse("Server: $text")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed", t)
                runOnUiThread {
                    isConnected = false
                    statusIndicator.setBackgroundColor(Color.RED)
                    appendToResponse("Connection failed: ${t.message}")
                    stopPing() // Stop pinging on failure
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Connection closing: $reason")
                runOnUiThread {
                    isConnected = false
                    statusIndicator.setBackgroundColor(Color.RED)
                    appendToResponse("Connection closing: $reason")
                    stopPing() // Stop pinging when closing
                }
            }
        })
    }

    private fun setupButtons() {
        val commands = listOf("cmd1", "cmd2", "cmd3", "cmd4", "cmd5", "cmd6")

        for (i in 1..6) {
            findViewById<MaterialButton>(
                resources.getIdentifier("button$i", "id", packageName)
            ).setOnClickListener { button ->
                if (!isConnected) {
                    appendToResponse("Not connected to server!")
                    return@setOnClickListener
                }

                val command = commands[i-1]
                Log.d("WebSocket", "Sending command: $command")
                val sent = webSocket.send(command)

                if (sent) {
                    appendToResponse("Sent: $command")
                    button.isEnabled = false // Temporarily disable button
                    button.postDelayed({
                        button.isEnabled = true
                    }, 100) // Re-enable after 100ms
                } else {
                    appendToResponse("Failed to send: $command")
                }
            }
        }
    }

    private fun appendToResponse(text: String) {
        Log.d("UI", "Appending text: $text")
        val currentText = responseText.text.toString()
        val newText = (if (currentText.isEmpty()) text else "$currentText\n$text")
            .split("\n")
            .takeLast(5)
            .joinToString("\n")
        responseText.setText(newText)
        responseText.setSelection(newText.length)
    }

    private fun getWebSocketUrl(): String {
        val prefs = getSharedPreferences("WebSocketSettings", Context.MODE_PRIVATE)
        val host = prefs.getString("host", "10.0.2.2") ?: "10.0.2.2"
        val port = prefs.getString("port", "8080") ?: "8080"
        return "ws://$host:$port"
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity destroyed")
    }
}