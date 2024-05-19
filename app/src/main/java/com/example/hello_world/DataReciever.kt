package com.example.hello_world

interface DataReceiver {
    var accData : AccelerometerData
    var oriData : OrientationData
}

class MyDataReceiver(context: android.content.Context): DataReceiver {
    override var accData: AccelerometerData = AccelerometerData(0f,0f,0f)
    override var oriData: OrientationData = OrientationData(0f,0f,0f)
}