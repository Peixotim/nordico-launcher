package app.nordico.launcher.data.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdParserTest {
    @Test fun decodes_each_pid_with_known_values() {
        assertEquals(1726.0, ObdPid.Rpm.decode(listOf(0x1A, 0xF8)), 0.0)
        assertEquals(60.0, ObdPid.Speed.decode(listOf(0x3C)), 0.0)
        assertEquals(83.0, ObdPid.Coolant.decode(listOf(0x7B)), 0.0)
        assertEquals(20.0, ObdPid.Intake.decode(listOf(0x3C)), 0.0)
        assertEquals(100.0, ObdPid.Load.decode(listOf(0xFF)), 0.0)
        assertEquals(0.0, ObdPid.Throttle.decode(listOf(0x00)), 0.0)
        assertEquals(100.0, ObdPid.Fuel.decode(listOf(0xFF)), 0.0)
        assertEquals(14.5, ObdPid.Voltage.decode(listOf(0x38, 0xA4)), 0.0)
        assertEquals(-5.0, ObdPid.Ambient.decode(listOf(0x23)), 0.0)
        assertEquals(130.0, ObdPid.Status.decode(listOf(0x82, 0x07, 0xE5, 0x00)), 0.0) // luz acesa + 2 falhas
    }

    @Test fun clean_reply_drops_noise() {
        assertEquals(listOf("410C1AF8"), cleanReply("010C\r41 0C 1A F8 \r\r>", echoOf = "010C"))
        assertEquals(listOf("410D3C"), cleanReply("SEARCHING...\r41 0D 3C\r>"))
        assertEquals(listOf("410D3C"), cleanReply("BUS INIT: OK\r41 0D 3C\r>"))
        assertTrue(cleanReply("").isEmpty())
        assertTrue(cleanReply("\r\r>").isEmpty())
    }

    @Test fun parses_data_bytes() {
        assertEquals(ObdResult.Ok(listOf(0x1A, 0xF8)), parseData("410C1AF8", 1, 0x0C))
        assertEquals(ObdResult.Ok(listOf(0x1A, 0xF8)), parseData("410C1AF8\r410C1AF8", 1, 0x0C))
        assertEquals(ObdResult.Ok(listOf(0x3C)), parseData("41 0D 3C\r\r>", 1, 0x0D))
    }

    @Test fun first_ecu_wins_even_if_another_says_no_data() {
        assertEquals(ObdResult.Ok(listOf(0x3C)), parseData("NO DATA\r410D3C", 1, 0x0D))
    }

    @Test fun maps_adapter_errors_without_throwing() {
        assertEquals(ObdResult.Err(ObdError.NoData), parseData("NO DATA", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.UnableToConnect), parseData("UNABLE TO CONNECT", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.CanError), parseData("CAN ERROR", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.Stopped), parseData("STOPPED", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.Unknown), parseData("?", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.Empty), parseData("", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.Unknown), parseData("GARBAGE@@", 1, 0x0C))
    }

    @Test fun reply_for_another_pid_is_an_error() {
        assertEquals(ObdResult.Err(ObdError.Unknown), parseData("410D3C", 1, 0x0C))
    }

    @Test fun odd_length_or_non_hex_payload_is_rejected() {
        assertEquals(ObdResult.Err(ObdError.Unknown), parseData("410C1AF", 1, 0x0C))
        assertEquals(ObdResult.Err(ObdError.Unknown), parseData("410CZZZZ", 1, 0x0C))
    }

    @Test fun supported_pids_from_bitmask() {
        val s = supportedPids(0x00, listOf(0xBE, 0x3F, 0xA8, 0x13))
        assertTrue(0x04 in s)
        assertTrue(0x05 in s)
        assertFalse(0x02 in s)
        assertFalse(0x08 in s)
        assertTrue(0x20 in s) // último bit = "existe o próximo bloco"
    }

    @Test fun supported_pids_respects_base() {
        val s = supportedPids(0x20, listOf(0x80, 0x00, 0x00, 0x00))
        assertEquals(setOf(0x21), s)
    }
}
