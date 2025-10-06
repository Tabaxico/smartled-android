package com.example.smartled

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.smartled.bt.BluetoothService

class MainActivity : AppCompatActivity() {

    private lateinit var viewLed: View
    private lateinit var btnToggle: Button
    private lateinit var tvStatus: TextView
    private lateinit var btnServer: Button
    private lateinit var btnClient: Button
    private lateinit var spPaired: Spinner

    private var ledOn = false
    private lateinit var bt: BluetoothService
    private var bonded: List<BluetoothDevice> = emptyList()

    private val reqPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        tvStatus.text = if (granted)
            "Permisos concedidos. Toca el botón de nuevo."
        else
            "Permisos denegados. Actívalos en Ajustes > Apps > SmartLED > Permisos."
        if (granted) loadPaired()
    }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            tvStatus.text = if (result.resultCode == RESULT_OK)
                "Estado: Bluetooth activado. Toca el botón de nuevo."
            else
                "Estado: Bluetooth desactivado."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewLed   = findViewById(R.id.viewLed)
        btnToggle = findViewById(R.id.btnToggle)
        tvStatus  = findViewById(R.id.tvStatus)
        btnServer = findViewById(R.id.btnServer)
        btnClient = findViewById(R.id.btnClient)
        spPaired  = findViewById(R.id.spPaired)

        bt = BluetoothService(
            this,
            onMessage = { msg -> runOnUiThread { applyIncoming(msg) } },
            onState   = { s   -> runOnUiThread { tvStatus.text = "Estado: $s" } }
        )

        if (!bt.isBluetoothAvailable()) {
            tvStatus.text = "Estado: Bluetooth no disponible"
            btnServer.isEnabled = false
            btnClient.isEnabled = false
        } else {
            loadPaired()
        }

        btnServer.setOnClickListener {
            if (!ensurePerms()) { tvStatus.text = "Pidiendo permisos…"; return@setOnClickListener }
            if (!ensureBluetoothOn()) return@setOnClickListener
            tvStatus.text = "Estado: iniciando HOST…"
            bt.startServer()
        }

        btnClient.setOnClickListener {
            if (!ensurePerms()) { tvStatus.text = "Pidiendo permisos…"; return@setOnClickListener }
            if (!ensureBluetoothOn()) return@setOnClickListener
            val index = spPaired.selectedItemPosition
            val device = bonded.getOrNull(index)
            if (device == null) { tvStatus.text = "Selecciona un dispositivo emparejado"; return@setOnClickListener }
            tvStatus.text = "Estado: conectando a ${device.name}…"
            bt.connectTo(device)
        }

        btnToggle.setOnClickListener {
            ledOn = !ledOn
            renderLed()
            bt.sendMessage(if (ledOn) "ON" else "OFF")
        }

        renderLed()
    }

    private fun loadPaired() {
        bonded = bt.bondedDevices()
        val names = bonded.map { "${it.name} (${it.address})" }
        spPaired.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun renderLed() {
        viewLed.setBackgroundColor(
            if (ledOn) 0xFFFFFF00.toInt() else 0xFF808080.toInt()
        )
        btnToggle.text = if (ledOn) "Apagar LED" else "Encender LED"
    }

    private fun applyIncoming(msg: String) {
        when (msg.uppercase()) {
            "ON"  -> { ledOn = true;  renderLed() }
            "OFF" -> { ledOn = false; renderLed() }
        }
    }

    private fun ensureBluetoothOn(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return if (!adapter.isEnabled) {
            tvStatus.text = "Estado: solicitando activar Bluetooth…"
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            false
        } else true
    }

    private fun ensurePerms(): Boolean {
        if (Build.VERSION.SDK_INT >= 31) {
            val need = mutableListOf<String>()
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                need += Manifest.permission.BLUETOOTH_CONNECT
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                need += Manifest.permission.BLUETOOTH_SCAN
            if (need.isNotEmpty()) { reqPerms.launch(need.toTypedArray()); return false }
            return true
        }
        val need = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            need += Manifest.permission.ACCESS_COARSE_LOCATION
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            need += Manifest.permission.ACCESS_FINE_LOCATION
        if (need.isNotEmpty()) { reqPerms.launch(need.toTypedArray()); return false }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        bt.stopAll()
    }
}
