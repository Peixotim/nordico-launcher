package app.nordico.launcher.data.obd

import java.util.Locale

enum class Alert { None, CoolantHigh, BatteryLow, BatteryHigh }

// Limites da spec 10.2, em um lugar só para calibrar.
const val COOLANT_ALERT_C = 105
const val COOLANT_CLEAR_C = 102
const val BATTERY_LOW_V = 11.8
const val BATTERY_HIGH_V = 15.0
const val ALERT_REPEAT_MS = 60_000L

/**
 * Alerta atual a partir da telemetria. O arrefecimento tem histerese (liga em 105, só apaga abaixo de 102)
 * e a bateria baixa só vale com o motor ligado: com a chave em ACC a tensão cai sem ser defeito.
 */
fun alertFor(t: VehicleTelemetry, previous: Alert): Alert {
    val c = t.coolantC
    val coolantHigh = when {
        c == null -> previous == Alert.CoolantHigh
        c >= COOLANT_ALERT_C -> true
        c < COOLANT_CLEAR_C -> false
        else -> previous == Alert.CoolantHigh
    }
    if (coolantHigh) return Alert.CoolantHigh
    val v = t.batteryV ?: return Alert.None
    val running = (t.rpm ?: 0) > 0
    return when {
        v > BATTERY_HIGH_V -> Alert.BatteryHigh
        running && v < BATTERY_LOW_V -> Alert.BatteryLow
        else -> Alert.None
    }
}

/** Sem conexão com o adaptador não há alerta: a telemetria vazia seria lida como "leitura ausente". */
fun alertFor(state: ObdState, t: VehicleTelemetry, previous: Alert): Alert =
    if (state is ObdState.Connected) alertFor(t, previous) else Alert.None

/** Mostra na entrada de um alerta, ao trocar de alerta e a cada minuto enquanto durar; nunca ao limpar. */
fun shouldShowAlert(previous: Alert, current: Alert, lastShownMs: Long?, nowMs: Long): Boolean = when {
    current == Alert.None -> false
    previous != current -> true
    lastShownMs == null -> true
    else -> nowMs - lastShownMs >= ALERT_REPEAT_MS
}

private fun volts(v: Double?) = String.format(Locale.US, "%.1f", v ?: 0.0).replace('.', ',')

fun alertText(alert: Alert, t: VehicleTelemetry): String = when (alert) {
    Alert.None -> ""
    Alert.CoolantHigh -> "Temperatura alta · ${t.coolantC ?: "—"} °C"
    Alert.BatteryLow -> "Bateria fraca · ${volts(t.batteryV)} V"
    Alert.BatteryHigh -> "Tensão alta · ${volts(t.batteryV)} V"
}
