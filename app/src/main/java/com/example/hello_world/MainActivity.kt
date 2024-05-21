package com.example.hello_world

import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class MainActivity : ComponentActivity() {
    private var mDisplay: Display?= null
    private var dataStreamer: DataStreamer?= null
    private var dataReceiver: DataReceiver?= null
    private var stop = false
    private var drawer: Drawer?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataStreamer = DataStreamer(applicationContext)
        dataReceiver = MyDataReceiver(applicationContext)

        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        mDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)

        drawer = findViewById(R.id.map)
    }

    fun startReading(view: View) {
        stop = false
        dataReceiver?.let { dataStreamer?.Start(it) }

        lifecycleScope.launch {
            val dataChan = Channel<RelVector>()
            drawer?.StartDrawing(dataChan)

            while (!stop) {
                val text = """
                    acc-x: ${dataReceiver?.accData?.x}
                    acc-y: ${dataReceiver?.accData?.y}
                    acc-z: ${dataReceiver?.accData?.z}
                    ori-az: ${dataReceiver?.oriData?.azimuth}
                    ori-pit: ${dataReceiver?.oriData?.pitch}
                    ori-toll: ${dataReceiver?.oriData?.roll}
                """.trimIndent()

                val accX = dataReceiver?.accData!!.x
                val accY = dataReceiver?.accData!!.y
                val accZ = dataReceiver?.accData!!.z

                val vec = RelVector(accX*100, accY*100, accZ)

                dataChan.send(vec)

                delay(3000)
            }
        }
    }

    fun stopReading(view: View) {
        stop = true
        dataStreamer?.Stop()
        drawer?.EndDrawing()
    }
}