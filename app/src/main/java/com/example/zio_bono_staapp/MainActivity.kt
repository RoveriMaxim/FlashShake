package com.example.zio_bono_staapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private var isAppInForeground = false
    private var isShakeServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val closeButton: Button = findViewById(R.id.close_btn)
        closeButton.setOnClickListener {
            if (isShakeServiceRunning) {
                stopShakeService()
                isShakeServiceRunning = false
            }
            finishAffinity()
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navView)

        // toggle per il menu a tendina
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // listener per le voci del menu
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_intensity -> {
                    showToast("Modifica Intensità clicked")
                }
                R.id.menu_item_timer -> {
                    showToast("Timer Spegnimento clicked")
                }
                R.id.menu_item_donate -> {
                    showToast("Dona clicked")
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // Imposta l'azione per aprire il menu
        val btnMenu = findViewById<ImageButton>(R.id.menu_icon)
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        /*val smileGifImageView: ImageView = findViewById(R.id.gif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.gif)
            .into(smileGifImageView)*/

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer sensor not found", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            if (checkAndRequestCameraPermission()) {
                startShakeService()
                isShakeServiceRunning = true
            }
        }
    }

    private fun stopShakeService() {
        val intent = Intent(this, ShakeService::class.java)
        stopService(intent)
    }

    private fun checkAndRequestCameraPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            // Richiesta dei permessi della fotocamera
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return false
        } else {
            // Ottenuti i permessi, posso avviare il servizio
            startShakeService()
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Ottenuto i permessi, posso avviare il servizio
                startShakeService()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun startShakeService() {
        val intent = Intent(this, ShakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ripristina l'orientamento predefinito solo se l'app viene chiusa completamente
        if (!isAppInForeground) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_hamburger, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.END) // Apri il menu a destra
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //Creo un alert dialog.....



    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
