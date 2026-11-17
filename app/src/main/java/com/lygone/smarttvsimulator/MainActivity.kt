package com.lygone.smarttvsimulator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // Señales que la interfaz Compose observa para reaccionar a eventos del sistema.
    private val homeSignal = mutableIntStateOf(0)     // se pulsó Inicio / se reabrió el launcher
    private val packageSignal = mutableIntStateOf(0)  // se instaló, quitó o actualizó una app
    private val resumeSignal = mutableIntStateOf(0)   // la pantalla volvió a primer plano

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            packageSignal.intValue++
        }
    }
    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true

        setContent {
            LauncherApp(
                homeSignal = homeSignal.intValue,
                packageSignal = packageSignal.intValue,
                resumeSignal = resumeSignal.intValue
            )
        }
    }

    // Con launchMode="singleTask", pulsar Inicio (o el icono) con el launcher ya abierto llega aquí.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.intValue++
    }

    override fun onResume() {
        super.onResume()
        resumeSignal.intValue++
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
    }
}
