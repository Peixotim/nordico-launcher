# Nórdico fase 2 (Veículo, Rádio e Mídia): plano de implementação

**Goal:** Adicionar ao launcher os módulos Veículo (OBD-II), Rádio e Mídia estendida, com interface completa, protocolo OBD testado e dados simulados, prontos para ligar à central no dia da instalação.

**Architecture:** Continua o módulo `:app` com `sim`/`device`. O protocolo ELM327 é código puro em `data/obd`, sobre uma interface `ObdTransport` (TCP para testes e adaptadores Wi-Fi, Bluetooth clássico para a central). `ElmObdRepository` fica em `main`, independente do transporte. Rádio e mídia seguem o mesmo padrão de contrato mais versão simulada.

**Tech Stack:** o mesmo da fase 1, mais `androidx.lifecycle:lifecycle-process` 2.11.0 (para saber se o app está visível).

**Spec:** `docs/specs/2026-11-17-fase2-veiculo-radio-midia-design.md`. Fase 1: `docs/specs/2026-11-15-launcher-fase1-design.md`.

## Global Constraints

- Pacote `app.nordico.launcher`; tela lógica 1280×720 dp; fontes, cores e mola da fase 1.
- OBD **somente leitura**: nenhum comando além de `AT...`, modo `01` e modo `03`.
- Unidades: km/h, °C, V, %. Frequência de rádio em **kHz inteiro** nas duas bandas.
- FM 76000–108000 kHz, passo 100. AM 520–1710 kHz, passo 10. Presets: 18 em FM, 12 em AM.
- Alertas: arrefecimento ≥ 105 °C (volta abaixo de 102), tensão < 11,8 V com rotação > 0 ou > 15,0 V. Repetição a cada 60 s; HUD de 6 s.
- Agendamento OBD: velocidade e rotação 250 ms; carga e acelerador 1000 ms; demais 2000 ms. PID com 3 `NO DATA` seguidos deixa de ser pedido.
- Reconexão com espera de 2, 4, 8 e no máximo 30 s, só com o app visível.
- Textos em pt-BR. Commits em inglês, Conventional Commits, um por arquivo, sem trailers de ferramenta.
- Comentários de atalho deliberado: `// Trade-off: ...`.

## Review Focus

- **Resposta do adaptador com lixo no meio** (eco, `SEARCHING...`, linhas de várias ECUs, `NO DATA`): o parser devolve erro tipado e nunca lança (teste na Task 1).
- **Adaptador some no meio da leitura** (socket fecha ou estoura o tempo): estado vira `Lost`, sem travar a interface, e reconecta com espera crescente (testes nas Tasks 2 e 4).
- **Carro que não suporta nenhum PID:** a tela mostra "—" e o polling não entra em laço quente (testes nas Tasks 2 e 5).
- **Motor desligado com tensão baixa** (12,1 V com rotação 0): sem alerta falso de bateria (teste na Task 3).
- **Rádio:** seek sem estação, sintonia fora da faixa e preset vazio não quebram (testes na Task 7).

## Estrutura de arquivos

```
data/obd/      ObdPid.kt  ObdParser.kt  DtcDecoder.kt  ObdSession.kt  PollScheduler.kt
               ObdAlerts.kt  ObdRepository.kt  StreamTransport.kt  ElmObdRepository.kt
data/radio/    RadioRepository.kt  RadioMath.kt
data/media/    MediaRepository.kt (altera)  SeekMath.kt
feature/vehicle/  VehicleScreen.kt  Gauges.kt  AdapterSheet.kt  FaultsSheet.kt  VehicleFormat.kt
feature/radio/    RadioScreen.kt  FrequencyDial.kt
feature/hud/      Huds.kt (altera: AlertHud)
app/src/sim/      SimObdRepository.kt  SimRadioRepository.kt  SimPanel.kt (altera)
app/src/device/   BluetoothSppTransport.kt  DeviceRadioRepository.kt
docs/prototipos/  04-veiculo-radio.html
```

