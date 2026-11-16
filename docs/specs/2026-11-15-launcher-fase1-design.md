# Launcher "Nórdico" para central Android: fase 1 (design)

Data: 2026-11-15 · Status: rascunho para aprovação

## 1. Intenção e porquê

O dono de um Honda Civic LXR 2.0 2015/2016 (última geração 9) comprou uma central multimídia Android chinesa (TorqueHammer VPL-1166). A interface de fábrica é Android genérico, feia e amadora. Ele quer uma experiência **nível montadora**, com referências em VW (Jetta GLI 2025), Fiat (Toro 2025) e principalmente **Apple**: minimalismo, cantos arredondados e animações fluidas. A interface não pode ter "cara de IA" nem de Android.

A solução **não é** uma ROM nova. É um **app launcher** (tela inicial) com telas próprias, instalado por cima do Android de fábrica. As funções do fabricante continuam funcionando por baixo. É uma solução reversível e sem root.

Uso pessoal, em um carro.

## 2. Hardware alvo

| Item | Valor |
|---|---|
| Central | TorqueHammer VPL-1166, tela 9" QLED 1280×720, touch capacitivo |
| SoC | Unisoc UIS8581A, octa-core 1.6 GHz (chip modesto) |
| Memória | 6 GB RAM, 128 GB |
| Android | 14 |
| Áudio | DSP BD37534, saída 4×45 W |
| Rádio | TSC4745 (FM 76–108 MHz, AM 520–1710 kHz) |
| Câmeras | AHD; 360° via TP2815; ré com disparo automático |
| Integração | Chicote Honda com caixa CAN (marca/modelo desconhecidos), comandos de volante, ZLINK (CarPlay/Android Auto sem fio), voz Toppal, iGO |
| Carro | Ar-condicionado digital automático (zona única), comandos físicos |

**A central ainda não chegou.** Não sabemos qual plataforma de software está por baixo (provavelmente uma base tipo FYT), nem se a caixa CAN entrega dados do ar. Por isso a fase 1 roda inteira com **dados simulados**.

## 3. Fases

1. **Fase 1 (este documento):** design system + Início, Tocando agora, Clima, Apps e avisos (HUD), com fontes de dados por interface. Dá para construir e testar no emulador.
2. **Fase 2:** telas de sistema (Rádio, Equalizador, Câmera 360, Telefone, Ajustes). Depende de mockups que ainda serão feitos.
3. **Fase 3:** integração real com a central (plataforma, caixa CAN, farol, rádio, volume do MCU) quando o hardware chegar.
4. **Futuro:** controle do ar pela tela (exige módulo CAN próprio, um projeto de hardware à parte).

## 4. Objetivos da fase 1

- O app é instalável como **tela inicial (HOME)** no Android 14 e roda no emulador 1280×720.
- Reproduz fielmente os protótipos aprovados (seção 13), em modo Dia e Noite.
- Animações a **60 fps** com curvas de mola. Fluidez é requisito central.
- Toda dependência de hardware fica atrás de interfaces com uma versão **simulada** (`sim`) e uma **real** (`device`, parcial nesta fase).
- O que já é Android padrão funciona de verdade no flavor `device`: lista de apps, mídia (MediaSession), volume do Android, hora, conectividade, localização.

## 5. Não-objetivos da fase 1

- Telas de Rádio, Equalizador, Câmera 360, Telefone e Ajustes (fase 2).
- Integração com caixa CAN, farol e APIs do fabricante (fase 3).
- Controle do ar-condicionado (futuro).
- Avisos (HUD) por cima de **outros apps** (exige permissão de sobreposição; fase 3). Na fase 1 os avisos aparecem só dentro do launcher.
- Reordenar apps arrastando, widgets de terceiros, tela dividida, atualização OTA e voz.

## 6. Anti-objetivos (fracasso mesmo que "funcione")

- Parecer Android genérico ou "feito por IA": degradês roxos, brilhos, várias cores de destaque, ícones misturados de estilos diferentes.
- Engasgar: animação travando, tela inicial demorando a aparecer.
- Quebrar funções do fabricante: câmera de ré, comandos de volante, ZLINK, rádio.
- Exigir root ou fazer qualquer coisa irreversível na central.

