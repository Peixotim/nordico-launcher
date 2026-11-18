# Nórdico: limpar falhas (plano de implementação)

**Goal:** Listar as falhas do carro com descrição, causa, solução e vídeo (conteúdo offline), permitir marcá-las como corrigidas (local) e **apagar todas de verdade** pelo modo 04 do OBD-II, com proteções e resultado verificado por releitura.

**Architecture:** Continua `:app` com `sim`/`device`. O comando `04` entra em `ObdSession` (código puro) atrás de uma lista fixa de comandos permitidos. `ObdRepository` ganha `clearFaults()` e um `StateFlow` de estado. Catálogo de códigos e registro local (marcas e histórico) são classes puras com codec de texto. A interface reescreve a folha de falhas.

**Tech Stack:** o mesmo da fase 2, mais `org.json:json` só em `testImplementation`.

**Spec:** `docs/specs/2026-11-18-limpar-falhas-design.md`.

## Global Constraints

- Pacote `app.nordico.launcher`; tela lógica 1280×720 dp; design system da fase 1.
- Comandos ao adaptador: só `^(AT[A-Z0-9 ]{1,8}|01[0-9A-F]{2}|03|04)$`. `04` só sai de `ObdSession.clearFaults()`.
- Apagar só com: conectado, **velocidade lida = 0** e algo para apagar. O repositório repete a checagem.
- `CLEAR_TIMEOUT_MS = 8000`, `CLEAR_SETTLE_MS = 1500`. Nenhum reenvio automático; segundo toque durante `Clearing` é ignorado.
- "Limpo" só depois de releitura vazia. Vermelho de sistema só para gravidade "Pare e procure ajuda".
- Histórico: no máximo 20 registros, gravado **antes** do envio.
- Textos em pt-BR. Commits em inglês, Conventional Commits, **um por arquivo**, sem trailers de ferramenta. Atalhos deliberados: `// Trade-off: ...`.
- Comandos: Git Bash na raiz. Testes: `./gradlew testSimDebugUnitTest` e `./gradlew testSimDebugUnitTest --tests '*<Classe>*'`.

## Review Focus

- **Nenhum caminho envia comando fora da lista** (teste com transporte que registra tudo, em todos os fluxos).
- **Resposta do `04` com várias ECUs, mistas ou com lixo:** `parseClear` nunca lança e decide corretamente (Task 1).
- **Queda de conexão no meio da limpeza:** o histórico já existe, o resultado é `NoResponse`, nada trava e a leitura contínua retoma (Task 2).
- **Carro em movimento ou velocidade sem leitura:** bloqueia nos dois níveis (guarda da interface e do repositório) (Task 2).
- **Marca "corrigida" nunca vira "apagado":** a interface e os dados mantêm os dois conceitos separados (Tasks 5 e 6).

## Estrutura de arquivos

```
data/obd/      ObdClear.kt  ObdSession.kt (altera)  ObdRepository.kt (altera)  ElmObdRepository.kt (altera)
               DtcCatalog.kt  DtcDescriptions.kt (remove ao fim)
data/prefs/    FaultLog.kt  PrefsRepository.kt (altera)
feature/vehicle/  FaultsLogic.kt  FaultRow.kt  FaultsSheet.kt (reescreve)  ClearConfirmSheet.kt  VehicleScreen.kt (altera)
assets/dtc/    catalog.json
app/src/sim/   SimObdRepository.kt (altera)
docs/prototipos/ 05-limpar-falhas.html
```

---

### Task 0: Protótipo da folha de falhas (portão de design)

**Files:**
- Create: `docs/prototipos/05-limpar-falhas.html`

Nenhum código de interface antes da aprovação desta task.

- [ ] **Step 1:** No navegador, 1280×720, com os tokens do protótipo 04: folha de falhas (lista com gravidade), linha expandida (causas, solução, vídeo, marcar), modo seleção com barra inferior, folha de confirmação, estado "apagando", cada resultado (`Cleared`, `Returned`, `Refused`, `Unverified`, `NoResponse`, `Blocked`), carro em movimento (botões bloqueados), lista vazia. Dia e Noite.
- [ ] **Step 2:** Mostrar ao usuário e ajustar até aprovar.
- [ ] **Step 3: Commit** `docs: add fault clearing prototype`.

---

### Task 1: Protocolo do apagar (puro)

