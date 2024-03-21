package com.example.hello_world

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.view.forEach
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var mSensorManager : SensorManager
    private var resume = false
    private val sensorTypes = arrayListOf<Int>()
    private val sensors = mutableMapOf<String, Sensor>()
    private val fileWriters = mutableMapOf<Int, FileWriter>()


    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && resume) {
            fileWriters[event.sensor.type]?.appendLine(event.values.joinToString(";"))
            fileWriters[event.sensor.type]?.flush()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("path", filesDir.path)

        val checkBoxes = findViewById<LinearLayout>(R.id.checkboxes)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mSensorManager.getSensorList(Sensor.TYPE_ALL).forEach {
            val check = CheckBox(this)
            val sensorType = it.stringType.removePrefix("android.sensor.").uppercase()
            check.text = sensorType
            checkBoxes.addView(check)

            sensors[sensorType] = it

            sensorTypes.add(it.type)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
        fileWriters.forEach{
            it.value.flush()
            it.value.close()
        }
    }

    fun resumeReading(view: View) {
        this.resume = true

        val checkBoxes = findViewById<LinearLayout>(R.id.checkboxes)
        checkBoxes.forEach {
            if (it is CheckBox && it.isChecked) {
                val sensor = sensors[it.text]
                if (sensor != null) {
                    mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                    val file = File(filesDir.path+"/"+it.text)
                    file.createNewFile()
                    fileWriters[sensor.type] = FileWriter(file)
                }
            }
        }
    }

    fun pauseReading(view: View) {
        this.resume = false
    }
}