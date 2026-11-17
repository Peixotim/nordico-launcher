# Nórdico fase 2: Veículo (OBD-II), Rádio e Mídia (design)

Data: 2026-11-17 · Status: rascunho para aprovação · Continua a [fase 1](2026-11-15-launcher-fase1-design.md)

## 1. Intenção e porquê

A central chegou, mas **não será usada em bancada**: o primeiro teste real acontece já instalada no Civic. Por isso a fase 2 prepara tudo o que dá para preparar **sem o hardware**: telas, contratos de dados, protocolo e testes. No dia da instalação, só falta ligar as pontas.

Três módulos, todos **dentro do Nórdico** (mesmo app, mesmo design system, mesma estrutura `sim`/`device`):

1. **Veículo:** painel com dados do carro lidos pela porta OBD-II (velocidade, rotação, temperatura do arrefecimento, bateria, combustível…).
2. **Rádio:** tela própria de FM/AM no estilo do Nórdico.
3. **Mídia:** Spotify (e qualquer app de música) com player mais completo: barra de progresso que se arrasta, aleatório, repetir e curtir.

Uso pessoal. A meta continua sendo a interface com cara de montadora, minimalista, uma única cor de destaque, animações com mola.

## 2. O que já se sabe do hardware e do adaptador

| Item | Valor |
|---|---|
| Central | TorqueHammer VPL-1166, Android 14, Wi-Fi dual, Bluetooth 5.0, Play Store disponível, internet disponível |
| Rádio | Chip TSC4745, FM 76–108 MHz e AM 520–1710 kHz, 18 presets de FM e 12 de AM (spec do fabricante) |
| Carro | Honda Civic LXR 2.0 automático 2015/16 (o anúncio do adaptador lista "Honda a partir de 2007"). O protocolo esperado é CAN 500 kbit/s (ISO 15765-4), **a confirmar** |
| Adaptador | ELM327 com PIN de pareamento (1234, 0000…), o que indica **Bluetooth clássico (SPP)**. O anúncio diz "funciona em iPhone", o que só seria possível com BLE; as duas afirmações juntas não batem, então **não se sabe qual é** até ele chegar |

**Premissa a verificar na central:** o Bluetooth dela pode ser só para áudio e chamadas, tratado pelo microcontrolador do fabricante, sem aparecer no Android. Se for o caso, o SPP não funciona e o plano B é um adaptador **ELM327 Wi-Fi** (o transporte TCP, que já faremos para testes, cobre isso).

## 3. Fases

1. **Fase 2 (este documento):** os três módulos com dados simulados, protocolo OBD completo e testado, transporte TCP (testes e Wi-Fi) e transporte Bluetooth clássico. Roda no emulador.
2. **Fase 3 (central instalada):** reconhecimento (seção 12), confirmar o transporte do adaptador, ligar o rádio ao fabricante, ar-condicionado, farol e volume do MCU.
3. **Futuro:** biblioteca do Spotify (precisa de cadastro no portal do Spotify, ação sua), avisos por cima de outros apps, consumo de combustível.

## 4. Objetivos

- **Veículo:** tela com velocidade, rotação, temperatura do arrefecimento, tensão da bateria, nível de combustível, carga do motor, acelerador e temperatura do ar de admissão, mais leitura dos códigos de falha (somente leitura).
- **Aviso de alerta** (HUD) quando a temperatura do arrefecimento passa de 105 °C ou a tensão da bateria sai da faixa.
- **Rádio:** tela com frequência grande, escala sintonizável, seek, 18 presets de FM e 12 de AM, estados "sem rádio disponível" e "sintonizando".
- **Mídia:** barra de progresso arrastável e botões de aleatório, repetir e curtir quando o app de música expõe essas ações.
- Todo acesso a hardware atrás de interface, com versão simulada e versão real.
- Protocolo OBD (decodificação, sessão ELM327, agendador) em **código puro, testado na JVM** com respostas reais do ELM327.
- O caminho completo do protocolo roda no emulador contra um simulador de ELM327 no PC.

## 5. Não-objetivos

- **Escrever no carro.** Nada de apagar códigos de falha, reiniciar a ECU ou atuar em qualquer coisa. O anúncio do adaptador vende "apagar luz de injeção"; o Nórdico só **lê**.
- Consumo instantâneo de combustível. Depende de PID de fluxo de ar que muitos Honda não têm, e o carro é flex (a mistura muda a conta). Fica para o futuro, calibrável.
- PIDs proprietários da Honda (câmbio CVT, ventoinha): só depois de descobrir quais existem.
- Biblioteca e playlists do Spotify (precisa de Client ID do Spotify).
- Leitura do OBD com o Nórdico em segundo plano e serviço em primeiro plano. Sem os avisos por cima de outros apps, não teria como mostrar o alerta, então fica junto com eles.
- Gravação de trajetos, histórico e gráficos.
- Telas de Equalizador, Câmera 360 e Telefone.