Comandos: Git Bash na raiz. Testes: `./gradlew testSimDebugUnitTest`.

---

### Task 0: Protótipos das telas novas (portão de design)

**Files:**
- Create: `docs/prototipos/04-veiculo-radio.html`

Nenhum código de interface antes da aprovação desta task.

- [ ] **Step 1:** Construir no navegador, na resolução 1280×720, com o mesmo CSS de tokens do protótipo 03: tela **Veículo** (conectado, sem adaptador, folha de adaptadores, folha de falhas, aviso de alerta), tela **Rádio** (FM com presets, AM, indisponível), **Tocando agora** com barra arrastável e ações, e os novos **atalhos do Início** (Rádio, Veículo, Câmera, Apps). Dia e Noite.
- [ ] **Step 2:** Mostrar ao usuário e ajustar até aprovar. Em conflito, o protótipo 04 vale para as telas novas e o 03 para o resto.
- [ ] **Step 3: Commit** `docs: add vehicle and radio prototype`.

---

### Task 1: Núcleo do protocolo OBD (puro)

**Files:**
- Create: `data/obd/ObdPid.kt`, `data/obd/ObdParser.kt`, `data/obd/DtcDecoder.kt`
- Test: `ObdParserTest.kt`, `DtcDecoderTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class ObdPid(val id: Int, val bytes: Int) {
      Speed(0x0D, 1), Rpm(0x0C, 2), Coolant(0x05, 1), Intake(0x0F, 1),
      Load(0x04, 1), Throttle(0x11, 1), Fuel(0x2F, 1), Voltage(0x42, 2),
  }
  fun ObdPid.decode(data: List<Int>): Double
  enum class ObdError { NoData, UnableToConnect, CanError, Stopped, Unknown, Empty }
  sealed interface ObdResult<out T> { data class Ok<T>(val value: T) : ObdResult<T>; data class Err(val error: ObdError) : ObdResult<Nothing> }
  fun cleanReply(raw: String): List<String>                       // linhas úteis, maiúsculas, sem espaços/eco/prompt
  fun parseData(raw: String, mode: Int, pid: Int?): ObdResult<List<Int>>   // bytes depois de "4x [pid]"
  fun supportedPids(base: Int, mask: List<Int>): Set<Int>
  data class Dtc(val code: String)
  fun decodeDtcs(raw: String, canCount: Boolean): List<Dtc>
  ```

- [ ] **Step 1: Testes que falham**
  - `decode`: `Rpm` com `[0x1A, 0xF8]` = 1726.0; `Speed` `[0x3C]` = 60.0; `Coolant` `[0x7B]` = 83.0; `Intake` `[0x3C]` = 20.0; `Load`/`Throttle`/`Fuel` `[0xFF]` = 100.0 e `[0x00]` = 0.0; `Voltage` `[0x38, 0xA4]` = 14.5.
  - `cleanReply("010C\r41 0C 1A F8 \r\r>")` = `["410C1AF8"]`; `cleanReply("SEARCHING...\r41 0D 3C\r>")` = `["410D3C"]`; `cleanReply("")` = lista vazia.
  - `parseData("410C1AF8", 1, 0x0C)` = `Ok([0x1A, 0xF8])`; com duas ECUs (`"410C1AF8\r410C1AF8"`) usa a primeira; `"NO DATA"` = `Err(NoData)`; `"UNABLE TO CONNECT"` = `Err(UnableToConnect)`; `"CAN ERROR"` = `Err(CanError)`; `"STOPPED"` = `Err(Stopped)`; `"?"` = `Err(Unknown)`; `""` = `Err(Empty)`; resposta de outro PID (`"410D3C"` pedindo `0C`) = `Err(Unknown)`.
  - `supportedPids(0x00, [0xBE, 0x3F, 0xA8, 0x13])` contém `0x04` e `0x05`, não contém `0x02` nem `0x08`; o bit menos significativo do último byte indica se o PID `base + 0x20` existe.
  - `decodeDtcs("43010133000000", canCount = true)` = `[P0133]`; `"4301412300"` = `[C0123]` (`0x41` → letra C); `[0xC1, 0x00]` → `U0100`; pares `0000` ignorados; vazio = lista vazia.
  Run: `./gradlew testSimDebugUnitTest --tests '*data.obd*'`. Expected: FALHA (símbolos ausentes).

