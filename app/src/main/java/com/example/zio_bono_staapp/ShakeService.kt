package com.example.zio_bono_staapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeService : Service(), SensorEventListener {

    //quanto tenere acceso e sensibilità

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String

    private var isFlashOn: Boolean = false
    private var isShakeOn: Boolean = false
    private val shakeThreshold = 70
    private val minTimeBetweenShakes: Long = 1000 // Tempo minimo tra una scossa e l'altra (in millisecondi)
    private var lastShakeTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "ShakeServiceChannel"
            val channel = NotificationChannel(
                notificationChannelId,
                "Shake Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, notificationChannelId)
                .setContentTitle("Shake Service")
                .setContentText("Service is running in the background.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]

        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer sensor not available.", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Verifica se l'app è in primo piano o in background
        val isAppInForeground = isAppInForeground()

        if (isAppInForeground) {
            // L'app è in primo piano, avvia il rilevamento delle scosse
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            }
            isShakeOn = true
        } else {
            // L'app è in background, arresta il rilevamento delle scosse
            sensorManager.unregisterListener(this)
            isShakeOn = false
        }

        return START_STICKY
    }

    private fun isAppInForeground(): Boolean {
        // Implementa la logica per verificare se l'app è in primo piano o in background
        // Posso utilizzare una variabile nel MainActivity per tenere traccia dello stato in onResume e onPause
        // Oppure posso utilizzare altre soluzioni come l'uso di un lifecycle observer.
        // A partire da settembre 2021, non ho informazioni sull'implementazione attuale dell'app.

        // Per il momento, posso semplicemente restituire true in modo che il servizio sia sempre attivo.
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble())
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastShakeTime > minTimeBetweenShakes) {
            if (acceleration > shakeThreshold) {
                if (!isFlashOn) {
                    turnOnFlash()
                } else {
                    turnOffFlash()
                }
                lastShakeTime = currentTime
            }
        }
    }

    private fun turnOnFlash() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, true)
                isFlashOn = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun turnOffFlash() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
                isFlashOn = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
