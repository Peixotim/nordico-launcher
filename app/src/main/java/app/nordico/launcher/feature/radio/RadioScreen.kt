package app.nordico.launcher.feature.radio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.radio.Band
import app.nordico.launcher.data.radio.Presets
import app.nordico.launcher.data.radio.RadioState
import app.nordico.launcher.data.radio.maxKhz
import app.nordico.launcher.data.radio.minKhz
import app.nordico.launcher.data.radio.stepKhz
import app.nordico.launcher.data.radio.stepSizeKhz
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.NordicCard
import app.nordico.launcher.designsystem.components.PageHeader
import app.nordico.launcher.designsystem.components.SegmentedControl
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick
import app.nordico.launcher.navigation.AppViewModel
import kotlinx.coroutines.launch

/** Rádio FM/AM (spec 9.3). Sem controle do fabricante mostra o cartão "Rádio do fabricante". */
@Composable
fun RadioScreen(vm: AppViewModel, c: AppContainer, onBack: () -> Unit) {
    val n = LocalNordic.current
    val available by c.radio.available.collectAsStateWithLifecycle()
    val state by c.radio.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(n.bg)) {
        PageHeader("Rádio", onBack) {
            if (available) {
                SegmentedControl(
                    options = listOf("" to "FM", "" to "AM"),
                    selected = state.band.ordinal,
                    onSelect = { c.radio.setBand(Band.entries[it]) },
                    modifier = Modifier.width(240.dp).height(52.dp),
                )
            }
        }
        if (available) {
            RadioBody(c, state, prefs.radioPresets, Modifier.weight(1f).padding(start = 28.dp, end = 28.dp, bottom = 28.dp))
        } else {
            NordicCard(Modifier.weight(1f).fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 28.dp), radius = 30.dp, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Sym("radio", size = 72.dp, tint = n.label3)
                    NText("Rádio do fabricante", nordicText(30, FontWeight.Bold, trackingEm = -0.02f))
                    NText(
                        "O controle do sintonizador desta central ainda não está disponível no Nórdico. Abra o rádio do fabricante pela gaveta de Apps.",
                        nordicText(17, align = TextAlign.Center),
                        color = n.label2,
                        maxLines = 3,
                        modifier = Modifier.widthIn(max = 560.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioBody(c: AppContainer, st: RadioState, presets: Presets, modifier: Modifier) {
    val n = LocalNordic.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        NordicCard(Modifier.weight(1f).fillMaxWidth(), radius = 30.dp) {
            Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 30.dp, bottom = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.Bottom) {
                    NText(formatFrequency(st.band, st.khz), nordicText(150, FontWeight.Light, display = true, trackingEm = -0.03f, tnum = true))
                    NText(frequencyUnit(st.band), nordicText(30, FontWeight.Medium), color = n.label2, modifier = Modifier.padding(start = 14.dp, bottom = 12.dp))
                    Column(Modifier.weight(1f).padding(bottom = 14.dp), horizontalAlignment = Alignment.End) {
                        NText(st.station ?: "", nordicText(30, FontWeight.SemiBold, trackingEm = -0.02f, align = TextAlign.End))
                        NText(if (st.stereo) "Estéreo" else "Mono", nordicText(16, align = TextAlign.End), color = n.label2)
                    }
                }
                Column {
                    FrequencyDial(st.band, st.khz, onTune = c.radio::tune)
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally)) {
                        Key("skip_previous", "Buscar", leading = true) { c.radio.seek(-1) }
                        Key("remove", null) { c.radio.tune(stepKhz(st.band, st.khz, -1)) }
                        Key("add", null) { c.radio.tune(stepKhz(st.band, st.khz, +1)) }
                        Key("skip_next", "Buscar", leading = false) { c.radio.seek(+1) }
                    }
                }
            }
        }
        PresetPager(c, st, presets)
    }
}

@Composable
private fun Key(glyph: String, label: String?, leading: Boolean = true, onClick: () -> Unit) {
    val n = LocalNordic.current
    Row(
        Modifier
            .pressClick(onClick)
            .height(64.dp)
            .then(if (label == null) Modifier.width(72.dp) else Modifier.widthIn(min = 120.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(n.fill3)
            .padding(horizontal = if (label == null) 0.dp else 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (label != null && !leading) NText(label, nordicText(17, FontWeight.SemiBold))
        Sym(glyph, size = 30.dp)
        if (label != null && leading) NText(label, nordicText(17, FontWeight.SemiBold))
    }
}

/** Escala de sintonia: tocar ou arrastar muda a frequência (spec 9.3). */
@Composable
private fun FrequencyDial(band: Band, khz: Int, onTune: (Int) -> Unit) {
    val n = LocalNordic.current
    val min = minKhz(band)
    val steps = (maxKhz(band) - min) / stepSizeKhz(band)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(74.dp)
            .pointerInput(band) { detectTapGestures { onTune(khzAtFraction(band, it.x / size.width)) } }
            .pointerInput(band) { detectHorizontalDragGestures { change, _ -> onTune(khzAtFraction(band, change.position.x / size.width)) } },
    ) {
        for (i in 0..steps) {
            val x = size.width * i / steps
            val (h, color) = when {
                i % 10 == 0 -> 46.dp to n.label2
                i % 5 == 0 -> 30.dp to n.label3
                else -> 16.dp to n.fill2
            }
            drawLine(color, Offset(x, size.height), Offset(x, size.height - h.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        val nx = size.width * dialFraction(band, khz)
        drawLine(n.accent, Offset(nx, size.height), Offset(nx, 0f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetPager(c: AppContainer, st: RadioState, presets: Presets) {
    val n = LocalNordic.current
    val scope = rememberCoroutineScope()
    val band = st.band
    val pages = presetPages(band)
    key(band) {
        val pager = rememberPagerState(pageCount = { pages })
        Column {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                NText("Memórias · toque para chamar, segure para salvar", nordicText(14, FontWeight.Medium), color = n.label2)
                repeat(pages) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.height(7.dp).width(if (on) 20.dp else 7.dp).clip(RoundedCornerShape(4.dp)).background(if (on) n.accent else n.fill2))
                }
            }
            HorizontalPager(pager, pageSpacing = 10.dp) { page ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(PRESETS_PER_PAGE) { slot ->
                        val index = page * PRESETS_PER_PAGE + slot
                        val value = presets.recall(band, index)
                        PresetChip(
                            band, value, active = value != null && value == st.khz, Modifier.weight(1f),
                            onTap = { value?.let(c.radio::tune) },
                            onSave = { scope.launch { c.prefs.setPreset(band, index, st.khz) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(band: Band, khz: Int?, active: Boolean, modifier: Modifier, onTap: () -> Unit, onSave: () -> Unit) {
    val n = LocalNordic.current
    val bg = if (active) n.accent else n.surface
    Column(
        modifier
            .height(70.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .pointerInput(khz) { detectTapGestures(onTap = { onTap() }, onLongPress = { onSave() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (khz == null) {
            Sym("add", size = 26.dp, tint = n.label3)
        } else {
            NText(formatFrequency(band, khz), nordicText(22, FontWeight.SemiBold), color = if (active) Color.White else n.label)
            NText(frequencyUnit(band), nordicText(12, FontWeight.Medium), color = if (active) Color.White.copy(alpha = 0.78f) else n.label2)
        }
    }
}