- [ ] **Step 2: Implementar** as funções acima. `parseData` procura a primeira linha limpa que começa com `0x40 + mode` e, se `pid != null`, o byte seguinte igual ao PID; nunca lança.
- [ ] **Step 3:** Rodar os testes. Expected: PASS.
- [ ] **Step 4: Commit**, um arquivo por commit (`feat(obd): ...`, `test(obd): ...`).

---

### Task 2: Sessão ELM327 e agendador

**Files:**
- Create: `data/obd/ObdSession.kt`, `data/obd/PollScheduler.kt`, `data/obd/ObdRepository.kt` (só `ObdTransport`)
- Test: `ObdSessionTest.kt`, `PollSchedulerTest.kt`

**Interfaces:**
- Consumes: Task 1.
- Produces:
  ```kotlin
  interface ObdTransport { suspend fun open(); suspend fun send(cmd: String): String; fun close() }
  data class InitResult(val adapter: String, val protocol: String, val supported: Set<Int>)
  class ObdSession(private val t: ObdTransport) {
      suspend fun init(): ObdResult<InitResult>
      suspend fun read(pid: ObdPid): ObdResult<Double>
      suspend fun readFaults(canCount: Boolean = true): ObdResult<List<Dtc>>
  }
  val POLL_INTERVALS: Map<ObdPid, Long>      // da seção Global Constraints
  fun nextPid(nowMs: Long, last: Map<ObdPid, Long>, active: Set<ObdPid>): ObdPid?
  class StrikeCounter(private val limit: Int = 3) { fun noData(p: ObdPid): Boolean /* true = desativar */; fun ok(p: ObdPid) }
  ```

- [ ] **Step 1: Testes que falham**
  - Transporte falso roteirizado (mapa comando → resposta). `init` envia nesta ordem: `ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0`, `ATSP0`, `ATI`, `ATDPN`, `0100`, `0120`, `0140`, `0160`; extrai versão (`"ELM327 v1.5"`), protocolo e a união dos PIDs suportados.
  - Resposta `?` a `ATL0` ou `ATS0` é ignorada; `UNABLE TO CONNECT` a `0100` = `Err(UnableToConnect)`; `0120` sem resposta útil não derruba o `init`.
  - `read(Rpm)` com `"410C1AF8"` = `Ok(1726.0)`; `NO DATA` = `Err(NoData)`; o transporte lançando exceção de E/S = `Err(Unknown)` (nunca propaga).
  - `readFaults` com `"43010133000000"` = `[P0133]`.
  - `nextPid`: nunca lido = devolve na ordem de prioridade (Speed, Rpm, Load, Throttle, Coolant, Intake, Fuel, Voltage); com Speed lido há 100 ms e Rpm há 300 ms, devolve Rpm; nada vencido = `null`; PID fora de `active` nunca sai; `active` vazio = `null`.
  - `StrikeCounter`: 3 `noData` seguidos retornam `true` na terceira; um `ok` zera a contagem.
  Run: `./gradlew testSimDebugUnitTest --tests '*ObdSession*' --tests '*PollScheduler*'`. Expected: FALHA.

- [ ] **Step 2: Implementar.** `ObdSession` relança `CancellationException` e converte qualquer outra falha de E/S em `Err(Unknown)`.
- [ ] **Step 3:** Rodar. Expected: PASS.
- [ ] **Step 4: Commit** por arquivo.

---

