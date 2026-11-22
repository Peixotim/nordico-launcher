package app.nordico.launcher.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import app.nordico.launcher.data.obd.StreamTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * ELM327 por Bluetooth clássico (perfil serial SPP). O dispositivo é resolvido aqui dentro: endereço
 * inválido, Bluetooth desligado ou permissão negada viram [IOException], que o laço de conexão trata
 * como "perdida" e repete com espera crescente.
 */
class BluetoothSppTransport(private val adapter: BluetoothAdapter?, private val address: String) : StreamTransport() {
    @SuppressLint("MissingPermission") // BLUETOOTH_CONNECT é pedida antes de listar os adaptadores.
    override suspend fun open() = withContext(Dispatchers.IO) {
        val device = try {
            adapter?.getRemoteDevice(address) ?: throw IOException("sem Bluetooth neste aparelho")
        } catch (e: IllegalArgumentException) {
            throw IOException("endereço Bluetooth inválido", e)
        }
        // Nunca iniciamos descoberta, então não há o que cancelar (cancelDiscovery exigiria BLUETOOTH_SCAN).
        val socket: BluetoothSocket = try {
            connect(device.createRfcommSocketToServiceRecord(SPP))
        } catch (_: IOException) {
            // Alguns adaptadores só aceitam a conexão sem criptografia.
            connect(device.createInsecureRfcommSocketToServiceRecord(SPP))
        } catch (e: SecurityException) {
            throw IOException("permissão Bluetooth negada", e)
        }
        attach(socket.inputStream, socket.outputStream) { runCatching { socket.close() } }
    }

    private fun connect(socket: BluetoothSocket): BluetoothSocket {
        try {
            socket.connect()
        } catch (e: IOException) {
            runCatching { socket.close() }
            throw e
        }
        return socket
    }
}