## 6. Anti-objetivos

- Qualquer comando ao carro que não seja leitura.
- Tela de OBD poluída ou que distraia na direção: poucos números grandes, legíveis de relance.
- Travar ou piscar a interface por causa do polling.
- Perder a conexão sem avisar, ou ficar tentando reconectar de forma agressiva.
- Quebrar a fase 1 (ela continua funcionando sem adaptador, sem rádio e sem Spotify).

## 7. Restrições

- Mesmas da fase 1: tela lógica 1280×720 dp, cores e fontes do design system, mola padrão, modo Dia e Noite, respeitar "reduzir animações".
- Android 14: precisa da permissão `BLUETOOTH_CONNECT` em tempo de execução para o Bluetooth.
- O polling só roda com o Nórdico visível (`STARTED`) e só enquanto alguém coleta os dados.
- Os alertas usam uma cor de aviso (vermelho do sistema `#FF3B30` e âmbar `#FF9F0A`), nunca a cor de destaque.

## 8. Abordagens consideradas

**Onde fica o código novo**
- **Módulos dentro do Nórdico: escolhida.** Mesmo design system, mesma navegação, um processo só.
- APKs separados: só serviria para um serviço de OBD em segundo plano, que está fora do escopo.

**Transporte do adaptador**
- **Interface `ObdTransport` com duas implementações: escolhida.** TCP (simulador no PC e adaptadores Wi-Fi) e Bluetooth clássico SPP (o mais provável pelo PIN). **BLE** só se o adaptador for BLE, depois de ele chegar.
- Biblioteca pronta de OBD: descartada. As existentes são Java antigas, sem manutenção e acopladas ao transporte; o protocolo ELM327 é pequeno e fica melhor testado nas nossas mãos.

**Como testar sem o carro**
- **Simulador de ELM327 no PC conectado por TCP: escolhido**, além do `SimObdRepository` (dados direto, sem protocolo) para a interface.
- Só dados falsos: descartado, porque não exercita o parser.

**Spotify**
- **Player próprio sobre a MediaSession: escolhido.** Não exige cadastro, funciona com qualquer app de música e já está em uso.
- App Remote e Web API: ficam para depois (exigem Client ID e, para controle, Premium).

**Rádio**
- Contrato próprio (`RadioRepository`) com versão simulada, e versão real "indisponível" até o reconhecimento. Se o fabricante não expuser controle do rádio, o atalho abre o app do fabricante.

## 9. Design

Mesmas regras visuais da fase 1 (spec 9). Os protótipos HTML das telas novas são o **primeiro passo do plano** e precisam da sua aprovação antes de qualquer código de interface.

### 9.1 Atalhos do Início

Os 4 atalhos passam a ser **Rádio, Veículo, Câmera e Apps**. O atalho Telefone sai (o CarPlay/ZLINK já cobre ligações) e vira um item na gaveta de apps.

### 9.2 Tela Veículo (1280×720 dp)

- Cabeçalho com voltar, "Veículo", chip de conexão ("Conectado · ELM327 v1.5") e botão "Falhas".
- À esquerda, um cartão grande: **velocidade** em Inter Display Light de 176 dp e, ao redor, um arco de **rotação** de 0 a 8.000 rpm.
- À direita, grade de cartões: **Arrefecimento** (°C com barra colorida: azul abaixo de 70, neutro de 70 a 100, âmbar de 100 a 104, vermelho acima de 105), **Bateria** (V), **Combustível** (%), **Carga do motor** (%), **Acelerador** (%), **Ar de admissão** (°C).
- Valores não suportados pelo carro aparecem como "—" em `label3`, sem esconder o cartão.
- **Sem adaptador:** estado vazio com "Conecte o adaptador OBD" e botão "Escolher adaptador".
- **Escolher adaptador:** folha com os dispositivos Bluetooth já pareados (nome e endereço) e o atalho "Parear novo", que abre os ajustes de Bluetooth do Android.
- **Falhas:** folha com a lista de códigos (ex.: "P0133") e a descrição em português quando conhecida; vazia mostra "Nenhuma falha registrada". Somente leitura, sem botão de apagar.

### 9.3 Tela Rádio

- Banda (FM / AM) em um segmentado, frequência em Inter Display Light de 120 dp, nome da estação quando houver, escala horizontal que se arrasta para sintonizar.
- Botões de seek (‹ ›) e passo fino (−/+), e a fileira de **presets** (18 em FM, 12 em AM; tocar chama, segurar salva a frequência atual).
- Indisponível: cartão "Rádio do fabricante" com botão que abre o app dele (quando conhecido).