### Task 3: Contratos, alertas, simulação e HUD

**Files:**
- Create: `data/obd/ObdRepository.kt` (contratos), `data/obd/ObdAlerts.kt`, `src/sim/.../SimObdRepository.kt`
- Modify: `data/prefs/PrefsRepository.kt` (`obdAdapter`), `feature/hud/HudController.kt` (`HudKind.Alert(6000)`), `feature/hud/Huds.kt` (`AlertHud`), `navigation/AppViewModel.kt`, `navigation/AppShell.kt`, os dois `AppContainer`
- Test: `ObdAlertsTest.kt`, `SimObdRepositoryTest.kt` (em `testSim`)

**Interfaces:**
- Produces: `VehicleTelemetry`, `ObdState`, `ObdAdapter(name: String, address: String)`, `ObdRepository` (spec 11.1); `enum class Alert { None, CoolantHigh, BatteryLow, BatteryHigh }`; `fun alertFor(t: VehicleTelemetry, previous: Alert): Alert`; `fun shouldShowAlert(previous: Alert, current: Alert, lastShownMs: Long?, nowMs: Long): Boolean`; `SimObdRepository.scenario(s: ObdScenario)` com `enum class ObdScenario { NoAdapter, Idle, Highway, Overheating, WeakBattery }`; `AppContainer.obd: ObdRepository`.

- [ ] **Step 1: Testes que falham**
  - `alertFor`: coolant 104 com `None` = `None`; 105 = `CoolantHigh`; 103 vindo de `CoolantHigh` = `CoolantHigh`; 101 vindo de `CoolantHigh` = `None`; coolant `null` mantém o anterior.
  - Bateria: 11,7 V com rpm 800 = `BatteryLow`; **11,7 V com rpm 0 ou `null` = `None`**; 12,1 V com rpm 0 = `None`; 15,1 V = `BatteryHigh`; 14,5 V = `None`.
  - `shouldShowAlert`: `None→CoolantHigh` = true; mesmo alerta com `lastShown` há 59 s = false e há 60 s = true; `CoolantHigh→None` = false.
  - `SimObdRepository`: `Highway` dá velocidade ≥ 80 e rotação > 0; `Overheating` dá coolant ≥ 105; `NoAdapter` dá `ObdState.NoAdapter` e telemetria toda `null`; `bondedAdapters()` devolve dois adaptadores fictícios.
  Run: `./gradlew testSimDebugUnitTest`. Expected: FALHA.

- [ ] **Step 2: Implementar.** `AppViewModel` observa `obd.telemetry`, calcula o alerta e chama `hud.show(HudKind.Alert)`. `AlertHud` usa o ícone `warning`, texto "Temperatura alta · 108 °C" / "Bateria fraca · 11,6 V" / "Tensão alta · 15,2 V", cor `#FF3B30`.
- [ ] **Step 3:** Rodar a suíte e `assembleDeviceDebug`. Expected: PASS e BUILD SUCCESSFUL (no `device`, `obd` aponta para um repositório `NoAdapter` até a Task 4).
- [ ] **Step 4: Commit** por arquivo.

---

### Task 4: Transporte TCP e ElmObdRepository

**Files:**
- Create: `data/obd/StreamTransport.kt`, `data/obd/ElmObdRepository.kt`, `data/obd/TcpTransport.kt`
- Modify: `gradle/libs.versions.toml` e `app/build.gradle.kts` (`lifecycle-process`; `buildConfigField OBD_TCP` no flavor `sim` lido de `-PobdTcp`), `src/sim/.../AppContainer.kt`
- Test: `StreamTransportTest.kt`, `ElmObdRepositoryTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  open class StreamTransport(private val timeoutMs: Long = 3000) : ObdTransport {
      protected fun attach(input: InputStream, output: OutputStream, onClose: () -> Unit)
      // send(): escreve "cmd\r", lê até '>' ou estourar o tempo (IOException)
  }
  class TcpTransport(private val host: String, private val port: Int) : StreamTransport()
  class ElmObdRepository(
      scope: CoroutineScope,
      visible: StateFlow<Boolean>,
      adapters: () -> List<ObdAdapter>,
      selected: Flow<String?>,
      select: suspend (String) -> Unit,
      transportFor: (address: String) -> ObdTransport,
  ) : ObdRepository
  ```