## 7. Restrições

- **Fontes e ícones da Apple são proibidos fora de aparelhos Apple** (licença). Usar **Inter** (OFL) e **Material Symbols Rounded** (Apache 2.0).
- **Câmera de ré é segurança.** Quem dispara e desenha a câmera é o fabricante, e o launcher nunca interfere nisso.
- Chip modesto: desfoque e efeitos pesados com moderação (ver 9.5).
- Densidade de tela desconhecida (centrais costumam vir com 160 ou 240 dpi). O layout precisa ficar idêntico independente disso (ver 10.4).

## 8. Abordagens consideradas

**Plataforma**
- ROM customizada: descartada. Perde drivers, rádio, câmeras e CAN, com risco de inutilizar a central.
- **Launcher + telas próprias por cima do Android de fábrica: escolhida.**

**Tecnologia**
- **Kotlin + Jetpack Compose (nativo): escolhida.** Melhor desempenho no chip, acesso direto às APIs do Android, e molas e transições compartilhadas nativas.
- React Native + TypeScript: descartada. A integração com o sistema seria em Kotlin de qualquer jeito (duas linguagens mais a ponte), e o motor JavaScript pesa no chip.
- WebView com React: descartada. É a opção mais lenta, contrária ao requisito de fluidez.
- React Flow: não se aplica (é uma biblioteca de diagramas).

**Arquitetura de código**
- **A: um módulo `:app`, pastas por funcionalidade: escolhida.** É simples e combina com um desenvolvedor só.
- B: multimódulo estilo "Now in Android": descartada por ora. Muita configuração de Gradle no início. Dá para migrar depois.
- Clean Architecture completa com casos de uso: descartada. Cerimônia demais para um app que é quase todo UI.

**Direção visual** (escolhida entre 4 opções em mockup): **"C · Nórdico"** (mapa protagonista, estilo Volvo/Polestar) somada à **linguagem Apple**. Para volume e brilho venceu o **"Painel A"** (estilo VW/Honda).

## 9. Design system

Fonte da verdade visual: os protótipos em `docs/prototipos/` (seção 13). As medidas abaixo estão em **dp**, numa tela lógica de **1280×720 dp** (1 px do protótipo = 1 dp).

### 9.1 Cores (inspiradas no Apple HIG)

| Token | Dia | Noite |
|---|---|---|
| bg | `#F2F2F7` | `#000000` |
| surface | `#FFFFFF` | `#1C1C1E` |
| label | `#000000` | `#FFFFFF` |
| label2 | `rgba(60,60,67,.60)` | `rgba(235,235,245,.60)` |
| label3 | `rgba(60,60,67,.30)` | `rgba(235,235,245,.30)` |
| separator | `rgba(60,60,67,.16)` | `rgba(84,84,88,.55)` |
| fill3 | `rgba(118,118,128,.14)` | `rgba(118,118,128,.24)` |
| fill2 | `rgba(118,118,128,.22)` | `rgba(118,118,128,.34)` |
| material (sobre o mapa) | `rgba(255,255,255,.78)` | `rgba(40,40,42,.78)` |
| segmentOn | `#FFFFFF` | `#48484A` |
| shadow (cards) | `0 1 2 /3%` + `0 6 20 /5%` | sem sombra |

**Cor de destaque:** uma só, usada com parcimônia (bolinhas de sugestão, botão recentralizar, chip do ar, itens ativos). Padrão **laranja** (`#F2620F` dia / `#FF7A2E` noite). Alternativas guardadas em preferência: azul (`#007AFF`/`#0A84FF`), verde (`#248A3D`/`#30D158`), grafite (`#3A3A3C`/`#AEAEB2`). Não há tela para trocar na fase 1.

**Cores de temperatura** (só na régua do Clima): frio < 21 °C `#0A84FF`; 21–25 °C usa `label`; quente > 25 °C `#FF9F0A`.

**Ponto de localização:** sempre azul `#0A84FF` com borda branca (convenção).

### 9.2 Tipografia

