package com.example.hello_world

import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private var mDisplay: Display?= null
    private var dataStreamer: DataStreamer?= null
    private var dataReceiver: DataReceiver?= null
    private var stop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataStreamer = DataStreamer(applicationContext)
        dataReceiver = MyDataReceiver(applicationContext)

        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        mDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)
    }

    fun startReading(view: View) {
        dataReceiver?.let { dataStreamer?.Start(it) }

        lifecycleScope.launch {
            val textView = findViewById<TextView>(R.id.mainText)

            while (!stop) {
                val text = """
                    acc-x: ${dataReceiver?.accData?.x}
                    acc-y: ${dataReceiver?.accData?.y}
                    acc-z: ${dataReceiver?.accData?.z}
                    ori-az: ${dataReceiver?.oriData?.azimuth}
                    ori-pit: ${dataReceiver?.oriData?.pitch}
                    ori-toll: ${dataReceiver?.oriData?.roll}
                """.trimIndent()
                println(text)
                textView.text = text
                delay(1000)
            }
        }
        println("Wtf")
    }

    fun stopReading(view: View) {
        stop = true
        dataStreamer?.Stop()
    }
}