- [ ] **Step 1: Testes que falham**
  - `StreamTransport` com `PipedInputStream`/`PipedOutputStream`: `send("010C")` escreve `010C\r` e devolve tudo até o `>`; sem `>` dentro do tempo = `IOException`; `close()` fecha os streams.
  - `ElmObdRepository` com `ObdTransport` falso (usando `runTest`): sem endereço selecionado = `NoAdapter`; com endereço e `init` bem-sucedido = `Connected` e a telemetria passa a ter velocidade e rotação; **o transporte caindo no meio = `Lost`, depois nova tentativa após 2 s, depois 4 s** (tempo virtual); `visible = false` pausa o polling (nenhum `send` novo) e `true` retoma; PID que dá 3 `NO DATA` deixa de ser pedido e sai como `null`; **todos os PIDs `NO DATA` não gera laço quente** (o número de `send` por segundo virtual fica dentro do limite do agendador).
  Run: testes novos. Expected: FALHA.

- [ ] **Step 2: Implementar.** O laço de polling pega o PID de `nextPid` e, se nada venceu, espera 50 ms. `selected` troca de adaptador reiniciando a sessão.
- [ ] **Step 3:** Rodar a suíte. Expected: PASS.
- [ ] **Step 4: Verificação manual com simulador de ELM327.** No PC, subir um simulador de ELM327 em modo de rede (o projeto `ELM327-emulator`, `pip install ELM327-emulator`; conferir a flag de porta na documentação dele, o padrão costuma ser 35000). Depois `./gradlew installSimDebug -PobdTcp=10.0.2.2:35000` e conferir os valores na tela. Anotar na ledger o comando exato que funcionou.
- [ ] **Step 5: Commit** por arquivo.

---

### Task 5: Tela Veículo e atalhos do Início

**Files:**
- Create: `feature/vehicle/VehicleScreen.kt`, `Gauges.kt`, `AdapterSheet.kt`, `FaultsSheet.kt`, `VehicleFormat.kt`
- Modify: `navigation/Screen.kt` (`Vehicle`), `navigation/AppShell.kt`, `feature/home/HomeScreen.kt` (atalhos), `AppContainer` (sim e device: `readFaults`)
- Test: `VehicleFormatTest.kt`

**Interfaces:**
- Produces: `fun coolantTone(c: Int?): TempTone4` com `enum class TempTone4 { Cool, Normal, Warm, Hot }`; `fun formatOrDash(v: Number?, suffix: String = ""): String`; `fun rpmFraction(rpm: Int?): Float` (0..8000 → 0f..1f, `null` = 0f); `@Composable fun VehicleScreen(c: AppContainer, onBack: () -> Unit)`.

- [ ] **Step 1: Testes que falham:** `coolantTone`: 69 = Cool, 70 = Normal, 100 = Normal, 101 = Warm, 104 = Warm, 105 = Hot, `null` = Normal; `formatOrDash(null)` = `"—"`, `formatOrDash(83, " °C")` = `"83 °C"`; `rpmFraction(4000)` = 0,5; 9000 = 1f; `null` = 0f. Expected: FALHA.
- [ ] **Step 2: Implementar** conforme o protótipo 04 aprovado (spec 9.2). Estados: `NoAdapter` (vazio com "Escolher adaptador"), `Connecting`, `Connected`, `Lost` (faixa "Conexão perdida, reconectando…"). Valores `null` = "—". Folha de adaptadores com o atalho `Settings.ACTION_BLUETOOTH_SETTINGS`. Folha de falhas sem botão de apagar. Atalhos do Início: Rádio, Veículo, Câmera, Apps (Telefone sai).
- [ ] **Step 3:** Rodar os testes, instalar no emulador e comparar com o protótipo nos cenários do painel de simulação (Dia e Noite). Carro sem nenhum PID suportado mostra todos os cartões com "—".
- [ ] **Step 4: Commit** por arquivo.