- **Inter** em pesos 300, 400, 500, 600 e 700, embutida no app.
- **Inter Display** para números grandes (≥ 32 sp): relógio, temperatura e volume.
- Números com `tabular-nums` (fontFeatureSettings `"tnum"`).
- Tamanhos principais: relógio 88 Light (tracking −4.5%); temperatura do Clima 176 ExtraLight (tracking −6%); título de página 34 Bold; título do card de música 21 SemiBold; texto 15–17; legendas 12–14.

### 9.3 Ícones

Material Symbols **Rounded**, peso 400, nas variantes vazada e preenchida. Vêm da **fonte variável oficial** (`MaterialSymbolsRounded[FILL,GRAD,opsz,wght].ttf`, ~15 MB) desenhada por ligadura, igual aos protótipos; o eixo `FILL` controla vazado ou preenchido. Revisado no plano: substitui os vector drawables, com o mesmo visual e sem conversão. Lista inicial: `home, navigation, search, mic, near_me, work, chevron_left, chevron_right, keyboard_arrow_down, bluetooth, signal_cellular_alt, wifi, skip_previous, skip_next, play_arrow, pause, call, radio, photo_camera, apps, remove, add, volume_up, volume_down, volume_off, brightness_auto, light_mode, dark_mode, power_settings_new, ac_unit, mode_fan, thermostat, device_thermostat, air, hdr_auto, sync, windshield_defrost_front, windshield_defrost_rear, info, music_note, map, explore, cast, library_music, movie, equalizer, 360, input, folder, photo_library, directions_car, settings`.

### 9.4 Forma

- Cantos **contínuos (squircle)**, não círculo puro. Implementar um `Shape` de superelipse. Fallback aceitável: `RoundedCornerShape`.
- Raios: mapa 30; cards grandes 24–26; tiles 24; botões do painel A 18–20; ícones de app 24 (em 88 dp); chips 18; HUD 30/36 (pílula).

### 9.5 Materiais e desfoque

- **Fase 1 sem desfoque de fundo.** Superfícies "material" sobre o mapa usam cor semi-opaca (tabela 9.1). Se ficar chapado demais, avaliar a biblioteca Haze depois de medir desempenho na central.
- **Fundo do Tocando agora:** a capa reduzida para ~32 px e esticada, escurecida (brilho 50%, saturação +60%). O desfoque sai de graça, sem custo de GPU.

### 9.6 Movimento

| Uso | Especificação |
|---|---|
| Mola padrão | `spring(dampingRatio = 0.82f, stiffness = 158f)` (≈ resposta 0,5 s, ~1% de passagem) |
| Toque | encolhe para 0.94 em 120 ms (ease-out); solta com mola padrão |
| Card/tile → tela cheia (Tocando agora, Apps, Clima) | transição de elemento compartilhado com mola padrão (~0,6 s) |
| Troca Dia/Noite | cores interpolam em 450 ms; o indicador do segmentado desliza com mola |
| HUD | entra com mola (de −16 dp e escala 0.94); sai com fade de 220 ms; some após 1,8 s (volume) ou 2,6 s (ar) |
| Modo edição dos apps | ícones tremem ±1,4° em ciclo de 340 ms com fases defasadas; selo −/+ entra com mola |
| Reorganização ao ocultar app | itens animam até a nova posição (`animateItem`) |
| Ventoinha do Clima | ícone gira; duração por volta = 1,5 − 0,17 × velocidade (s) |

Tudo respeita "reduzir animações" do sistema (sem animação nesse caso).

### 9.7 Componentes do design system

`NordicTheme` (tokens, Dia/Noite, destaque), `SquircleShape`, `Card`, `PressScale` (modifier), `SegmentedControl` (indicador com mola), `Pill`, `Hud` (base de avisos), `SegmentMeter` (régua de N segmentos), `IconPlate` (ícone de app em 88 dp), `PageHeader` (voltar + título + ação).

## 10. Telas e comportamento

Layout geral do Início (1280×720 dp): mapa à esquerda (x 16, y 16, 760×688, raio 30); coluna direita de x 798 até 1258, entre y 20 e y 704, com conteúdo distribuído verticalmente (`SpaceBetween`).

### 10.1 Início

