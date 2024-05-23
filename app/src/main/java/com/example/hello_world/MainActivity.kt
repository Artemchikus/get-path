package com.example.hello_world

import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.math.*
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var mDisplay: Display
    private lateinit var dataStreamer: DataStreamer
    private lateinit var dataReceiver: DataReceiver
    private var stop = false
    private lateinit var drawer: Drawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataStreamer = DataStreamer(applicationContext)
        dataReceiver = MyDataReceiver(applicationContext)

        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        mDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)

        drawer = findViewById(R.id.map)
        Log.i("Shit", "init1")
    }

    fun startReading(view: View) {
        stop = false
        dataReceiver.let { dataStreamer.Start(it) }

        lifecycleScope.launch {
            val dataChan = Channel<RelVector>()
            drawer.StartDrawing(dataChan)

            while (!stop) {
                val text = """
                    acc-x: ${dataReceiver.accData.x}
                    acc-y: ${dataReceiver.accData.y}
                    acc-z: ${dataReceiver.accData.z}
                    ori-az: ${dataReceiver.oriData.azimuth}
                    ori-pit: ${dataReceiver.oriData.pitch}
                    ori-roll: ${dataReceiver.oriData.roll}
                """.trimIndent()

                val accX = dataReceiver.accData.x
                val accY = dataReceiver.accData.y
                val accZ = dataReceiver.accData.z

//                val vec = RelVector(accX*100, accY*100, accZ)

                Log.i("Shit", text)
                val vec = getOrientationVector(dataReceiver.oriData)

                Log.i("Shit", vec.toString())
                dataChan.send(RelVector(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat(), getVectorLength(dataReceiver.accData)))

                delay(100)
            }
        }
    }

    data class OrientationVector(val x: Double, val y: Double, val z: Double)

    fun getOrientationVector(orientationData: OrientationData): OrientationVector {
        val azimuth = orientationData.azimuth

        val vectorX = sin(azimuth.plus(Math.PI*8))
        val vectorY = cos(azimuth.plus(Math.PI*8))

        return OrientationVector(vectorX, vectorY, 0.toDouble())
    }

    fun getVectorLength(accelerometerData: AccelerometerData): Float {
        return sqrt(accelerometerData.x * accelerometerData.x +
                accelerometerData.y * accelerometerData.y +
                accelerometerData.z * accelerometerData.z)
    }

    fun stopReading(view: View) {
        stop = true
        dataStreamer.Stop()
        drawer.EndDrawing()
    }
}