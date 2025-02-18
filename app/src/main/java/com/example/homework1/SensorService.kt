package com.example.homework1

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import kotlin.math.abs

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lastNotificationTime: Long = 0
    private val notificationCooldown = 2000
    private var lastYRotation: Float? = null
    private var initialYRotation: Float? = null
    private var hasMoved = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val yRot = Math.toDegrees(orientationAngles[2].toDouble()).toFloat() // 🔹 Y-Rotation
            val currentTime = System.currentTimeMillis()

            Log.d("SensorDebug", "Y-Rotation: $yRot")

            // Ensimmäinen sensorilukema tallennetaan, mutta EI lähetetä ilmoitusta
            if (initialYRotation == null) {
                initialYRotation = yRot
                lastYRotation = yRot
                Log.d("SensorDebug", "Tallennettiin alkutila, mutta ei ilmoitusta!")
                return
            }

            // Jos arvo ei ole muuttunut yli 45°, ei tehdä mitään
            if (abs(yRot - lastYRotation!!) < 45) {
                return
            }

            //  Estetään ensimmäinen virheliike heti käynnistyksessä
            if (!hasMoved) {
                hasMoved = true
                lastYRotation = yRot
                Log.d("SensorDebug", "Ensimmäinen kallistus huomioitu, mutta ei ilmoitusta!")
                return
            }

            // Tarkistetaan, että edellisestä ilmoituksesta on kulunut riittävästi aikaa
            if (currentTime - lastNotificationTime > notificationCooldown) {
                lastNotificationTime = currentTime
                lastYRotation = yRot // verrataan edelliseen mittaukseen

                Log.d("NotificationDebug", "Ilmoitus: Puhelimen kallistus muuttui!")

                NotificationHelper.sendNotification(
                    applicationContext,
                    "Kaaduitko?!",
                    "Kävikö?"
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