**Files:**
- Create: `data/obd/ObdClear.kt`
- Modify: `data/obd/ObdSession.kt`
- Test: `data/obd/ObdClearTest.kt`, `ObdSessionTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  const val CLEAR_TIMEOUT_MS = 8_000L
  const val CLEAR_SETTLE_MS = 1_500L
  sealed interface ClearReply {
      data object Accepted : ClearReply
      data class Refused(val nrc: Int) : ClearReply
      data object NoResponse : ClearReply
      data class Failed(val error: ObdError) : ClearReply
  }
  fun parseClear(raw: String): ClearReply
  // em ObdSession:
  suspend fun clearFaults(): ClearReply        // envia "04" com CLEAR_TIMEOUT_MS; nunca lança
  ```

- [ ] **Step 1: Testes que falham** (`./gradlew testSimDebugUnitTest --tests '*ObdClear*' --tests '*ObdSession*'`)
  - `parseClear("44")` e `parseClear("04\r44\r\r>")` = `Accepted`.
  - Duas ECUs, `"44\r44"` = `Accepted`. Mista, `"44\r7F0422"` = `Accepted`.
  - `"7F0422"` = `Refused(0x22)`; `"7F0411"` = `Refused(0x11)`; `"7F0421"` = `Refused(0x21)`.
  - `"NO DATA"` e `""` = `NoResponse`.
  - `"BUS BUSY"` = `Failed(Unknown)`; `"CAN ERROR"` = `Failed(CanError)`; `"UNABLE TO CONNECT"` = `Failed(UnableToConnect)`; `"?"` = `Failed(Unknown)`.
  - Resposta de outro modo (`"43010133"`) = `Failed(Unknown)`. Lixo qualquer nunca lança.
  - `ObdSession.clearFaults()` com transporte roteirizado: envia exatamente `04` (e mais nada), respeita `CLEAR_TIMEOUT_MS`, devolve `Accepted`/`Refused`; exceção de E/S = `NoResponse`; cancelamento propaga.
  - **Lista de comandos permitidos:** um transporte que grava tudo, rodando `init`, `read` de cada PID, `readFaults` e `clearFaults`; todos os comandos casam com `^(AT[A-Z0-9 ]{1,8}|01[0-9A-F]{2}|03|04)$` e o `04` aparece **só** na chamada de `clearFaults`.
- [ ] **Step 2: Implementar.** `parseClear` limpa com `cleanReply`; conta linhas `44` e `7F04xx`; aplica a tabela da spec 8.1. Em `ObdSession`, centralizar o envio num método privado `send` que faz `require` da expressão permitida; atualizar o comentário da classe ("leitura, mais o apagar do modo 04").
- [ ] **Step 3:** Rodar os testes. Expected: PASS.
- [ ] **Step 4: Commit** um por arquivo (`feat(obd): ...`, `test(obd): ...`).

---

### Task 2: Estado e fluxo no repositório

**Files:**
- Create: `data/obd/ObdClear.kt` (acrescenta tipos)
- Modify: `data/obd/ObdRepository.kt`, `data/obd/ElmObdRepository.kt`
- Test: `ElmObdRepositoryTest.kt`, `ObdClearTest.kt`

**Interfaces:**
- Consumes: Task 1.
- Produces:
  ```kotlin
  sealed interface ClearOutcome {
      data object Cleared : ClearOutcome
      data class Returned(val codes: List<Dtc>) : ClearOutcome
      data class Refused(val nrc: Int) : ClearOutcome
      data object Unverified : ClearOutcome
      data object NoResponse : ClearOutcome
      data class Blocked(val reason: ClearBlock) : ClearOutcome
  }
  enum class ClearBlock { NotConnected, Moving, NothingToClear }
  sealed interface ClearState {
      data object Idle : ClearState
      data object Clearing : ClearState
      data class Done(val outcome: ClearOutcome) : ClearState
  }
  /** Guarda pura usada pela interface e repetida pelo repositório. null = pode apagar. */
  fun clearGate(state: ObdState, t: VehicleTelemetry, faults: List<Dtc>?): ClearBlock?
  // em ObdRepository / NoAdapterObdRepository:
  val clearState: StateFlow<ClearState>
  fun clearFaults()
  fun dismissClearResult()
  ```

