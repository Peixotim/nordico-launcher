package app.nordico.launcher.data.radio

import kotlinx.coroutines.flow.StateFlow

/** Sintonia atual. `khz` em kHz nas duas bandas; `station` é o nome (RDS) quando houver. */
data class RadioState(val band: Band, val khz: Int, val station: String?, val stereo: Boolean = true)

interface RadioRepository {
    /** `false` quando o fabricante não expõe controle do rádio: a tela mostra o cartão "Rádio do fabricante". */
    val available: StateFlow<Boolean>
    val state: StateFlow<RadioState>

    fun setBand(band: Band)
    fun tune(khz: Int)

    /** Procura a próxima estação para cima (`dir` > 0) ou para baixo. */
    fun seek(dir: Int)
}