**Mapa (esquerda)**
- MapLibre Android com estilos OpenFreeMap `positron` (dia) e `dark` (noite), sem interação de arrastar/zoom na fase 1.
- Ponto de localização no GPS da central. No flavor `sim`, posição fixa na Av. Paulista (−23.5614, −46.6558), zoom 15.4.
- **"Para onde?" (pílula):** abre o app de navegação preferido.
- **Card de sugestões "Casa" / "Trabalho":** abre a navegação até o destino salvo.
- **Botão recentralizar:** recentraliza no GPS.
- O estilo noturno ficou escuro demais no protótipo. Ajustar as cores do estilo (ruas e fundo mais claros) ao gosto.

**Cabeçalho (direita)**
- Relógio do sistema e data por extenso em pt-BR ("domingo, 15 de novembro").
- Temperatura externa (do `VehicleRepository`; some se não houver dado).
- Ícones de Bluetooth, celular e Wi-Fi com estado real.
- **Chip do ar** ("22,5° · Auto"): abre o Clima com transição compartilhada. **Oculto se não houver dados do ar.**

**Card Tocando agora (mini)**
- Capa 80, título, artista, barra de progresso e anterior / tocar-pausar / próxima.
- Tocar no card fora dos botões abre o Tocando agora em tela cheia.
- **Sem mídia ativa:** card mostra estado vazio ("Nada tocando") com atalho para o app de música preferido.

**Atalhos (4 tiles):** Telefone, Rádio, Câmera e Apps. Na fase 1, Telefone, Rádio e Câmera abrem os apps do fabricante (ou nada no `sim`), e Apps abre a gaveta com transição compartilhada.

**Painel A**
- **Volume:** botões − e + de 58 dp. Segurar repete após 380 ms, a cada 85 ms. No centro ficam o ícone (mudo / baixo < 14 / alto), o número de 0 a 40 e uma régua de 40 segmentos. Tocar no centro alterna mudo, e o + sai do mudo.
- **Modo da tela:** segmentado Auto / Dia / Noite. **Auto** usa o farol (`VehicleRepository.headlightsOn`) quando disponível; senão usa horário fixo (Noite das 18h às 6h). O modo escolhido fica salvo.
- **Tela:** desliga a tela (fase 1: camada preta por cima com relógio fraco; o toque religa). Desligar a luz de fundo de verdade fica para a fase 3.

### 10.2 Tocando agora (tela cheia)

- Fundo com a capa desfocada (ver 9.5).
- Capa de 420 com transição compartilhada a partir do mini card.
- Origem ("iPhone · Bluetooth" / nome do app), título 46 Bold, artista 24, barra de progresso, controles grandes e botão de fechar (seta para baixo).
- Volume nesta tela: usar o mesmo componente do painel A em versão escura. O slider do protótipo 01 está obsoleto.

### 10.3 Clima (somente leitura)

- Cabeçalho com voltar, "Clima" e o aviso "Somente leitura · o ajuste continua nos botões do painel".
- **Card temperatura:** valor definido com uma casa decimal e vírgula (18,0–32,0, passo 0,5), °C, selo AUTO, estado do A/C e régua de 29 segmentos (18 → 32) colorida conforme 9.1.
- **Ventilação:** 7 barras crescentes, texto "Automática · 3 de 7" ou "Manual · …" e ventoinha girando.
- **Externa:** temperatura grande.
- **Direção do ar:** Rosto / Rosto e pés / Pés / Pés e para-brisa, com o ativo destacado.
- **Estados:** AUTO, A/C, Recircular, Desemb. frontal e Desemb. traseiro (chips com ativo destacado).
- **Indisponível** (sem dados): a tela não é alcançável (chip e app "Clima" ocultos).

### 10.4 Densidade e escala

- O app desenha sempre numa tela lógica de **1280×720 dp**. Na `MainActivity`, sobrescrever `LocalDensity` com `density = larguraEmPx / 1280f` e `fontScale = 1f`. O layout fica idêntico em 160 ou 240 dpi.
- Deixar o fator ajustável numa constante, para calibrar na central real.

### 10.5 Apps (gaveta)

- **Cabeçalho:** voltar, "Apps", busca (filtra por nome ignorando acentos) e "Editar"/"Concluir" na cor de destaque.
- **Seções:**
  - **Seus apps:** instalados pelo usuário.
  - **Do carro:** apps do fabricante e do sistema que tenham ícone de abertura.
  - **Ocultos:** aparece só se houver algum.
