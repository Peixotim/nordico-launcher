package app.nordico.launcher.sim

import app.nordico.launcher.data.audio.VolumeOrigin
import app.nordico.launcher.data.audio.VolumeState
import org.junit.Assert.assertEquals
import org.junit.Test

class SimVolumeTest {
    @Test fun external_change_marks_origin() {
        val r = SimVolumeRepository()
        r.simulateExternal(2)
        assertEquals(VolumeState(20, false, VolumeOrigin.External), r.state.value)
    }

    @Test fun internal_step_marks_internal() {
        val r = SimVolumeRepository()
        r.step(-1)
        assertEquals(VolumeOrigin.Internal, r.state.value.origin)
        assertEquals(17, r.state.value.level)
    }

    @Test fun toggle_mute() {
        val r = SimVolumeRepository()
        r.toggleMute()
        assertEquals(VolumeState(18, true, VolumeOrigin.Internal), r.state.value)
    }
}
