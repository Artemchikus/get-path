package com.example.hello_world

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

data class AccelerometerData(var x: Float, var y: Float, var z: Float)
data class OrientationData(var azimuth: Float, var pitch: Float, var roll: Float)

class DataStreamer(context: Context): SensorEventListener {
    private var mSensorManager : SensorManager
    private var mAccelerometer : Sensor?= null
    private var mMagnetometer : Sensor?= null
    private var mRotVector : Sensor?= null
    private var orientationArray = FloatArray(3)
    private var rotationVector = FloatArray(9)
    private val VALUE_DRIFT = 0.05f
    private var receiver : DataReceiver?=null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val data = event.values.clone()

                    receiver?.accData?.x = data[0]
                    receiver?.accData?.y = data[1]
                    receiver?.accData?.z = data[2]
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val data = event.values.clone()
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    rotationVector = event.values.clone()

                    val rotationV = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationV, rotationVector)

                    val orientationValuesV = FloatArray(3)
                    SensorManager.getOrientation(rotationV, orientationValuesV)

                    receiver?.oriData?.azimuth = orientationValuesV[0]
                    receiver?.oriData?.pitch = orientationValuesV[1]
                    receiver?.oriData?.roll = orientationValuesV[2]
                }
                else -> {
                    return
                }
            }



//            val rotationMatrix = FloatArray(9)
//            val rotationOK = SensorManager.getRotationMatrix(rotationMatrix, null, accData, magnData)
//
//            var rotationMatrixAdjusted = FloatArray(9);
//            val rotation = mDisplay?.rotation
//
//            when (rotation) {
//                android.view.Surface.ROTATION_0 -> rotationMatrixAdjusted = rotationMatrix.clone()
//                android.view.Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
//                    rotationMatrix,
//                    SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
//                    rotationMatrixAdjusted
//                )
//                android.view.Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
//                    rotationMatrix,
//                    SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
//                    rotationMatrixAdjusted
//                )
//                android.view.Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
//                    rotationMatrix,
//                    SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
//                    rotationMatrixAdjusted
//                )
//            }
//
//            if (rotationOK) {
//                SensorManager.getOrientation(rotationMatrixAdjusted, orientationArray)
//            }
//
//            var azimuth = orientationArray[0]
//            var pitch = orientationArray[1]
//            var roll = orientationArray[2]
//
//            if (abs(pitch) < VALUE_DRIFT) {
//                pitch = 0F;
//            }
//            if (abs(roll) < VALUE_DRIFT) {
//                roll = 0F;
//            }
//
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }


    init{
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mRotVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }


    fun Start(dataGetter: DataReceiver): Boolean {
        if (mRotVector != null && mAccelerometer != null) {
            receiver = dataGetter
            mSensorManager.registerListener(this, mRotVector, SensorManager.SENSOR_DELAY_NORMAL)
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            return true
        }

        return false
    }

    fun Stop(): Boolean {
        mSensorManager.unregisterListener(this)
        receiver = null

        return true
    }
}