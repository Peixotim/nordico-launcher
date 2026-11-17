package app.nordico.launcher.device

import app.nordico.launcher.data.radio.Band
import app.nordico.launcher.data.radio.RadioRepository
import app.nordico.launcher.data.radio.RadioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Rádio da central. O sintonizador é controlado pelo microcontrolador do fabricante e o Android não
 * tem API de rádio FM para apps comuns, então fica indisponível até o reconhecimento da central (spec 12).
 */
// Trade-off: indisponível de propósito; a tela mostra o cartão "Rádio do fabricante".
class DeviceRadioRepository : RadioRepository {
    override val available: StateFlow<Boolean> = MutableStateFlow(false)
    override val state: StateFlow<RadioState> = MutableStateFlow(RadioState(Band.Fm, 98_500, null))
    override fun setBand(band: Band) = Unit
    override fun tune(khz: Int) = Unit
    override fun seek(dir: Int) = Unit
}