---

### Task 6: Bluetooth clássico e container do `device`

**Files:**
- Create: `src/device/.../device/BluetoothSppTransport.kt`
- Modify: `src/main/AndroidManifest.xml` (`BLUETOOTH_CONNECT`), `src/device/.../AppContainer.kt`, `MainActivity.kt` (pedido de permissão)

- [ ] **Step 1:** `BluetoothSppTransport(device: BluetoothDevice) : StreamTransport()`: `cancelDiscovery()`, `createRfcommSocketToServiceRecord(00001101-0000-1000-8000-00805F9B34FB)`; se falhar, tentar `createInsecureRfcommSocketToServiceRecord`; abrir em `Dispatchers.IO`.
- [ ] **Step 2:** No `device`, `obd = ElmObdRepository(...)` usando `BluetoothAdapter.bondedDevices` (com `BLUETOOTH_CONNECT` concedida; sem permissão ou sem adaptador Bluetooth, a lista é vazia e o estado é `NoAdapter`). O pedido de permissão acontece ao abrir "Escolher adaptador", não na partida.
- [ ] **Step 3:** `assembleDeviceDebug` e teste manual no emulador: a folha abre, a lista vem vazia sem Bluetooth e nada quebra. O teste com o adaptador real fica para o dia da instalação (spec 12).
- [ ] **Step 4: Commit** por arquivo.

---

### Task 7: Rádio

**Files:**
- Create: `data/radio/RadioRepository.kt`, `data/radio/RadioMath.kt`, `src/sim/.../SimRadioRepository.kt`, `src/device/.../DeviceRadioRepository.kt`, `feature/radio/RadioScreen.kt`, `feature/radio/FrequencyDial.kt`
- Modify: `Screen.kt` (`Radio`), `AppShell.kt`, `HomeScreen.kt` (atalho), `PrefsRepository.kt` (presets), `AppContainer`s
- Test: `RadioMathTest.kt`, `SimRadioRepositoryTest.kt`

**Interfaces:**
- Produces: `Band`, `RadioState`, `RadioRepository` (spec 11.1); `fun stepKhz(band: Band, khz: Int, dir: Int): Int` (com volta circular nos limites); `fun snapKhz(band: Band, khz: Int): Int`; `class Presets { fun save(band: Band, index: Int, khz: Int): Presets; fun recall(band: Band, index: Int): Int? }`; `fun nextStation(band: Band, khz: Int, dir: Int, stations: List<Int>): Int?`.

- [ ] **Step 1: Testes que falham:** `stepKhz(Fm, 107900, +1)` = 108000; `(Fm, 108000, +1)` = 76000; `(Fm, 76000, -1)` = 108000; `(Am, 520, -1)` = 1710; `(Am, 1710, +1)` = 520. `snapKhz(Fm, 98040)` = 98000; `(Fm, 200000)` = 108000; `(Am, 1000)` = 1000; `(Am, 1004)` = 1000. `Presets`: salvar e recuperar; índice 18 em FM e 12 em AM não altera nada; preset vazio = `null`. `nextStation`: lista vazia = `null`; passa da última volta para a primeira; para trás da primeira volta para a última. `SimRadioRepository`: `seek(+1)` vai para a próxima estação; `setBand(Am)` troca a frequência para uma de AM.
- [ ] **Step 2: Implementar.** `DeviceRadioRepository.available = false` (comentário `// Trade-off: sem controle do rádio até o reconhecimento da central`). `RadioScreen` conforme o protótipo (segmentado FM/AM, escala arrastável, seek, passo fino, presets: tocar chama, segurar salva). Indisponível: cartão "Rádio do fabricante" que abre o app dele quando o pacote for conhecido; hoje, sem ação.
- [ ] **Step 3:** Rodar a suíte e verificar no emulador. Expected: PASS.
- [ ] **Step 4: Commit** por arquivo.

