package app.nordico.launcher.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdAlertsTest {
    private fun t(coolant: Int? = null, volts: Double? = null, rpm: Int? = null) =
        VehicleTelemetry(coolantC = coolant, batteryV = volts, rpm = rpm)

    @Test fun coolant_threshold_and_hysteresis() {
        assertEquals(Alert.None, alertFor(t(coolant = 104), Alert.None))
        assertEquals(Alert.CoolantHigh, alertFor(t(coolant = 105), Alert.None))
        assertEquals(Alert.CoolantHigh, alertFor(t(coolant = 103), Alert.CoolantHigh))
        assertEquals(Alert.CoolantHigh, alertFor(t(coolant = 102), Alert.CoolantHigh))
        assertEquals(Alert.None, alertFor(t(coolant = 101), Alert.CoolantHigh))
    }

    @Test fun missing_coolant_keeps_the_previous_state() {
        assertEquals(Alert.CoolantHigh, alertFor(t(coolant = null), Alert.CoolantHigh))
        assertEquals(Alert.None, alertFor(t(coolant = null), Alert.None))
    }

    @Test fun without_a_connection_there_is_no_alert() {
        // Sem conexão o alerta anterior não pode ficar preso mostrando "— °C".
        val hot = t(coolant = 110)
        assertEquals(Alert.None, alertFor(ObdState.NoAdapter, hot, Alert.CoolantHigh))
        assertEquals(Alert.None, alertFor(ObdState.Connecting, hot, Alert.CoolantHigh))
        assertEquals(Alert.None, alertFor(ObdState.Lost, t(), Alert.BatteryLow))
    }

    @Test fun with_a_connection_the_telemetry_decides() {
        val connected = ObdState.Connected("A6", "ELM327")
        assertEquals(Alert.CoolantHigh, alertFor(connected, t(coolant = 110), Alert.None))
        assertEquals(Alert.CoolantHigh, alertFor(connected, t(coolant = null), Alert.CoolantHigh))
    }

    @Test fun low_battery_only_with_the_engine_running() {
        assertEquals(Alert.BatteryLow, alertFor(t(volts = 11.7, rpm = 800), Alert.None))
        assertEquals(Alert.None, alertFor(t(volts = 11.7, rpm = 0), Alert.None))
        assertEquals(Alert.None, alertFor(t(volts = 11.7, rpm = null), Alert.None))
        assertEquals(Alert.None, alertFor(t(volts = 12.1, rpm = 0), Alert.None))
    }

    @Test fun high_battery_voltage_alerts_regardless_of_rpm() {
        assertEquals(Alert.BatteryHigh, alertFor(t(volts = 15.1, rpm = 2000), Alert.None))
        assertEquals(Alert.None, alertFor(t(volts = 14.5, rpm = 2000), Alert.None))
    }

    @Test fun coolant_alert_wins_over_battery() {
        assertEquals(Alert.CoolantHigh, alertFor(t(coolant = 110, volts = 11.0, rpm = 900), Alert.None))
    }

    @Test fun first_occurrence_shows() {
        assertTrue(shouldShowAlert(Alert.None, Alert.CoolantHigh, null, 0))
    }

    @Test fun repeat_every_sixty_seconds_while_it_lasts() {
        assertFalse(shouldShowAlert(Alert.CoolantHigh, Alert.CoolantHigh, 0, 59_000))
        assertTrue(shouldShowAlert(Alert.CoolantHigh, Alert.CoolantHigh, 0, 60_000))
    }

    @Test fun clearing_never_shows() {
        assertFalse(shouldShowAlert(Alert.CoolantHigh, Alert.None, 0, 120_000))
        assertFalse(shouldShowAlert(Alert.None, Alert.None, null, 0))
    }

    @Test fun switching_to_another_alert_shows_immediately() {
        assertTrue(shouldShowAlert(Alert.CoolantHigh, Alert.BatteryLow, 1_000, 2_000))
    }

    @Test fun texts_use_comma_decimals() {
        assertEquals("Temperatura alta · 108 °C", alertText(Alert.CoolantHigh, t(coolant = 108)))
        assertEquals("Bateria fraca · 11,6 V", alertText(Alert.BatteryLow, t(volts = 11.6)))
        assertEquals("Tensão alta · 15,2 V", alertText(Alert.BatteryHigh, t(volts = 15.2)))
        assertEquals("", alertText(Alert.None, t()))
    }
}