- [ ] **Step 1: Testes que falham**
  - `clearGate`: desconectado = `NotConnected`; velocidade `null` ou > 0 = `Moving`; conectado, parado, `faults = []` e `milOn != true` = `NothingToClear`; parado com falhas, ou com `milOn = true` e `faults = null` = `null`.
  - Repositório (transporte roteirizado, relógio virtual): com sessão conectada e `Speed = 0`:
    - `04` aceito e releitura `NO DATA` (vazia) = `Done(Cleared)`; a lista e o `faultCount` são atualizados.
    - `04` aceito e releitura com `P0420` = `Done(Returned([P0420]))`.
    - `7F0422` = `Done(Refused(0x22))`, sem releitura.
    - Releitura que falha = `Done(Unverified)`.
    - `04` sem resposta = `Done(NoResponse)`.
    - Conexão cai no meio (transporte lança) = `Done(NoResponse)`, estado `Lost`, e a leitura contínua retoma ao reconectar.
    - Velocidade 5 = `Done(Blocked(Moving))` e o transporte **não recebe** `04`.
    - Segunda chamada durante `Clearing` é ignorada (o transporte recebe um único `04`).
    - Passa `CLEAR_SETTLE_MS` entre o `04` e a releitura (relógio virtual).
    - `dismissClearResult()` volta a `Idle`.
  Run: `./gradlew testSimDebugUnitTest --tests '*ElmObdRepository*' --tests '*ObdClear*'`. Expected: FALHA.
- [ ] **Step 2: Implementar** em `ElmObdRepository`: `clearFaults()` roda no `scope`; checa `clearGate` com a telemetria e a lista atuais; muda para `Clearing`; envia `04` sob o `lock`; se aceito, `delay(CLEAR_SETTLE_MS)`, relê falhas e status; mapeia para `ClearOutcome`; atualiza `_faults`; publica `Done`. Qualquer exceção de E/S vira `NoResponse`. `NoAdapterObdRepository`: `clearState` fixo em `Idle`, `clearFaults()` e `dismissClearResult()` vazios.
- [ ] **Step 3:** Rodar os testes. Expected: PASS.
- [ ] **Step 4: Commit** um por arquivo.

---

### Task 3: Simulador

**Files:**
- Modify: `app/src/sim/.../SimObdRepository.kt`
- Test: `app/src/testSim/.../SimObdRepositoryTest.kt`

**Interfaces:**
- Produces: `ObdScenario` ganha `FaultsPersistent` (aceita, mas os códigos voltam) e `ClearRefused` (recusa `22`); `SimObdRepository` implementa `clearState`, `clearFaults()`, `dismissClearResult()` com um atraso curto simulado.

- [ ] **Step 1: Testes que falham**
  - `Faults`: `clearFaults()` leva `Clearing` → `Done(Cleared)`, a lista fica vazia, `milOn = false`, `faultCount = 0`.
  - `FaultsPersistent`: `Done(Returned)` com os mesmos códigos e a luz continua acesa.
  - `ClearRefused`: `Done(Refused(0x22))`, nada muda.
  - Cenário com velocidade > 0 (`Highway` com falhas): `Done(Blocked(Moving))`.
- [ ] **Step 2: Implementar.** O painel de simulação já gera os botões a partir de `ObdScenario.entries`.
- [ ] **Step 3:** Rodar `./gradlew testSimDebugUnitTest`. Expected: PASS.
- [ ] **Step 4: Commit** um por arquivo.

---

### Task 4: Integração contra o emulador ELM327

**Files:**
- Modify: `app/src/test/.../ElmEmulatorIntegrationTest.kt`

- [ ] **Step 1:** Verificar no emulador (Python 3.11, venv do `%TEMP%`, porta 35001) se o cenário `car` responde ao `04`. Rodar com `-PobdTcp=localhost:35001`.
- [ ] **Step 2:** Se responder, acrescentar o caso "apagar" ao teste de integração (lê falhas, apaga, relê). Se não responder, deixar um comentário `// Trade-off:` explicando que o `04` fica só no roteiro e **não** acrescentar teste frágil.
- [ ] **Step 3:** Encerrar o emulador **só pelo PID/porta** que você iniciou.
- [ ] **Step 4: Commit** `test(obd): cover clearing faults against the ELM327 emulator` (se aplicável).

---

### Task 5: Catálogo de códigos (conteúdo offline)