---

### Task 8: Mídia estendida

**Files:**
- Create: `data/media/SeekMath.kt`
- Modify: `data/media/MediaRepository.kt`, `SimRepositories.kt` (mídia), `DeviceMediaRepository.kt`, `feature/nowplaying/NowPlayingScreen.kt`
- Test: `SeekMathTest.kt`

**Interfaces:**
- Produces: `fun seekTarget(fraction: Float, durationMs: Long): Long` (limita a 0..duração; duração ≤ 0 = 0); `data class MediaAction(val id: String, val label: String, val glyph: String, val active: Boolean)`; `MediaRepository.seekTo(ms: Long)`, `val actions: StateFlow<List<MediaAction>>`, `fun perform(id: String)`.

- [ ] **Step 1: Testes que falham:** `seekTarget(0.5f, 200_000)` = 100000; `(1.5f, 200_000)` = 200000; `(-0.2f, 200_000)` = 0; `(0.5f, 0)` = 0.
- [ ] **Step 2: Implementar.** `device`: `seekTo` por `transportControls.seekTo`; `actions` vêm de `PlaybackState.customActions` (id = ação, rótulo e ícone mapeados quando reconhecidos, como aleatório, repetir e curtir; o resto é ignorado); `perform` por `sendCustomAction`. `sim`: aleatório, repetir e curtir funcionando. UI: barra arrastável (a posição vai ao soltar; durante o arraste mostra o tempo alvo) e linha de ações só com o que existir.
- [ ] **Step 3:** Rodar a suíte e verificar no emulador (arrastar a barra do simulador muda a posição). Expected: PASS.
- [ ] **Step 4: Commit** por arquivo.

---

### Task 9: Painel de simulação, documentação e verificação final

**Files:**
- Modify: `src/sim/.../SimPanel.kt` (linha "OBD" com os 5 cenários), `README.md`, `README.pt-BR.md` (roteiro e recursos)

- [ ] **Step 1:** Painel de simulação com os cenários de OBD. Atualizar os dois READMEs (funcionalidades novas, comando `-PobdTcp`, roteiro) e acrescentar prints novos em `docs/screenshots/`.
- [ ] **Step 2: Verificação final** (marcar cada item):
  - Suíte inteira verde e `assembleDeviceDebug` verde.
  - Cenário `Overheating`: alerta aparece em qualquer tela, some em 6 s, repete a cada 60 s e para quando a temperatura cai abaixo de 102 °C.
  - `NoAdapter`: estado vazio, "Escolher adaptador" abre a folha.
  - Carro sem PIDs suportados: tudo "—", sem consumo anormal de CPU.
  - Conexão do adaptador cai e volta: faixa "Conexão perdida", depois reconecta.
  - Veículo e Rádio em Dia e Noite e a 240 dpi, iguais ao protótipo.
  - Com "remover animações" ativo nada quebra.
  - Protocolo ponta a ponta contra o simulador de ELM327 por TCP (Task 4, passo 4).
- [ ] **Step 3: Commit** por arquivo.

---

## Cobertura da spec

| Seção da spec | Task |
|---|---|
| 9 (design) | 0, 5, 7, 8 |
| 10.1 (protocolo) | 1, 2, 4 |
| 10.2 (alertas) | 3 |
| 10.3 (rádio) | 7 |
| 10.4 (mídia) | 8 |
| 11 (arquitetura, contratos, simulação) | 3, 4, 6 |
| 12 (dia da instalação) | fora do plano: checklist manual na central |
| 13 (testes) | em cada task e na 9 |
