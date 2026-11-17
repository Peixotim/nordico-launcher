package app.nordico.launcher.data.obd

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * ELM327 por TCP: simulador no PC (emulador acessa o PC em `10.0.2.2`) ou adaptador Wi-Fi
 * (normalmente `192.168.0.10:35000`).
 */
class TcpTransport(private val host: String, private val port: Int) : StreamTransport() {
    override suspend fun open() = withContext(Dispatchers.IO) {
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), 3000)
        attach(socket.getInputStream(), socket.getOutputStream()) { runCatching { socket.close() } }
    }
}
