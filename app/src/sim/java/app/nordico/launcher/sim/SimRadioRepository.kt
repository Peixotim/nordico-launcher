package app.nordico.launcher.sim

import app.nordico.launcher.data.radio.Band
import app.nordico.launcher.data.radio.RadioRepository
import app.nordico.launcher.data.radio.RadioState
import app.nordico.launcher.data.radio.nextStation
import app.nordico.launcher.data.radio.snapKhz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Rádio simulado: estações fixas, seek de verdade e memória da última frequência de cada banda. */
class SimRadioRepository : RadioRepository {
    private val stations = mapOf(
        Band.Fm to mapOf(
            88_100 to "Cidade FM", 91_300 to "Rádio Jazz", 94_700 to "Antena Um",
            98_500 to "Rádio Nórdico", 102_100 to "Clássica FM", 105_500 to "Rock Estrada",
        ),
        Band.Am to mapOf(620 to "Rádio Tempo", 790 to "Notícias AM", 1_040 to "Esporte AM", 1_230 to "Rádio Popular", 1_490 to "Rádio Sertão"),
    )
    private val last = mutableMapOf(Band.Fm to 98_500, Band.Am to 1_040)
    private val _available = MutableStateFlow(true)
    private val _state = MutableStateFlow(RadioState(Band.Fm, 98_500, stations.getValue(Band.Fm)[98_500]))
    override val available: StateFlow<Boolean> = _available
    override val state: StateFlow<RadioState> = _state

    override fun setBand(band: Band) = show(band, last.getValue(band))

    override fun tune(khz: Int) = show(_state.value.band, snapKhz(_state.value.band, khz))

    override fun seek(dir: Int) {
        val band = _state.value.band
        nextStation(band, _state.value.khz, dir, stations.getValue(band).keys.toList())?.let { show(band, it) }
    }

    private fun show(band: Band, khz: Int) {
        last[band] = khz
        _state.value = RadioState(band, khz, stations.getValue(band)[khz])
    }
}