- **Grade:** 8 colunas, ícone 88 com raio 24, nome 14 Medium com reticências.
- **Ícones:**
  - Apps de terceiros usam o ícone oficial (adaptive icon) recortado no formato squircle.
  - Apps do fabricante ganham **ícones próprios**: glifo Material sobre placa `surface`, todos no mesmo estilo. O mapeamento pacote → glifo + nome em pt-BR fica numa tabela, preenchida de verdade na fase 3. Sem mapeamento, usar o ícone original recortado.
- **Edição:** segurar um ícone por 550 ms ou tocar em Editar. Os ícones tremem, aparece o selo −, e tocar oculta o app (ele vai para Ocultos com animação). Em Ocultos o selo é +, e tocar restaura. Sair da gaveta encerra a edição. A lista de ocultos fica salva.
- Tocar num app (fora da edição) abre o app. O "Clima" interno abre a tela Clima.
- A lista atualiza quando apps são instalados ou removidos.

### 10.6 Avisos (HUD)

- **Volume:** pílula no topo central com ícone, régua de 20 segmentos e número. Aparece quando o volume muda **por fora** da tela (volante, botão físico), nunca pelos botões do painel A.
- **Ar:** pílula maior com ícone, temperatura, ventoinha + 7 mini barras e selo AUTO. Aparece quando o estado do ar muda e a tela Clima não está aberta. Tocar abre o Clima.
- Os dois respeitam os tempos da seção 9.6.

### 10.7 Navegação entre telas

- Estado simples (`Home`, `NowPlaying`, `Apps`, `Climate`), com transições de elemento compartilhado (`SharedTransitionLayout` + `AnimatedContent`). Sem biblioteca de navegação.
- O botão voltar do Android volta ao Início. No Início, voltar não faz nada.

### 10.8 Comportamento de launcher

- `MainActivity` com intent-filter `MAIN` + `HOME` + `DEFAULT`, `launchMode="singleTask"` e orientação paisagem.
- Barras do sistema escondidas (imersivo; deslizar mostra temporariamente).

## 11. Arquitetura

Um módulo `:app`, pastas por funcionalidade. Injeção de dependências manual (`AppContainer`), sem Hilt. Cada tela tem um `ViewModel` com `StateFlow`.

```
app/src/main/java/<pacote>/
  MainActivity.kt          tela inicial (HOME), LocalDensity, imersivo
  App.kt                   Application; cria o AppContainer
  AppContainer.kt          liga interfaces às implementações do flavor
  navigation/              estado de tela + SharedTransitionLayout
  designsystem/            tema, tokens, tipografia, SquircleShape, componentes
  feature/home/            Início (mapa, cabeçalho, card mídia, tiles, painel A)
  feature/nowplaying/
  feature/climate/
  feature/apps/
  feature/hud/
  data/media/              MediaRepository
  data/apps/               AppsRepository
  data/audio/              VolumeRepository
  data/vehicle/            VehicleRepository (ar, farol, temp. externa)
  data/status/             StatusRepository (BT, celular, Wi-Fi)
  data/location/           posição para o mapa
  data/prefs/              PrefsRepository (DataStore)
app/src/sim/java/...       implementações simuladas + painel de simulação
app/src/device/java/...    implementações reais
```

**Flavors:** `sim` (emulador, dados simulados) e `device` (central).

### 11.1 Contratos de dados (interfaces)

| Interface | Expõe | `sim` | `device` (fase 1) |
|---|---|---|---|
| `MediaRepository` | `Flow<NowPlaying?>` (título, artista, capa, posição, duração, tocando, origem) + `play/pause/next/prev` | música fictícia tocando | `MediaSessionManager` + `NotificationListenerService` (pedir acesso a notificações na 1ª execução) |
| `AppsRepository` | `Flow<List<AppEntry>>` (pacote, nome, ícone, seção) + `launch(app)` | lista fictícia dos protótipos | `LauncherApps` / `PackageManager` (`<queries>` MAIN/LAUNCHER). Seção: `FLAG_SYSTEM` → "Do carro", senão "Seus apps" |
| `VolumeRepository` | `Flow<VolumeState>` (0–40, mudo, origem: interna/externa) + `set`, `step`, `toggleMute` | estado em memória | `AudioManager` `STREAM_MUSIC` reescalado para 0–40. Escuta mudanças externas para o HUD |
| `VehicleRepository` | `Flow<ClimateState?>`, `Flow<Boolean?>` farol, `Flow<Double?>` temp. externa | painel de simulação | retorna `null` (fase 3) |
| `StatusRepository` | BT, celular, Wi-Fi | fixos ligados | `ConnectivityManager` / `BluetoothAdapter` |
| `LocationRepository` | posição | fixa | `FusedLocation` não disponível sem GMS; usar `LocationManager` GPS |
| `PrefsRepository` | modo da tela, destaque, ocultos, casa/trabalho, app de navegação, app de música | DataStore | DataStore |

