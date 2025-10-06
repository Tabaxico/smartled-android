package com.example.smartled.bt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class BluetoothService(
    private val context: Context,
    private val onMessage: (String) -> Unit,
    private val onState: (String) -> Unit
) {
    companion object {
        // UUID SPP estándar (Serial Port Profile)
        val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val APP_NAME = "SmartLED"
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var connJob: Job? = null
    private var socket: BluetoothSocket? = null

    fun isBluetoothAvailable() = adapter != null

    fun isPermitted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun bondedDevices(): List<BluetoothDevice> {
        if (!isPermitted()) return emptyList()
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        if (!isPermitted()) { onState("Permisos Bluetooth faltantes"); return }
        stopAll()
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val server = adapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                onState("HOST: esperando conexión…")
                val sock = server?.accept()
                server?.close()
                if (sock != null) {
                    socket = sock
                    onState("HOST: conectado a ${sock.remoteDevice.name}")
                    listenSocket(sock)
                } else onState("HOST: no se pudo aceptar conexión")
            } catch (e: Exception) { onState("HOST error: ${e.message}") }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectTo(device: BluetoothDevice) {
        if (!isPermitted()) { onState("Permisos Bluetooth faltantes"); return }
        stopAll()
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val sock = device.createRfcommSocketToServiceRecord(APP_UUID)
                adapter?.cancelDiscovery()
                onState("CLIENTE: conectando a ${device.name}…")
                sock.connect()
                socket = sock
                onState("CLIENTE: conectado")
                listenSocket(sock)
            } catch (e: Exception) { onState("CLIENTE error: ${e.message}") }
        }
    }

    private fun listenSocket(sock: BluetoothSocket) {
        connJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                BufferedReader(InputStreamReader(sock.inputStream)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        onMessage(line.trim())
                    }
                }
                onState("Conexión cerrada")
            } catch (se: SecurityException) {
                onState("Permisos Bluetooth denegados")
            } catch (e: Exception) {
                onState("Conexión cerrada: ${e.message}")
            }
        }
    }

    fun sendMessage(msg: String) {
        try {
            socket?.outputStream?.write((msg + "\n").toByteArray())
            socket?.outputStream?.flush()
        } catch (e: Exception) { onState("Error enviando: ${e.message}") }
    }

    fun stopAll() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        serverJob?.cancel(); clientJob?.cancel(); connJob?.cancel()
    }
}