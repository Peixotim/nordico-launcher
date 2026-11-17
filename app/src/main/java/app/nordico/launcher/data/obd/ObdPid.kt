package app.nordico.launcher.data.obd

/** PIDs do modo 01 (SAE J1979) lidos pelo Nórdico. Somente leitura. */
enum class ObdPid(val id: Int, val bytes: Int) {
    Speed(0x0D, 1),
    Rpm(0x0C, 2),
    Coolant(0x05, 1),
    Intake(0x0F, 1),
    Load(0x04, 1),
    Throttle(0x11, 1),
    Fuel(0x2F, 1),
    Voltage(0x42, 2),
}

/** Converte os bytes de dados do PID para a unidade da tela (km/h, rpm, °C, %, V). */
fun ObdPid.decode(data: List<Int>): Double {
    val a = data.getOrElse(0) { 0 }
    val b = data.getOrElse(1) { 0 }
    return when (this) {
        ObdPid.Speed -> a.toDouble()
        ObdPid.Rpm -> (a * 256 + b) / 4.0
        ObdPid.Coolant, ObdPid.Intake -> a - 40.0
        ObdPid.Load, ObdPid.Throttle, ObdPid.Fuel -> a * 100.0 / 255.0
        ObdPid.Voltage -> (a * 256 + b) / 1000.0
    }
}