`ClimateState`: `setTemp: Double`, `fanSpeed: Int (1–7)`, `auto: Boolean`, `ac: Boolean`, `airflow: Face|FaceFeet|Feet|FeetDefrost`, `recirc: Boolean`, `defrostFront: Boolean`, `defrostRear: Boolean`.

### 11.2 Painel de simulação (`sim`)

- Aberto por um toque longo no relógio. É uma folha com os botões do protótipo 03: Temp ±, Vent ±, AUTO, A/C, Direção, Recircular, Desembaçadores, Volante Vol ±, Farol liga/desliga e "sem dados do ar".
- Existe só no flavor `sim`.

## 12. Testes e verificação

- **Testes JVM** das regras com lógica:
  - reescala de volume 0–40 e mudo;
  - resolução do modo Auto (farol → horário);
  - formatação da temperatura ("22,5");
  - cor da régua por faixa;
  - classificação de apps em seções, ocultos e busca sem acento;
  - regras do painel de simulação do ar (AUTO repõe ventilação e direção).
- **Verificação manual** no emulador: AVD 1280×720 em paisagem, Android 14. Conferir lado a lado com os protótipos em Dia e Noite.
- **Desempenho:** sem quedas visíveis nas transições (inspecionar no emulador com o perfil de GPU). Medição real na central fica para a fase 3.

## 13. Referências visuais (fonte da verdade)

Abrir no navegador:
- `docs/prototipos/03-inicio-painelA-clima-apps.html`: **principal**, com Início + painel A, Clima, avisos, Apps e Tocando agora.
- `docs/prototipos/01-inicio-tocando-agora.html`: Início original e Tocando agora. O slider de brilho/volume deste arquivo foi **substituído** pelo painel A.
- `docs/prototipos/02-controles-brilho-som.html`: comparação das 3 opções de controles. A opção vencedora é a **A**.

Em conflito entre protótipos, vale o 03.

## 14. Riscos conhecidos (para a fase 3)

- O fabricante pode bloquear launcher de terceiros ou forçar o próprio.
- O volume pode ser controlado pelo MCU e não pelo `AudioManager`, e o fabricante pode mostrar o próprio aviso de volume (duplicado).
- A caixa CAN pode não fornecer dados do ar nem do farol.
- O desempenho real do mapa vetorial e das transições no UIS8581A precisa ser medido.

## 15. Decisões tomadas por mim (revisar)

- Nome do app "Nórdico" e pacote `app.nordico.launcher`.
- Projeto Android na raiz desta pasta (`new-interface/`), com git inicializado.
- Sem desfoque de fundo na fase 1; fundo do Tocando agora feito com capa reduzida.
- Auto sem farol = horário fixo 18h–6h.
- Tela "desligada" na fase 1 é só uma camada preta.
- `minSdk` = `targetSdk` = 34. Kotlin e Compose nas versões estáveis mais recentes (BOM).
- App de música do estado vazio: Spotify se instalado; senão o último app que tocou mídia.
- Acesso a notificações (necessário para ler a mídia): na primeira execução, o card de música mostra "Permitir acesso à música". O toque abre a tela do sistema.
- Mapa sem internet: o cache automático do MapLibre mostra as áreas já vistas. Download offline fica fora da fase 1.

## 16. Deixado para quem implementar

- Implementação do `SquircleShape`.
- Ajuste fino das cores do estilo de mapa noturno.
- Organização interna de cada `feature/` (arquivos e componentes).
- Estratégia de cache dos ícones de apps.
