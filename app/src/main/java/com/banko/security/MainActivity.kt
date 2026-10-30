package com.banko.security

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.*
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.concurrent.thread

class MainActivity : Activity() {

private lateinit var status: TextView
private lateinit var results: TextView
private lateinit var scanButton: Button

private val background = Color.rgb(10, 15, 24)
private val panel = Color.rgb(22, 31, 45)
private val green = Color.rgb(75, 235, 177)

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(22, 28, 22, 20)
        setBackgroundColor(background)
    }

    val title = TextView(this).apply {
        text = "BANKO"
        textSize = 34f
        setTextColor(green)
        typeface = Typeface.DEFAULT_BOLD
    }

    val subtitle = TextView(this).apply {
        text = "ROOTED NETWORK SECURITY"
        textSize = 12f
        setTextColor(Color.LTGRAY)
        setPadding(0, 4, 0, 22)
    }

    status = TextView(this).apply {
        text = "Checking root access..."
        textSize = 16f
        setTextColor(Color.WHITE)
        setPadding(14, 18, 14, 18)
        setBackgroundColor(panel)
    }

    val heading = TextView(this).apply {
        text = "NETWORK TOOLS"
        textSize = 14f
        setTextColor(green)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 24, 0, 10)
    }

    scanButton = Button(this).apply {
        text = "Scan My Wi-Fi"
        isAllCaps = false
        setOnClickListener { scanNetwork() }
    }

    val note = TextView(this).apply {
        text = "Use only on networks you own or have permission to test."
        textSize = 12f
        setTextColor(Color.LTGRAY)
        setPadding(0, 12, 0, 14)
    }

    results = TextView(this).apply {
        text = "Your scan results will appear here."
        textSize = 14f
        setTextColor(Color.WHITE)
        setPadding(14, 16, 14, 16)
        setBackgroundColor(panel)
    }

    layout.addView(title)
    layout.addView(subtitle)
    layout.addView(status)
    layout.addView(heading)
    layout.addView(scanButton)
    layout.addView(note)
    layout.addView(results)

    setContentView(ScrollView(this).apply {
        addView(layout)
    })

    thread {
        val rootAvailable = checkRoot()

        runOnUiThread {
            status.text = if (rootAvailable) {
                "ROOT ACCESS: Available"
            } else {
                "ROOT ACCESS: Not detected"
            }

            status.setTextColor(
                if (rootAvailable) green else Color.rgb(255, 170, 90)
            )

            scanButton.isEnabled = rootAvailable
        }
    }
}

private fun checkRoot(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec(
            arrayOf("su", "-c", "id")
        )
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor() == 0 && output.contains("uid=0")
    } catch (e: Exception) {
        false
    }
}

private fun scanNetwork() {
    scanButton.isEnabled = false
    results.text = "Scanning local network..."

    thread {
        val report = StringBuilder()

        try {
            val wifi = NetworkInterface.getNetworkInterfaces()
                .toList()
                .firstOrNull {
                    it.isUp &&
                    !it.isLoopback &&
                    it.name.startsWith("wlan")
                }

            val address = wifi?.interfaceAddresses
                ?.firstOrNull { it.address is Inet4Address }

            val ip = address?.address?.hostAddress

            if (ip == null) {
                report.append(
                    "Wi-Fi IPv4 address not found. Connect to Wi-Fi and try again."
                )
            } else {
                report.append("Your local IP: $ip\n\n")
                report.append(
                    "This first version detects the local address only.\n"
                )
                report.append(
                    "Full device discovery will be added in a later version."
                )
            }
        } catch (e: Exception) {
            report.append("Network check failed: ${e.message}")
        }

        runOnUiThread {
            results.text = report.toString()
            scanButton.isEnabled = checkRoot()
        }
    }
}

}