**Files:**
- Create: `data/obd/DtcCatalog.kt`, `app/src/main/assets/dtc/catalog.json`
- Modify: `app/build.gradle.kts`, `gradle/libs.versions.toml` (`testImplementation` de `org.json:json`)
- Remove: `data/obd/DtcDescriptions.kt` e `DtcDescriptionsTest.kt`, depois de migrar quem os usa
- Test: `data/obd/DtcCatalogTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class Severity { Ok, Attention, Stop }          // Pode seguir, Atenção, Pare e procure ajuda
  data class DtcInfo(val code: String, val title: String, val severity: Severity,
                     val causes: List<String>, val solution: String, val known: Boolean)
  class DtcCatalog(private val entries: Map<String, DtcInfo>) {
      fun lookup(code: String): DtcInfo           // nunca nulo; fallback por prefixo
      companion object { fun parse(json: String): DtcCatalog }
  }
  ```

- [ ] **Step 1: Testes que falham**
  - `parse` de um JSON pequeno: título, gravidade, causas e solução lidos; código em minúsculas é achado.
  - **Validação do arquivo real** (`catalog.json` carregado dos assets de teste): todo código casa `^[PCBU][0-3][0-9A-F]{3}$`, sem duplicatas, `title`/`solution` não vazios, ao menos uma causa, gravidade válida.
  - `lookup("P0420")` conhecido = `known = true`; `lookup("P0999")` desconhecido = `known = false`, título "Descrição não disponível no Nórdico" e texto por categoria (P0/P2 padrão, P1/P3 fabricante, B carroceria, C chassi, U rede); gravidade do fallback = `Attention`.
  - JSON malformado não lança: devolve catálogo vazio (só fallback).