### 9.4 Tocando agora

- A barra de progresso vira um controle arrastável (a posição só é enviada ao soltar).
- Linha de ações opcionais: aleatório, repetir e curtir, que só aparecem se o app expõe a ação.

### 9.5 Aviso de alerta

Pílula no topo (mesmo HUD), ícone de aviso, texto curto ("Temperatura alta · 108 °C") em vermelho de sistema. Fica 6 s e se repete a cada 60 s enquanto a condição durar.

## 10. Comportamento

### 10.1 Protocolo OBD

- **Inicialização** ao conectar: `ATZ`, `ATE0` (sem eco), `ATL0`, `ATS0` (sem espaços na resposta), `ATH0` (sem cabeçalhos), `ATSP0` (protocolo automático). Em seguida `ATI` (versão, mostrada no chip), `ATDPN` (protocolo detectado) e a consulta dos PIDs suportados (`0100`, `0120`, `0140`, `0160`).
- **Respostas:** o parser remove eco residual, `SEARCHING...`, `BUS INIT`, o prompt `>` e espaços. `NO DATA`, `UNABLE TO CONNECT`, `CAN ERROR`, `STOPPED` e `?` viram erros tipados, não exceções.
- **PIDs lidos (modo 01):** `0D` velocidade, `0C` rotação, `05` temperatura do líquido, `0F` ar de admissão, `04` carga, `11` acelerador, `2F` combustível, `42` tensão do módulo. Só são pedidos os que o carro declara suportar.
- **Códigos de falha:** modo `03` (somente leitura). Em CAN o primeiro byte depois do `43` é a quantidade.
- **Agendamento:** velocidade e rotação a cada ~250 ms; carga e acelerador a cada ~1 s; temperaturas, tensão e combustível a cada ~2 s. Pede sempre o PID mais atrasado. Um PID que responde `NO DATA` três vezes seguidas é marcado como não suportado e deixa de ser pedido.
- **Conexão:** estados `SemAdaptador`, `Conectando`, `Conectado(protocolo, versão)`, `Perdida`. Reconexão automática com espera crescente (2 s, 4 s, 8 s, no máximo 30 s) e só com o Nórdico visível. A seleção do adaptador fica salva.
- **Adaptador clone:** versões "v2.1" de baixa qualidade costumam rejeitar comandos. Comandos de configuração rejeitados são ignorados; a versão aparece no chip para o diagnóstico.

### 10.2 Alertas

- Arrefecimento ≥ 105 °C: alerta; volta ao normal só abaixo de 102 °C (histerese de 3 °C).
- Tensão < 11,8 V com o motor ligado (rotação > 0) ou > 15,0 V: alerta.
- Os mesmos limites ficam em constantes únicas, ajustáveis na calibração.

### 10.3 Rádio

- FM: 76,0 a 108,0 MHz em passos de 0,1. AM: 520 a 1710 kHz em passos de 10. A frequência é guardada em kHz (inteiro) nas duas bandas.
- Seek procura a próxima estação; no simulador, as estações são fixas.
- Os presets pertencem ao Nórdico e são salvos nas preferências, por banda, mesmo que o sintonizador seja do fabricante.

### 10.4 Mídia

- `seekTo(posição)` e ações personalizadas da MediaSession. Quando o app não expõe uma ação, o botão não aparece.

## 11. Arquitetura

Segue a fase 1: um módulo `:app`, interfaces em `data/`, `sim`/`device` por flavor, injeção manual.

```
data/obd/        ObdPid, ObdParser, DtcDecoder, ObdSession (ElmSession), PollScheduler,
                 ObdAlerts, ObdTransport, ObdRepository, ElmObdRepository
data/radio/      RadioRepository, RadioState, RadioMath (passos, limites, presets)
data/media/      MediaRepository (+ seekTo, customActions)
feature/vehicle/ VehicleScreen, Gauge, AdapterSheet, FaultsSheet
feature/radio/   RadioScreen, FrequencyDial, PresetRow
feature/nowplaying/  barra arrastável, ações
app/src/sim/     SimObdRepository, SimRadioRepository, painel de simulação ampliado
app/src/device/  BluetoothSppTransport, DeviceRadioRepository (indisponível por ora)
```

### 11.1 Contratos