- [ ] **Step 2: Escrever o catálogo v1.** Escrito e revisado à mão, em português, com as causas e a solução como **orientação**, não diagnóstico, e gravidade conservadora (na dúvida, `Attention`; falha de ignição em vários cilindros e superaquecimento = `Stop`). Cobrir pelo menos: P0011, P0101, P0107, P0113, P0117, P0125, P0128, P0133, P0171, P0172, P0300–P0304, P0335, P0420, P0442, P0455, P0456, P0500, P0505, P0507, P0562, P0563, mais os P1xxx Honda que tiverem fonte conferida. Citar a fonte consultada para cada entrada no commit ou em comentário do PR; sem fonte confiável, a entrada não entra.
- [ ] **Step 3: Implementar** `DtcCatalog` (parser com `org.json`) e um carregador `fun loadCatalog(context: Context): DtcCatalog` (lê o asset uma vez, fora da thread principal) em `device`/`sim` compartilhado via `main`. Migrar `VehicleScreen`/`Sheets` de `dtcDescription` para `lookup`. Apagar `DtcDescriptions.kt` e seu teste.
- [ ] **Step 4:** Rodar `./gradlew testSimDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit** um por arquivo.

---

### Task 6: Marcas e histórico locais

**Files:**
- Create: `data/prefs/FaultLog.kt`
- Modify: `data/prefs/PrefsRepository.kt`
- Test: `data/prefs/FaultLogTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class ClearResult { Attempt, Cleared, Returned, Refused, Unverified, NoResponse }
  data class ClearRecord(val atMs: Long, val codes: List<String>, val result: ClearResult)
  data class FaultLog(val marks: Map<String, Long> = emptyMap(), val history: List<ClearRecord> = emptyList()) {
      fun mark(codes: Collection<String>, atMs: Long): FaultLog
      fun unmark(codes: Collection<String>): FaultLog
      fun prune(present: Collection<String>): FaultLog           // descarta marcas de códigos ausentes
      fun begin(codes: List<String>, atMs: Long): FaultLog       // registra "Attempt" antes do envio
      fun finish(atMs: Long, result: ClearResult): FaultLog      // completa o registro mais recente
      fun encode(): String
      companion object { fun decode(text: String?): FaultLog }
  }
  ```
  `Prefs` ganha `faultLog: FaultLog`; `PrefsRepository` ganha `suspend fun updateFaultLog(f: (FaultLog) -> FaultLog)`.

- [ ] **Step 1: Testes que falham**
  - `mark`/`unmark` idempotentes; `prune(["P0420"])` mantém só a marca de `P0420`.
  - `begin` cria um registro `Attempt`; `finish` troca o resultado do mais recente; o histórico respeita o limite de 20 (o mais antigo sai).
  - `encode`/`decode` ida e volta com marcas e histórico; `decode(null)`, `""` e texto corrompido devolvem `FaultLog()` sem lançar.
- [ ] **Step 2: Implementar** (formato em texto simples, como os presets). **Step 3:** testes passam.
- [ ] **Step 4: Commit** um por arquivo.

---

### Task 7: Interface da folha de falhas

**Pré-requisito:** protótipo da Task 0 aprovado. Em conflito, o protótipo vale.

**Files:**
- Create: `feature/vehicle/FaultsLogic.kt`, `FaultRow.kt`, `ClearConfirmSheet.kt`
- Modify: `feature/vehicle/FaultsSheet.kt` (reescreve), `VehicleScreen.kt`, `navigation/AppViewModel.kt` (se precisar expor o `FaultLog`)
- Test: `feature/vehicle/FaultsLogicTest.kt`

**Interfaces (lógica pura em `FaultsLogic.kt`):**
```kotlin
fun faultsSummary(count: Int, milOn: Boolean?): String          // "2 falhas · luz do motor acesa"
fun severityLabel(s: Severity): String
fun videoQuery(code: String, vehicle: String): String            // URL de busca
fun clearOutcomeText(o: ClearOutcome): String                    // mensagens da spec 8.2
fun canOpenVideo(speedKmh: Int?): Boolean                        // só com 0
```

- [ ] **Step 1: Testes que falham.** `faultsSummary(0, false)` = "Nenhuma falha registrada"; `(1, true)` = "1 falha · luz do motor acesa"; `(2, false)` = "2 falhas"; `videoQuery("P0420", "Honda Civic")` codifica espaços e contém o código; `canOpenVideo(null)` e `(5)` = falso, `(0)` = verdadeiro; `clearOutcomeText` cobre **todos** os resultados e `Refused(0x22)` fala em motor desligado e chave ligada.
- [ ] **Step 2: Implementar a lógica.** Testes passam.
- [ ] **Step 3: Interface.** `FaultsSheet`: cabeçalho com resumo e "Selecionar"; botão principal "Apagar todas as falhas" (desativado com o motivo quando `clearGate` bloqueia); lista rolável com `FaultRow` (código, título, selo de gravidade, expande para causas, solução, "Ver vídeo" e "Marcar como corrigida"); modo seleção com caixas e barra "Marcar N como corrigidas"; texto fixo "Só no app. Não apaga nada no carro." ao lado de marcar; linha "Corrigida, aguardando apagar" para marcadas ainda presentes. `ClearConfirmSheet`: lista os códigos, os efeitos e as condições, com Cancelar/Apagar; ao confirmar, grava `begin(...)` no `FaultLog` **antes** de chamar `clearFaults()`, e ao receber `Done` grava `finish(...)`, poda marcas e mostra o cartão de resultado com `clearOutcomeText`. Durante `Clearing`, indicador e botões desativados. "Ver vídeo" abre `ACTION_VIEW`; sem app, mostra "Nenhum app para abrir o vídeo".
- [ ] **Step 4:** `./gradlew testSimDebugUnitTest assembleSimDebug assembleDeviceDebug`. Expected: tudo verde.
- [ ] **Step 5: Commit** um por arquivo.

---

### Task 8: Fechamento, documentação e verificação

**Files:**
- Modify: `README.md`, `README.pt-BR.md`, `docs/specs/2026-11-17-fase2-veiculo-radio-midia-design.md` (nota na seção 5)
- Create: `docs/screenshots/faults-*.png`

- [ ] **Step 1:** README (EN e PT-BR): trocar "Nunca apaga códigos" pelo novo comportamento (apaga só a pedido, com confirmação, parado e verificado por releitura; descreve marcas locais), acrescentar ao roteiro. Na spec da fase 2, nota apontando que a seção 5 foi alterada por esta spec.
- [ ] **Step 2: Checklist de verificação** (emulador, `sim`): lista com descrições; linha expandida; seleção e marcar/desmarcar; confirmar e apagar (`Faults` = limpo); `FaultsPersistent` mostra `Returned`; `ClearRefused` mostra a orientação do motor; carro em movimento bloqueia o botão e o vídeo; sem adaptador não mostra o botão; marca some ao apagar; histórico guardado; Dia/Noite e 240 dpi; animações reduzidas.
- [ ] **Step 3:** Capturas de tela para `docs/screenshots/` (lista, confirmação, resultado).
- [ ] **Step 4:** Suíte inteira e os dois builds verdes.
- [ ] **Step 5: Commit** um por arquivo. **Push só se o usuário pedir.**

---

### Fase 3 (com o adaptador e o carro)

Executar o checklist da seção 11 da spec no Civic e ajustar as constantes de espera e a mensagem de recusa conforme o comportamento real. Só então considerar a feature validada.