```kotlin
data class VehicleTelemetry(
    val speedKmh: Int?, val rpm: Int?, val coolantC: Int?, val intakeC: Int?,
    val loadPct: Int?, val throttlePct: Int?, val fuelPct: Int?, val batteryV: Double?,
)                                   // null = não suportado ou sem leitura

sealed interface ObdState {
    data object NoAdapter : ObdState
    data object Connecting : ObdState
    data class Connected(val protocol: String, val adapter: String) : ObdState
    data object Lost : ObdState
}

data class Dtc(val code: String)    // "P0133"

interface ObdRepository {
    val state: StateFlow<ObdState>
    val telemetry: StateFlow<VehicleTelemetry>
    val faults: StateFlow<List<Dtc>?>   // null = ainda não lido
    fun bondedAdapters(): List<ObdAdapter>
    fun select(address: String)
    fun readFaults()
}

interface ObdTransport { suspend fun open(); suspend fun send(cmd: String): String; fun close() }

enum class Band { Fm, Am }
data class RadioState(val band: Band, val khz: Int, val station: String?, val stereo: Boolean)
interface RadioRepository {
    val available: StateFlow<Boolean>
    val state: StateFlow<RadioState>
    fun setBand(b: Band); fun tune(khz: Int); fun seek(dir: Int)
}
```

`MediaRepository` ganha `fun seekTo(ms: Long)` e `val actions: StateFlow<List<MediaAction>>` com `fun perform(id: String)`.
`Prefs` ganha `obdAdapter: String?` e os presets de rádio.

### 11.2 Simulação

- `SimObdRepository`: cenários controlados pelo painel (parado, ligado em marcha lenta, estrada a 90 km/h, **superaquecendo**, bateria fraca, sem adaptador).
- `SimRadioRepository`: estações fixas, seek de verdade.
- **Protocolo no emulador:** com `-PobdTcp=10.0.2.2:35000` o flavor `sim` usa `ElmObdRepository` sobre TCP, conectado a um simulador de ELM327 rodando no PC.

## 12. Dia da instalação (checklist de reconhecimento)

Com a central no carro, ADB por Wi-Fi (hotspot do celular):
1. `adb shell service list | grep -i qf` e `pm list packages | grep -i -E "qf|can|radio|zlink|toppal"`: identifica a plataforma.
2. `adb shell pm list features | grep -i bluetooth` e `dumpsys bluetooth_manager`: confirma se o Bluetooth está no Android.
3. Instalar o Nórdico (`installDeviceDebug`), definir como tela inicial, confirmar que o fabricante não bloqueia.
4. Plugar o adaptador, parear (PIN do anúncio), escolher no Nórdico e ver o chip: versão e protocolo.
5. Ligar o carro e confirmar quais PIDs ele aceita.
6. Procurar o controle do rádio (intents e serviços do app do fabricante).

## 13. Testes e verificação

- **JVM:** decodificação de cada PID com valores conhecidos, limpeza de respostas reais (`SEARCHING...`, eco, `NO DATA`, várias linhas), máscara de PIDs suportados, decodificação de falhas, sessão ELM327 com transporte falso roteirizado (inicialização, PID não suportado, queda e reconexão), agendador, regras de alerta com histerese, matemática do rádio e dos presets.
- **Emulador:** cada tela em Dia e Noite, comparada com os protótipos aprovados; cenários do painel de simulação; alerta aparecendo e repetindo.
- **Protocolo ponta a ponta:** `sim` conectado ao simulador de ELM327 por TCP, verificando valores na tela.
- **Bluetooth clássico:** só dá para testar na central real. Fica registrado como verificação manual do dia da instalação.

## 14. Riscos

- O Bluetooth da central pode não ser acessível pelo Android (seção 2): plano B é adaptador Wi-Fi.
- O adaptador pode ser clone de baixa qualidade e falhar em alguns comandos.
- O rádio pode não ter nenhum controle exposto pelo fabricante: o atalho abre o app dele.
- Alguns PIDs podem não existir no Civic G9: a tela mostra "—".
- Carga extra do polling no SoC modesto: medir na central.

## 15. Decisões tomadas por mim (revisar)

- O atalho Telefone sai da tela inicial para dar lugar ao Veículo.
- Só leitura no OBD, sem apagar falhas (o anúncio do adaptador vende "apagar luz de injeção").
- Sem consumo de combustível nesta fase.
- Limites de alerta: 105 °C (histerese 3 °C), tensão fora de 11,8–15,0 V.
- Primeiro Bluetooth clássico e TCP; BLE só se o adaptador pedir.
- O polling para quando o Nórdico sai da frente.
- Presets de rádio guardados pelo Nórdico, não pelo fabricante.
- Biblioteca do Spotify fica de fora até você cadastrar o app no portal do Spotify.

## 16. Deixado para quem implementar

- Protocolo exato de conexão do SPP (UUID `00001101-0000-1000-8000-00805F9B34FB`, tentativa insegura como alternativa) e os tempos de espera.
- Descrições em português dos códigos de falha mais comuns.
- Organização interna dos arquivos de cada tela.
