# Launcher Nórdico (fase 1): plano de implementação

**Goal:** Um app Android (Kotlin + Jetpack Compose) instalável como tela inicial (HOME), com Início, Tocando agora, Clima, Apps e avisos fiéis aos protótipos. Roda no emulador 1280×720 com dados simulados (`sim`) e com APIs reais do Android (`device`).

**Architecture:** Um módulo `:app` com pastas por funcionalidade (`designsystem/`, `feature/*`, `data/*`). Cada fonte de dados é uma interface. As implementações simuladas ficam em `src/sim`, as reais em `src/device`, e um `AppContainer` por flavor faz a ligação. A UI desenha numa tela lógica de 1280×720 dp, forçada via `LocalDensity`. A navegação é um estado simples com `SharedTransitionLayout`.

**Tech Stack:** Kotlin 2.4.21, AGP 9.5.1, Gradle 9.9.1, Compose BOM 2026.09.00 (só `foundation`/`ui`/`animation`, **sem Material3**), lifecycle 2.11.0, activity-compose 1.13.0, DataStore 1.3.0, MapLibre Android 13.7.1, JUnit 4.13.2, kotlinx-coroutines-test 1.11.0. JDK 21 (já instalado).

**Spec:** `docs/specs/2026-11-15-launcher-fase1-design.md`. Visual: `docs/prototipos/03-inicio-painelA-clima-apps.html` (vale em conflito).

## Global Constraints

- Pacote/applicationId `app.nordico.launcher` (flavor `sim` adiciona sufixo `.sim`). Nome do app: "Nórdico".
- `minSdk = 34`, `targetSdk = 34`, `compileSdk = 37` (plataforma instalada).
- Tela lógica **1280×720 dp**: `density = larguraPx / 1280f`, `fontScale = 1f`. Medidas do protótipo em px = dp.
- Fontes: Inter (texto) e Inter Display (números ≥ 32). Números com `fontFeatureSettings = "tnum"`.
- Ícones: Material Symbols Rounded (fonte variável), peso 400, `FILL` 0 ou 1.
- Cores exatamente como na spec 9.1. Destaque padrão laranja `#F2620F` (dia) / `#FF7A2E` (noite).
- Mola padrão: `spring(dampingRatio = 0.82f, stiffness = 158f)`. Toque: escala 0.94 em 120 ms ease-out, soltar com a mola.
- Volume da UI: 0–40. Temperatura do ar: 18,0–32,0, passo 0,5, exibida com vírgula.
- Auto sem farol: Noite das 18h (inclusive) às 6h (exclusive).
- Textos da UI em pt-BR, exatamente como nos protótipos.
- Sem desfoque de fundo na fase 1. Sem Material3, sem Hilt, sem biblioteca de navegação.
- Respeitar "remover animações" do sistema (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`: sem animação).

## Review Focus

- **Sem sessão de mídia, ou metadados sem título/capa:** o card mostra "Nada tocando", ou cai para o nome da origem, e não quebra (teste na Task 8).
- **Volume do sistema com máximo diferente de 40 (15, 30):** 0↔0 e máx↔40 exatos, e cada passo da UI muda o volume real (teste na Task 3).
- **App oculto que foi desinstalado:** não aparece em "Ocultos" e não quebra a gaveta (teste na Task 10).
- **Volante repetindo volume rápido:** o aviso continua visível e o tempo reinicia a cada evento, sem piscar (teste na Task 5).
- **Dados do ar somem com o Clima aberto:** volta para o Início e o chip some (teste na Task 5).

---

## Estrutura de arquivos

```
.gitignore
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/…                       (gerado)
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/font/                 inter_*.ttf, interdisplay_*.ttf, material_symbols_rounded.ttf
app/src/main/res/values/strings.xml
app/src/main/java/app/nordico/launcher/
  App.kt                               Application; cria AppContainer
  MainActivity.kt                      HOME, LocalDensity, imersivo, permissões
  LogicalDensity.kt                    logicalDensity()
  navigation/Screen.kt                 Screen, nextScreenOnClimate()
  navigation/AppViewModel.kt           estado global (tela, modo, HUD)
  navigation/AppShell.kt               SharedTransitionLayout + telas + HUDs + tela desligada
  designsystem/Tokens.kt               NordicColors, nordicColors(), LocalNordic
  designsystem/ThemeMode.kt            resolveDark()
  designsystem/Type.kt                 Inter, InterDisplay, estilos
  designsystem/Motion.kt               nordicSpring(), Modifier.pressScale()
  designsystem/Sym.kt                  ícone por ligadura
  designsystem/SquircleShape.kt
  designsystem/NordicTheme.kt
  designsystem/components/             NordicCard, SegmentedControl, SegmentMeter, Hud, PageHeader
  feature/home/                        HomeScreen, Header, MiniPlayerCard, Tiles, VolumePanel, DateFormat
  feature/map/                         MapPane, NavLauncher
  feature/nowplaying/                  NowPlayingScreen, MiniCardState, TimeFormat
  feature/climate/                     ClimateScreen, ClimateFormat
  feature/apps/                        AppsScreen, AppsLogic, AppsViewModel, AppIcon
  feature/hud/                         HudController, VolumeHud, ClimateHud
  data/media/MediaRepository.kt
  data/apps/AppsRepository.kt  data/apps/VendorApps.kt
  data/audio/VolumeRepository.kt  data/audio/VolumeMath.kt
  data/vehicle/VehicleRepository.kt  data/vehicle/ClimatePanel.kt
  data/status/StatusRepository.kt
  data/location/LocationRepository.kt
  data/prefs/PrefsRepository.kt
app/src/sim/java/app/nordico/launcher/
  AppContainer.kt  sim/Sim*.kt  sim/SimPanel.kt
app/src/sim/res/drawable-nodpi/sim_art.jpg
app/src/device/java/app/nordico/launcher/
  AppContainer.kt  device/Device*.kt  device/MediaListenerService.kt
app/src/test/java/app/nordico/launcher/…   testes JVM
```

Comandos: rodar no Git Bash a partir da raiz `new-interface/`. Testes: `./gradlew testSimDebugUnitTest`.

---

### Task 1: Ambiente, emulador e esqueleto que abre como tela inicial

**Files:**
- Create: `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/app/nordico/launcher/{App.kt,MainActivity.kt,LogicalDensity.kt}`
- Test: `app/src/test/java/app/nordico/launcher/LogicalDensityTest.kt`

**Interfaces:**
- Produces: `fun logicalDensity(widthPx: Int): Float` (`= widthPx / 1280f`); `const val LOGICAL_WIDTH_DP = 1280`; `class MainActivity` (HOME); `class App : Application` com `lateinit var container: AppContainer` (o `AppContainer` vem na Task 4; até lá a linha fica fora).

- [ ] **Step 1: Ferramentas do SDK e imagem Android 14**

  As ferramentas de linha de comando não estão instaladas. Baixar `https://dl.google.com/android/repository/commandlinetools-win-9862592_latest.zip` e extrair em `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\` (pasta `bin` dentro de `latest`). Depois:
  ```bash
  S="$LOCALAPPDATA/Android/Sdk"; SM="$S/cmdline-tools/latest/bin/sdkmanager.bat"
  yes | "$SM" --licenses
  "$SM" "cmdline-tools;latest" "system-images;android-34;google_apis;x86_64"
  ```
  Esperado: `"$S/system-images/android-34/google_apis/x86_64"` existe.

- [ ] **Step 2: Criar o AVD "Central9" (imita a central)**
  ```bash
  "$S/cmdline-tools/latest/bin/avdmanager.bat" create avd -n Central9 -k "system-images;android-34;google_apis;x86_64" -d "pixel_tablet" --force
  ```
  Em `~/.android/avd/Central9.avd/config.ini`, definir: `hw.lcd.width=1280`, `hw.lcd.height=720`, `hw.lcd.density=160`, `hw.initialOrientation=landscape`, `showDeviceFrame=no`, `hw.keyboard=yes`, `skin.name=1280x720`, `skin.path=_no_skin`.
  Subir: `"$S/emulator/emulator" -avd Central9 &` e verificar `adb shell wm size` → `Physical size: 1280x720`, `adb shell wm density` → `160`.

- [ ] **Step 3: git e Gradle Wrapper**
  ```bash
  git init && git checkout -b main
  ```
  `.gitignore`: `.gradle/`, `build/`, `local.properties`, `.idea/`, `*.iml`, `captures/`, `.kotlin/`.
  Wrapper: baixar `https://services.gradle.org/distributions/gradle-9.9.1-bin.zip` no scratchpad, extrair e rodar `<extraído>/bin/gradle wrapper --gradle-version 9.9.1` na raiz. Esperado: `./gradlew --version` mostra `Gradle 9.9.1`.

- [ ] **Step 4: Arquivos Gradle**

  `settings.gradle.kts`: repositórios `google()`, `mavenCentral()` (mais `gradlePluginPortal()` em `pluginManagement`), `rootProject.name = "nordico"`, `include(":app")`.
  `gradle.properties`: `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx4g`, `kotlin.code.style=official`.
  `build.gradle.kts` (raiz): `plugins { alias(libs.plugins.android.application) apply false; alias(libs.plugins.kotlin.compose) apply false }`.
  `gradle/libs.versions.toml`:
  ```toml
  [versions]
  agp = "9.5.1"
  kotlin = "2.4.21"
  composeBom = "2026.09.00"
  activityCompose = "1.13.0"
  lifecycle = "2.11.0"
  datastore = "1.3.0"
  coreKtx = "1.19.1"
  maplibre = "13.7.1"
  coroutines = "1.11.0"
  junit = "4.13.2"

  [libraries]
  androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
  androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
  androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
  androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
  androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
  compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
  compose-ui = { module = "androidx.compose.ui:ui" }
  compose-foundation = { module = "androidx.compose.foundation:foundation" }
  compose-animation = { module = "androidx.compose.animation:animation" }
  compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
  compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
  maplibre = { module = "org.maplibre.gl:android-sdk", version.ref = "maplibre" }
  coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
  junit = { module = "junit:junit", version.ref = "junit" }

  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
  ```
  `app/build.gradle.kts`: plugins `android.application` e `kotlin.compose` (o AGP 9 já traz Kotlin embutido; se o build pedir o plugin Kotlin, aplicar `org.jetbrains.kotlin.android` na versão `kotlin`). `namespace`/`applicationId` = `app.nordico.launcher`, `compileSdk = 37`, `minSdk = 34`, `targetSdk = 34`, `versionCode = 1`, `versionName = "0.1"`. `flavorDimensions += "target"` com flavors `sim` (`applicationIdSuffix = ".sim"`) e `device`. `buildFeatures.compose = true`, Java 17. Dependências: todas as `libraries` acima (`compose-bom` via `platform(...)`, `ui-tooling` como `debugImplementation`, `junit` e `coroutines-test` como `testImplementation`).

- [ ] **Step 5: Teste que falha**
  ```kotlin
  class LogicalDensityTest {
      @Test fun `1280 px wide gives density 1`() = assertEquals(1f, logicalDensity(1280), 0.0001f)
      @Test fun `1920 px wide gives density 1_5`() = assertEquals(1.5f, logicalDensity(1920), 0.0001f)
  }
  ```
  Run: `./gradlew testSimDebugUnitTest --tests '*LogicalDensityTest'`. Esperado: FALHA (`logicalDensity` não existe).

- [ ] **Step 6: Implementar `logicalDensity`, `App`, `MainActivity`, manifesto**
  - `MainActivity : ComponentActivity`. Em `setContent`, envolver o conteúdo em `CompositionLocalProvider(LocalDensity provides Density(logicalDensity(resources.displayMetrics.widthPixels), fontScale = 1f))`. Por enquanto o conteúdo é um `Box` preto em tela cheia.
  - Modo imersivo: `WindowCompat.getInsetsController(window, window.decorView).apply { hide(WindowInsetsCompat.Type.systemBars()); systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }`.
  - `onBackPressedDispatcher` sem ação (o Início ignora voltar; a Task 5 conecta voltar → Início).
  - Manifesto: `android:name=".App"`, `label="@string/app_name"` ("Nórdico"). Activity `exported="true"`, `launchMode="singleTask"`, `screenOrientation="landscape"`, `configChanges="orientation|screenSize|screenLayout|density|uiMode|keyboardHidden"`, intent-filter `MAIN` + `HOME` + `DEFAULT` + `LAUNCHER`.

- [ ] **Step 7: Testes passam e o app abre como HOME**
  ```bash
  ./gradlew testSimDebugUnitTest --tests '*LogicalDensityTest'     # PASS
  ./gradlew installSimDebug
  adb shell cmd package set-home-activity app.nordico.launcher.sim/app.nordico.launcher.MainActivity
  adb shell input keyevent KEYCODE_HOME
  adb shell dumpsys activity activities | grep -m1 mResumedActivity   # contém app.nordico.launcher.sim
  ```

- [ ] **Step 8: Commit**
  ```bash
  git add -A && git commit -m "chore: esqueleto Android, AVD Central9 e app como HOME"
  ```

---

### Task 2: Design system (tema Dia/Noite, fontes, ícones, mola, squircle)

**Files:**
- Create: `designsystem/{Tokens.kt,ThemeMode.kt,Type.kt,Motion.kt,Sym.kt,SquircleShape.kt,NordicTheme.kt}`, `data/prefs/PrefsRepository.kt` (só os enums nesta task), `res/font/*`
- Test: `app/src/test/java/app/nordico/launcher/designsystem/ThemeModeTest.kt`, `TokensTest.kt`

**Interfaces:**
- Produces:
  - `enum class ScreenMode { Auto, Day, Night }`, `enum class Accent { Orange, Blue, Green, Graphite }` (em `data/prefs/PrefsRepository.kt`).
  - `fun resolveDark(mode: ScreenMode, headlightsOn: Boolean?, hour: Int): Boolean`.
  - `data class NordicColors(val dark: Boolean, val bg: Color, val surface: Color, val label: Color, val label2: Color, val label3: Color, val separator: Color, val fill3: Color, val fill2: Color, val material: Color, val segmentOn: Color, val accent: Color)`; `fun nordicColors(dark: Boolean, accent: Accent): NordicColors`; `val LocalNordic: ProvidableCompositionLocal<NordicColors>`.
  - `val Inter: FontFamily`, `val InterDisplay: FontFamily`, `val Tnum = "tnum"`.
  - `fun <T> nordicSpring(): SpringSpec<T>`; `fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier`.
  - `@Composable fun Sym(name: String, modifier: Modifier = Modifier, size: Dp = 24.dp, filled: Boolean = false, tint: Color = LocalNordic.current.label)`.
  - `class SquircleShape(private val radius: Dp) : Shape`.
  - `@Composable fun NordicTheme(dark: Boolean, accent: Accent, content: @Composable () -> Unit)`: anima cada cor por 450 ms (`animateColorAsState(tween(450))`) e provê `LocalNordic` e `LocalContentColor`-equivalente (texto padrão `label`, `Inter`).

- [ ] **Step 1: Testes que falham**
  ```kotlin
  class ThemeModeTest {
      @Test fun day_and_night_are_fixed() { assertFalse(resolveDark(ScreenMode.Day, true, 23)); assertTrue(resolveDark(ScreenMode.Night, false, 12)) }
      @Test fun auto_follows_headlights_when_known() { assertTrue(resolveDark(ScreenMode.Auto, true, 12)); assertFalse(resolveDark(ScreenMode.Auto, false, 23)) }
      @Test fun auto_without_headlights_uses_18_to_6() {
          assertFalse(resolveDark(ScreenMode.Auto, null, 17)); assertTrue(resolveDark(ScreenMode.Auto, null, 18))
          assertTrue(resolveDark(ScreenMode.Auto, null, 5));  assertFalse(resolveDark(ScreenMode.Auto, null, 6))
      }
  }
  class TokensTest {
      @Test fun orange_accent_per_mode() {
          assertEquals(Color(0xFFF2620F), nordicColors(false, Accent.Orange).accent)
          assertEquals(Color(0xFFFF7A2E), nordicColors(true, Accent.Orange).accent)
      }
      @Test fun backgrounds() { assertEquals(Color(0xFFF2F2F7), nordicColors(false, Accent.Blue).bg); assertEquals(Color.Black, nordicColors(true, Accent.Blue).bg) }
  }
  ```
  Run: `./gradlew testSimDebugUnitTest --tests '*designsystem*'`. Esperado: FALHA (símbolos ausentes).

- [ ] **Step 2: Fontes**

  Baixar `https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip`. De `extras/ttf/`, copiar para `res/font/` com nomes minúsculos:
  - `Inter-{Light,Regular,Medium,SemiBold,Bold}.ttf` → `inter_light.ttf`… `inter_bold.ttf`;
  - `InterDisplay-{ExtraLight,Light,Regular,SemiBold,Bold}.ttf` → `interdisplay_extralight.ttf`….

  Baixar `https://github.com/google/material-design-icons/raw/master/variablefont/MaterialSymbolsRounded%5BFILL,GRAD,opsz,wght%5D.ttf` → `res/font/material_symbols_rounded.ttf`.

- [ ] **Step 3: Implementar**
  - `Tokens.kt`: valores exatos da spec 9.1. Alfas como `Color(r, g, b, alpha)`. Destaque:

    | Accent | Dia | Noite |
    |---|---|---|
    | Orange | `#F2620F` | `#FF7A2E` |
    | Blue | `#007AFF` | `#0A84FF` |
    | Green | `#248A3D` | `#30D158` |
    | Graphite | `#3A3A3C` | `#AEAEB2` |

  - `Sym`: `BasicText(name)` com `FontFamily(Font(R.font.material_symbols_rounded, variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", if (filled) 1f else 0f), FontVariation.weight(400), FontVariation.Setting("opsz", 24f))))` e `fontSize = size.value.sp` (densidade lógica: 1 sp = 1 dp). Adicionar `semantics { invisibleToUser() }`; quem precisar de rótulo põe no botão.
  - `SquircleShape`: contorno de superelipse com cantos contínuos. Path com curvas cúbicas, controle a ~1,28 × raio. Limitar o raio a `min(w, h) / 2`.
  - `pressScale`: `collectIsPressedAsState()`. Quando pressionado, `animateFloatAsState(0.94f, tween(120, easing = FastOutSlowInEasing))`; ao soltar, volta a `1f` com `nordicSpring()`. Aplicar via `graphicsLayer { scaleX = s; scaleY = s }`.

- [ ] **Step 4: Testes passam**
  Run: `./gradlew testSimDebugUnitTest --tests '*designsystem*'`. Esperado: PASS.

- [ ] **Step 5: Commit**
  ```bash
  git add -A && git commit -m "feat: design system Nórdico (cores, Inter, ícones, mola, squircle)"
  ```

---

### Task 3: Regras puras (volume, clima, formatação)

**Files:**
- Create: `data/audio/VolumeMath.kt`, `data/vehicle/ClimatePanel.kt` (com `ClimateState`/`Airflow`), `feature/climate/ClimateFormat.kt`, `feature/home/DateFormat.kt`, `feature/nowplaying/TimeFormat.kt`
- Test: `VolumeMathTest.kt`, `ClimatePanelTest.kt`, `ClimateFormatTest.kt`, `DateFormatTest.kt`, `TimeFormatTest.kt`

**Interfaces:**
- Produces:
  - `const val VOLUME_MAX = 40`; `enum class VolumeOrigin { Internal, External }`; `data class VolumeState(val level: Int, val muted: Boolean, val origin: VolumeOrigin = VolumeOrigin.Internal)`; `fun VolumeState.stepped(delta: Int): VolumeState` (limita a 0..40; `delta > 0` sai do mudo; origem `Internal`); `fun VolumeState.icon(): String` (`"volume_off"` se mudo ou 0; `"volume_down"` se < 14; senão `"volume_up"`); `fun toUiLevel(sys: Int, sysMax: Int): Int`; `fun toSysLevel(ui: Int, sysMax: Int): Int`.
  - `enum class Airflow(val label: String) { Face("Rosto"), FaceFeet("Rosto e pés"), Feet("Pés"), FeetDefrost("Pés e para-brisa") }`; `data class ClimateState(val setTemp: Double, val fanSpeed: Int, val auto: Boolean, val ac: Boolean, val airflow: Airflow, val recirc: Boolean, val defrostFront: Boolean, val defrostRear: Boolean)`; `val DEFAULT_CLIMATE = ClimateState(22.5, 3, true, true, Airflow.FaceFeet, false, false, false)`; `enum class PanelButton { TempDown, TempUp, FanDown, FanUp, Auto, Ac, Airflow, Recirc, DefrostFront, DefrostRear }`; `fun ClimateState.press(b: PanelButton): ClimateState`.
  - `fun formatTemp(t: Double): String`; `enum class TempTone { Cool, Mid, Warm }`; `fun toneFor(t: Double): TempTone`; `fun fanLabel(c: ClimateState): String`; `fun fanSpinMillis(speed: Int): Int`; `fun chipLabel(c: ClimateState): String`.
  - `fun formatDate(d: LocalDate): String`; `fun formatClock(t: LocalTime): String`; `fun formatElapsed(ms: Long): String`.

- [ ] **Step 1: Testes que falham**
  ```kotlin
  class VolumeMathTest {
      @Test fun step_clamps_and_unmutes() {
          assertEquals(40, VolumeState(39, false).stepped(5).level)
          assertEquals(0, VolumeState(1, false).stepped(-3).level)
          assertFalse(VolumeState(10, true).stepped(1).muted)
          assertTrue(VolumeState(10, true).stepped(-1).muted)
      }
      @Test fun icons() { assertEquals("volume_off", VolumeState(0, false).icon()); assertEquals("volume_down", VolumeState(13, false).icon()); assertEquals("volume_up", VolumeState(14, false).icon()); assertEquals("volume_off", VolumeState(30, true).icon()) }
      @Test fun ends_map_exactly_for_any_system_max() {
          for (max in listOf(15, 30, 40)) {
              assertEquals(0, toUiLevel(0, max)); assertEquals(40, toUiLevel(max, max))
              assertEquals(0, toSysLevel(0, max)); assertEquals(max, toSysLevel(40, max))
          }
      }
      @Test fun every_ui_step_up_never_lowers_system_and_round_trips() {
          for (max in listOf(15, 30)) for (ui in 0 until 40) {
              assertTrue(toSysLevel(ui + 1, max) >= toSysLevel(ui, max))
              val s = toSysLevel(ui, max); assertEquals(s, toSysLevel(toUiLevel(s, max), max))
          }
      }
  }
  class ClimatePanelTest {
      @Test fun temp_steps_half_degree_within_18_32() {
          assertEquals(23.0, DEFAULT_CLIMATE.press(PanelButton.TempUp).setTemp, 0.0)
          assertEquals(18.0, DEFAULT_CLIMATE.copy(setTemp = 18.0).press(PanelButton.TempDown).setTemp, 0.0)
          assertEquals(32.0, DEFAULT_CLIMATE.copy(setTemp = 32.0).press(PanelButton.TempUp).setTemp, 0.0)
      }
      @Test fun manual_fan_or_airflow_leaves_auto() {
          val f = DEFAULT_CLIMATE.press(PanelButton.FanUp); assertEquals(4, f.fanSpeed); assertFalse(f.auto)
          val a = DEFAULT_CLIMATE.press(PanelButton.Airflow); assertEquals(Airflow.Feet, a.airflow); assertFalse(a.auto)
          assertEquals(7, DEFAULT_CLIMATE.copy(fanSpeed = 7).press(PanelButton.FanUp).fanSpeed)
          assertEquals(1, DEFAULT_CLIMATE.copy(fanSpeed = 1).press(PanelButton.FanDown).fanSpeed)
      }
      @Test fun auto_restores_fan_airflow_and_ac() {
          val c = DEFAULT_CLIMATE.copy(auto = false, fanSpeed = 6, airflow = Airflow.Face, ac = false).press(PanelButton.Auto)
          assertEquals(DEFAULT_CLIMATE.copy(setTemp = c.setTemp), c)
      }
      @Test fun airflow_cycles() { assertEquals(Airflow.Face, DEFAULT_CLIMATE.copy(airflow = Airflow.FeetDefrost).press(PanelButton.Airflow).airflow) }
  }
  class ClimateFormatTest {
      @Test fun temp_uses_comma() { assertEquals("22,5", formatTemp(22.5)); assertEquals("18,0", formatTemp(18.0)) }
      @Test fun tones() { assertEquals(TempTone.Cool, toneFor(20.5)); assertEquals(TempTone.Mid, toneFor(21.0)); assertEquals(TempTone.Mid, toneFor(25.0)); assertEquals(TempTone.Warm, toneFor(25.5)) }
      @Test fun labels() {
          assertEquals("Automática · 3 de 7", fanLabel(DEFAULT_CLIMATE)); assertEquals("Manual · 5 de 7", fanLabel(DEFAULT_CLIMATE.copy(auto = false, fanSpeed = 5)))
          assertEquals("22,5° · Auto", chipLabel(DEFAULT_CLIMATE)); assertEquals("22,5° · Manual", chipLabel(DEFAULT_CLIMATE.copy(auto = false)))
      }
      @Test fun spin() { assertEquals(1330, fanSpinMillis(1)); assertEquals(310, fanSpinMillis(7)) }
  }
  class DateFormatTest {
      @Test fun ptBr() { assertEquals("domingo, 15 de novembro", formatDate(LocalDate.of(2026, 11, 15))); assertEquals("09:05", formatClock(LocalTime.of(9, 5))); assertEquals("00:00", formatClock(LocalTime.MIDNIGHT)) }
  }
  class TimeFormatTest {
      @Test fun elapsed() { assertEquals("1:42", formatElapsed(102_000)); assertEquals("0:00", formatElapsed(0)); assertEquals("10:05", formatElapsed(605_999)); assertEquals("0:00", formatElapsed(-5)) }
  }
  ```
  Run: `./gradlew testSimDebugUnitTest`. Esperado: FALHA.

- [ ] **Step 2: Implementar**
  - `toUiLevel`/`toSysLevel`: arredondar com `Math.round` proporcional. `toSysLevel` precisa ser monotônico e inverso exato nos pontos do sistema; se o arredondamento simples falhar no teste de ida e volta, usar `ceil` em `toUiLevel`.
  - `fanSpinMillis(s) = ((1.5 - 0.17 * s) * 1000).roundToInt()`.
  - `formatDate`: `DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))`.

- [ ] **Step 3: Testes passam**
  Run: `./gradlew testSimDebugUnitTest`. Esperado: PASS.

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: regras de volume, clima e formatação pt-BR"
  ```

---

### Task 4: Contratos de dados, implementações simuladas e AppContainer

**Files:**
- Create: `data/media/MediaRepository.kt`, `data/apps/AppsRepository.kt`, `data/apps/VendorApps.kt`, `data/audio/VolumeRepository.kt`, `data/vehicle/VehicleRepository.kt`, `data/status/StatusRepository.kt`, `data/location/LocationRepository.kt`, `data/prefs/PrefsRepository.kt` (completar)
- Create: `src/sim/java/app/nordico/launcher/AppContainer.kt`, `src/sim/java/app/nordico/launcher/sim/{SimMedia.kt,SimApps.kt,SimVolume.kt,SimVehicle.kt,SimStatus.kt,SimLocation.kt}`, `src/sim/res/drawable-nodpi/sim_art.jpg` (baixar `https://picsum.photos/id/1015/400/400`)
- Create: `src/device/java/app/nordico/launcher/AppContainer.kt` (por enquanto com as `Sim*` copiadas em `device/` como provisórias; as Tasks 7, 8, 10 e 11 substituem)
- Modify: `App.kt` (cria `container = AppContainer(this)`)
- Test: `app/src/testSim/java/app/nordico/launcher/sim/SimVolumeTest.kt`, `SimVehicleTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class NowPlaying(val title: String, val artist: String, val art: ImageBitmap?, val positionMs: Long, val durationMs: Long, val playing: Boolean, val source: String)
  interface MediaRepository { val nowPlaying: StateFlow<NowPlaying?>; val hasAccess: StateFlow<Boolean>; fun playPause(); fun next(); fun previous() }

  enum class AppSection { Mine, Car }
  enum class InternalRoute { Climate }
  data class AppEntry(val id: String, val label: String, val section: AppSection, val icon: ImageBitmap? = null, val glyph: String? = null, val brandColor: Color? = null, val route: InternalRoute? = null)
  interface AppsRepository { val apps: StateFlow<List<AppEntry>>; fun launch(app: AppEntry) }
  val VENDOR_APPS: Map<String, Pair<String, String>> // pacote → (glifo, nome pt-BR); vazio até a fase 3

  interface VolumeRepository { val state: StateFlow<VolumeState>; fun step(delta: Int); fun toggleMute() }
  interface VehicleRepository { val climate: StateFlow<ClimateState?>; val headlightsOn: StateFlow<Boolean?>; val outsideTemp: StateFlow<Double?> }
  data class StatusState(val bluetooth: Boolean, val cellular: Boolean, val wifi: Boolean)
  interface StatusRepository { val state: StateFlow<StatusState> }
  data class LatLng(val lat: Double, val lng: Double)
  interface LocationRepository { val location: StateFlow<LatLng?> }

  data class Prefs(val screenMode: ScreenMode = ScreenMode.Auto, val accent: Accent = Accent.Orange, val hiddenApps: Set<String> = emptySet(), val navApp: String? = null)
  class PrefsRepository(context: Context) { val prefs: Flow<Prefs>; suspend fun setScreenMode(m: ScreenMode); suspend fun setHidden(id: String, hidden: Boolean) }

  class AppContainer(context: Context) { val media: MediaRepository; val apps: AppsRepository; val volume: VolumeRepository; val vehicle: VehicleRepository; val status: StatusRepository; val location: LocationRepository; val prefs: PrefsRepository; val devPanel: (@Composable (onClose: () -> Unit) -> Unit)? }
  ```
  - Só no `sim`: `SimVolumeRepository.simulateExternal(delta: Int)` (origem `External`); `SimVehicleRepository.press(b: PanelButton)`, `setHeadlights(on: Boolean?)`, `setClimateAvailable(available: Boolean)`; `SimMediaRepository` toca "Evidências" / "Chitãozinho & Xororó", 278 s, a partir de 102 s, origem "iPhone · Bluetooth", e avança 1 s por segundo enquanto toca.
  - `SimAppsRepository`: a lista dos protótipos. Seus apps: Spotify `#1DB954` `music_note`, Waze `#2BB3E8` `navigation`, Google Maps `#4285F4` `map`, YouTube Music `#E62117` `play_circle`, CarPlay / AA `#3A3A3C` `cast`, iGO `#00897B` `explore`, YouTube `#E62117` `smart_display`, Play Store `#01875F` `shop`. Do carro, com glifo e sem cor: Rádio `radio`, Música `library_music`, Vídeos `movie`, Bluetooth `bluetooth`, Telefone `call`, Equalizador `equalizer`, Câmera 360 `360`, AUX / AV `input`, Voz `mic`, Arquivos `folder`, Galeria `photo_library`, Carro `directions_car`, Ajustes `settings`. `id` = nome em minúsculas sem acento. `launch` não faz nada.

- [ ] **Step 1: Testes que falham** (pasta `src/testSim`)
  ```kotlin
  class SimVolumeTest {
      @Test fun external_change_marks_origin() { val r = SimVolumeRepository(); r.simulateExternal(2); assertEquals(VolumeState(20, false, VolumeOrigin.External), r.state.value) }
      @Test fun internal_step_marks_internal() { val r = SimVolumeRepository(); r.step(-1); assertEquals(VolumeOrigin.Internal, r.state.value.origin); assertEquals(17, r.state.value.level) }
  }
  class SimVehicleTest {
      @Test fun panel_updates_climate() { val v = SimVehicleRepository(); v.press(PanelButton.TempUp); assertEquals(23.0, v.climate.value!!.setTemp, 0.0) }
      @Test fun climate_can_disappear() { val v = SimVehicleRepository(); v.setClimateAvailable(false); assertNull(v.climate.value) }
  }
  ```
  Volume inicial do sim: 18, sem mudo. Run: `./gradlew testSimDebugUnitTest`. Esperado: FALHA.

- [ ] **Step 2: Implementar contratos, `Sim*`, `PrefsRepository` (DataStore `preferencesDataStore("prefs")`) e os dois `AppContainer`.** O `devPanel` do sim aponta para `SimPanel` (Task 11); até lá, `null`.

- [ ] **Step 3: Testes passam e os dois flavors compilam**
  ```bash
  ./gradlew testSimDebugUnitTest assembleSimDebug assembleDeviceDebug   # BUILD SUCCESSFUL
  ```

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: contratos de dados, implementações simuladas e AppContainer"
  ```

---

### Task 5: Navegação, estado global e controlador de avisos

**Files:**
- Create: `navigation/Screen.kt`, `navigation/AppViewModel.kt`, `navigation/AppShell.kt`, `feature/hud/HudController.kt`
- Modify: `MainActivity.kt` (usa `AppShell`; voltar → `Home`)
- Test: `navigation/ScreenTest.kt`, `feature/hud/HudControllerTest.kt`

**Interfaces:**
- Consumes: `AppContainer` (Task 4), `resolveDark`, `NordicTheme` (Task 2).
- Produces:
  - `enum class Screen { Home, NowPlaying, Apps, Climate }`; `fun nextScreenOnClimate(current: Screen, climate: ClimateState?): Screen` (Climate + null → Home; senão `current`).
  - `enum class HudKind { Volume, Climate }`; `class HudController(scope: CoroutineScope) { val visible: StateFlow<HudKind?>; fun show(kind: HudKind) }` (Volume 1800 ms, Climate 2600 ms; um novo `show` reinicia o tempo; outro tipo substitui).
  - `class AppViewModel(c: AppContainer) : ViewModel` com: `screen: StateFlow<Screen>`, `go(s: Screen)`, `dark: StateFlow<Boolean>` (`resolveDark` com prefs + farol + hora, reavaliado a cada minuto), `accent: StateFlow<Accent>`, `screenOff: StateFlow<Boolean>`, `setScreenOff(b: Boolean)`, `hud: HudController`. Regras: volume com `origin == External` → `hud.show(Volume)`; mudança de `climate` (não nula, diferente da anterior, ignorando a primeira emissão) com tela ≠ Climate → `hud.show(Climate)`; `climate` virando null → `nextScreenOnClimate`.
  - `@Composable fun AppShell(vm: AppViewModel, container: AppContainer)`: `NordicTheme` + `SharedTransitionLayout` + `AnimatedContent(screen)` + camadas de HUD + tela desligada (preta, relógio Inter Display Light 96 com alfa .22, "Toque para ligar a tela"; toque religa; fade 500 ms). Expõe aos filhos `LocalSharedScope` e `LocalAnimatedScope` (CompositionLocals) para `sharedElement`/`sharedBounds`.

- [ ] **Step 1: Testes que falham**
  ```kotlin
  class ScreenTest {
      @Test fun climate_null_on_climate_goes_home() { assertEquals(Screen.Home, nextScreenOnClimate(Screen.Climate, null)) }
      @Test fun climate_null_elsewhere_stays() { assertEquals(Screen.Apps, nextScreenOnClimate(Screen.Apps, null)) }
      @Test fun climate_present_stays() { assertEquals(Screen.Climate, nextScreenOnClimate(Screen.Climate, DEFAULT_CLIMATE)) }
  }
  @OptIn(ExperimentalCoroutinesApi::class)
  class HudControllerTest {
      @Test fun volume_hides_after_1800() = runTest {
          val h = HudController(backgroundScope); h.show(HudKind.Volume); runCurrent()
          advanceTimeBy(1799); assertEquals(HudKind.Volume, h.visible.value)
          advanceTimeBy(2); assertNull(h.visible.value)
      }
      @Test fun repeated_events_restart_timer() = runTest {
          val h = HudController(backgroundScope)
          repeat(3) { h.show(HudKind.Volume); advanceTimeBy(1000) }   // t = 3000, último em 2000
          assertEquals(HudKind.Volume, h.visible.value)
          advanceTimeBy(801); assertNull(h.visible.value)              // 2000 + 1800 = 3800
      }
      @Test fun climate_lasts_2600_and_replaces_volume() = runTest {
          val h = HudController(backgroundScope); h.show(HudKind.Volume); h.show(HudKind.Climate); runCurrent()
          assertEquals(HudKind.Climate, h.visible.value); advanceTimeBy(2601); assertNull(h.visible.value)
      }
  }
  ```
  Run: `./gradlew testSimDebugUnitTest --tests '*ScreenTest' --tests '*HudControllerTest'`. Esperado: FALHA.

- [ ] **Step 2: Implementar `Screen`, `HudController` (um `Job` cancelado e recriado a cada `show`), `AppViewModel`, `AppShell` com telas provisórias (texto com o nome da tela) e conectar no `MainActivity`.**

- [ ] **Step 3: Testes passam e o app roda**
  Run: `./gradlew testSimDebugUnitTest installSimDebug`. Esperado: PASS. No emulador, o Início mostra o texto provisório e o fundo `#F2F2F7` (ou preto depois das 18h).

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: navegação com transições, estado global e controlador de avisos"
  ```

---

### Task 6: Início: cabeçalho, card de música, atalhos e painel A

**Files:**
- Create: `designsystem/components/{NordicCard.kt,SegmentedControl.kt,SegmentMeter.kt}`, `feature/home/{HomeScreen.kt,Header.kt,MiniPlayerCard.kt,Tiles.kt,VolumePanel.kt}`
- Modify: `navigation/AppShell.kt` (Home real)

**Interfaces:**
- Consumes: `AppViewModel`, `MediaRepository`, `VolumeRepository`, `VehicleRepository`, `StatusRepository`, `PrefsRepository`, formatadores (Task 3).
- Produces:
  - `@Composable fun NordicCard(modifier: Modifier = Modifier, radius: Dp = 24.dp, content: @Composable BoxScope.() -> Unit)` (fundo `surface`, sombra só no dia, `SquircleShape(radius)`).
  - `@Composable fun SegmentedControl(options: List<Pair<String, String>>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)` (pares glifo + rótulo; indicador `segmentOn` deslizando com `nordicSpring`).
  - `@Composable fun SegmentMeter(count: Int, lit: Int, muted: Boolean, segmentWidth: Dp = 4.dp, height: Dp = 8.dp)` (espaço 2,5 dp; aceso `label` ou `label3` se mudo; apagado `fill2`).
  - `@Composable fun HomeScreen(vm: AppViewModel, c: AppContainer, mapSlot: @Composable () -> Unit)`. O mapa entra na Task 7; até lá, `mapSlot` é um `Box` com `surface` e raio 30.
  - Elementos compartilhados com chaves fixas: `"np-card"`, `"np-art"`, `"sheet-apps"`, `"sheet-clima"`.

- [ ] **Step 1: Montar o layout com as medidas do protótipo 03**
  - **Mapa:** `x 16, y 16, 760×688`, raio 30.
  - **Coluna direita:** `x 798 … 1258`, topo 20, base 16, `Arrangement.SpaceBetween`.
  - **Cabeçalho:**
    - relógio `formatClock` Inter Display Light 88, tracking −0,045 em, tnum;
    - data `formatDate` + `" · 27 °C"` (`outsideTemp`, omitido se nulo), 17, `label2`;
    - chip do ar com altura 36, raio 18, `fill3`, `ac_unit` preenchido em `accent` (`mode_fan` se A/C desligado), `chipLabel`, `chevron_right` `label3`. É `sharedBounds("sheet-clima")` e vai para `Screen.Climate`; oculto se `climate == null`.
    - ícones BT, sinal e Wi-Fi 20 dp, mostrados conforme `StatusState`.
    - toque longo de 700 ms no relógio abre `devPanel`, se não for nulo.
  - **Card de música:** raio 26, padding 18/20/12; capa 80 com raio 14; título 21 SemiBold; artista 16 `label2`; barra 6 dp com raio 3 (fundo `fill3`, preenchimento `label`, largura animada linear de 1 s); controles anterior / tocar-pausar (46) / próxima (36), espaço 48.
  - **Atalhos:** 4 tiles com altura 96, raio 24, espaço 12, ícone 28, texto 14 Medium. Apps é `sharedBounds("sheet-apps")` e vai para `Screen.Apps`. Telefone abre `Intent(Intent.ACTION_DIAL)`. Rádio e Câmera não fazem nada na fase 1, com o comentário `// fase 3: abrir app do fabricante (pacote desconhecido até a central chegar)`.
  - **Painel A:**
    - **Volume:** card com raio 24 e padding 9. Botões de 58 dp com raio 18 e fundo `fill3` (`remove`/`add` 30). No centro: ícone `VolumeState.icon()` 22 em `label2` + número 28 SemiBold, e abaixo `SegmentMeter(40, level)`. Tocar no centro chama `toggleMute`.
    - **Segurar repete:** primeiro passo no toque, repetição após 380 ms, a cada 85 ms (`awaitEachGesture` + `delay`).
    - **Segmentado:** Auto / Dia / Noite (`brightness_auto`, `light_mode`, `dark_mode`), altura 58, raio 20, grava `setScreenMode`.
    - **Botão Tela:** 58×58, `power_settings_new`, rótulo "Tela" 11 SemiBold, chama `setScreenOff(true)`.
  - Todos os toques usam `pressScale`.

- [ ] **Step 2: Comparar com o protótipo**
  Run: `./gradlew installSimDebug`, abrir lado a lado com `docs/prototipos/03-inicio-painelA-clima-apps.html`.
  - Conferir `adb exec-out screencap -p > captures/home-dia.png` em Dia e em Noite, pelo segmentado.
  - Conferir que a cor anima em 450 ms e que o indicador desliza com mola.
  - Conferir que segurar + sobe o número continuamente e que "Tela" escurece e religa.
  - Esperado: diferença visual só no mapa provisório.

- [ ] **Step 3: Commit**
  ```bash
  git add -A && git commit -m "feat: tela Início com cabeçalho, card de música, atalhos e painel A"
  ```

---

### Task 7: Mapa e atalhos de navegação

**Files:**
- Create: `feature/map/MapPane.kt`, `feature/map/NavLauncher.kt`, `src/device/.../device/DeviceLocationRepository.kt`
- Modify: `feature/home/HomeScreen.kt` (`mapSlot` real), `MainActivity.kt` (pede `ACCESS_FINE_LOCATION`), `AndroidManifest.xml` (permissões de localização e internet; `<queries>` para `com.waze` e `com.google.android.apps.maps`), `src/device/.../AppContainer.kt`
- Test: `feature/map/NavLauncherTest.kt`

**Interfaces:**
- Consumes: `LocationRepository`, `Prefs.navApp`, `dark`.
- Produces:
  - `enum class NavApp(val pkg: String, val label: String) { Waze("com.waze", "Waze"), Maps("com.google.android.apps.maps", "Google Maps") }`.
  - `enum class Favorite { Home, Work }`.
  - `fun chooseNavApp(pref: String?, installed: Set<String>): NavApp?`: prefere `pref` se instalado; senão Waze; senão Maps; senão null.
  - `fun favoriteUri(app: NavApp, fav: Favorite): String?`: Waze → `"waze://?favorite=home"` / `"waze://?favorite=work"`; Maps → null.
  - `fun showSuggestions(app: NavApp?): Boolean` (= `app == NavApp.Waze`).
  - `@Composable fun MapPane(dark: Boolean, location: LatLng?, navApp: NavApp?, onOpenNav: () -> Unit, onFavorite: (Favorite) -> Unit, onRecenter: () -> Unit)`.

- [ ] **Step 1: Testes que falham**
  ```kotlin
  class NavLauncherTest {
      @Test fun prefers_saved_then_waze_then_maps() {
          assertEquals(NavApp.Maps, chooseNavApp("com.google.android.apps.maps", setOf("com.waze", "com.google.android.apps.maps")))
          assertEquals(NavApp.Waze, chooseNavApp("com.ausente", setOf("com.waze", "com.google.android.apps.maps")))
          assertEquals(NavApp.Maps, chooseNavApp(null, setOf("com.google.android.apps.maps")))
          assertNull(chooseNavApp(null, emptySet()))
      }
      @Test fun favorites_only_for_waze() {
          assertEquals("waze://?favorite=home", favoriteUri(NavApp.Waze, Favorite.Home)); assertEquals("waze://?favorite=work", favoriteUri(NavApp.Waze, Favorite.Work))
          assertNull(favoriteUri(NavApp.Maps, Favorite.Home)); assertTrue(showSuggestions(NavApp.Waze)); assertFalse(showSuggestions(NavApp.Maps))
      }
  }
  ```
  Run: `./gradlew testSimDebugUnitTest --tests '*NavLauncherTest'`. Esperado: FALHA.

- [ ] **Step 2: Implementar**
  - **Mapa:** um único `MapView` via `AndroidView` (`MapLibre.getInstance(context)` antes), com o ciclo de vida ligado ao `LocalLifecycleOwner`. Estilo `https://tiles.openfreemap.org/styles/positron` (dia) / `.../dark` (noite) via `setStyle` quando `dark` muda. Gestos desligados (`uiSettings.setAllGesturesEnabled(false)`), atribuição no canto superior direito, câmera centrada em `location` no zoom 15.4.
    - `// Trade-off: um MapView com troca de estilo pisca ao trocar Dia/Noite; dois MapViews empilhados se incomodar (custa GPU em dobro)`
  - **Ponto azul:** em Compose, centralizado no mapa: 22 dp, `#0A84FF` com borda branca 3,5 e halo pulsando (escala 0,4 → 1,6 e alfa 1 → 0 em 2,4 s).
  - **Pílula "Para onde?":** 344×54, raio 27, cor `material`, sombra, `search` + "Para onde?" (17, `label2`) + `mic`.
  - **Card de sugestões** (só se `showSuggestions`): 344 de largura, raio 24, linhas de 66 dp:
    - bolinha 38 `accent` com `home`/`work` preenchido branco 21;
    - "Casa" / "Trabalho" 17 SemiBold, com subtítulo "Abrir no Waze" 14 `label2`;
    - `chevron_right` `label3`;
    - separador de 0,5 dp a partir de x 66.
  - **Recentralizar:** 54, `material`, `near_me` preenchido em `accent`.
  - **Ações:**
    - "Para onde?" abre `packageManager.getLaunchIntentForPackage(navApp.pkg)`;
    - favoritos usam `Intent(ACTION_VIEW, Uri.parse(favoriteUri))`;
    - sem app de navegação, a pílula fica visível e o toque não faz nada.
  - **`DeviceLocationRepository`:** `LocationManager.GPS_PROVIDER`, 1 s / 5 m, começa ao receber a permissão.
  - **`SimLocationRepository`:** fixo em `LatLng(-23.5614, -46.6558)`.

- [ ] **Step 3: Testes passam e o mapa aparece**
  Run: `./gradlew testSimDebugUnitTest installSimDebug`. Esperado: PASS. O Início mostra o mapa da Av. Paulista, e ao trocar Dia/Noite o estilo troca.

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: mapa MapLibre com estilos dia/noite e atalhos de navegação"
  ```

---

### Task 8: Tocando agora (tela cheia) e mídia real

**Files:**
- Create: `feature/nowplaying/{NowPlayingScreen.kt,MiniCardState.kt}`, `src/device/.../device/{DeviceMediaRepository.kt,MediaListenerService.kt}`
- Modify: `feature/home/MiniPlayerCard.kt` (usa `MiniCardState`, elementos compartilhados), `navigation/AppShell.kt`, `AndroidManifest.xml` (serviço com `BIND_NOTIFICATION_LISTENER_SERVICE`), `src/device/.../AppContainer.kt`
- Test: `feature/nowplaying/MiniCardStateTest.kt`

**Interfaces:**
- Consumes: `MediaRepository`, `VolumePanel` (Task 6), `formatElapsed`.
- Produces:
  ```kotlin
  sealed interface MiniCardState {
      data object NeedsAccess : MiniCardState           // "Permitir acesso à música"
      data object Empty : MiniCardState                 // "Nada tocando"
      data class Playing(val title: String, val artist: String, val art: ImageBitmap?, val progress: Float, val playing: Boolean, val source: String) : MiniCardState
  }
  fun miniCardState(hasAccess: Boolean, np: NowPlaying?): MiniCardState
  ```
  Regras: sem acesso → `NeedsAccess`; `np == null` → `Empty`; título em branco → `title = source`; `progress = (positionMs / durationMs).coerceIn(0f, 1f)`, ou 0 se a duração ≤ 0.

- [ ] **Step 1: Testes que falham**
  ```kotlin
  class MiniCardStateTest {
      private val np = NowPlaying("Evidências", "Chitãozinho & Xororó", null, 102_000, 278_000, true, "Spotify")
      @Test fun access_first() { assertEquals(MiniCardState.NeedsAccess, miniCardState(false, np)) }
      @Test fun empty_when_nothing() { assertEquals(MiniCardState.Empty, miniCardState(true, null)) }
      @Test fun blank_title_falls_back_to_source() { assertEquals("Spotify", (miniCardState(true, np.copy(title = " ")) as MiniCardState.Playing).title) }
      @Test fun progress_is_clamped_and_safe() {
          assertEquals(0f, (miniCardState(true, np.copy(durationMs = 0)) as MiniCardState.Playing).progress, 0f)
          assertEquals(1f, (miniCardState(true, np.copy(positionMs = 400_000)) as MiniCardState.Playing).progress, 0f)
      }
  }
  ```
  Run: `./gradlew testSimDebugUnitTest --tests '*MiniCardStateTest'`. Esperado: FALHA.

- [ ] **Step 2: Implementar**
  - **Card mini:**
    - `NeedsAccess`: texto "Permitir acesso à música" com `chevron_right`. O toque abre `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
    - `Empty`: "Nada tocando" e um botão que abre o Spotify (`com.spotify.music`) se instalado.
    - `Playing`: o toque fora dos botões vai para `Screen.NowPlaying`. O card é `sharedBounds("np-card")` e a capa é `sharedElement("np-art")`.
  - **Tela cheia:**
    - fundo `#1A1A1A` + capa reduzida a 32 px, esticada para preencher (`ContentScale.Crop`, `FilterQuality.Low`) com `ColorMatrix` de brilho 0,5 e saturação 1,6;
    - fechar 48 em `(28, 26)` com `keyboard_arrow_down` 32 sobre branco a 14%;
    - capa 420 em `(112, 150)`, raio 22, sombra;
    - informações a partir de x 600 até 112 da borda, centralizadas na vertical: origem 15 SemiBold branco a 60%, título 46 Bold, artista 24 branco a 62%, barra de 7 dp (branco a 22% / branco), controles 52/72/52 espaçados;
    - por último, o `VolumePanel` em variante escura (`dark = true` forçado localmente).
  - **`DeviceMediaRepository`:**
    - `MediaSessionManager.getActiveSessions(ComponentName(ctx, MediaListenerService::class.java))` + `addOnActiveSessionsChangedListener`;
    - usa o primeiro controller tocando ou, se nenhum estiver tocando, o primeiro;
    - `MediaController.Callback` para metadados e estado;
    - `source` = nome do app do pacote do controller;
    - capa de `METADATA_KEY_ALBUM_ART` ou `METADATA_KEY_ART` via `.asImageBitmap()`;
    - enquanto toca, avança a posição a cada 1 s a partir de `PlaybackState.position` + `lastPositionUpdateTime`;
    - `hasAccess` checado no `ON_RESUME` com `NotificationManagerCompat.getEnabledListenerPackages`.
  - **`MediaListenerService`:** `NotificationListenerService` vazio.

- [ ] **Step 3: Testes passam e verificação manual**
  - Run: `./gradlew testSimDebugUnitTest installSimDebug`. Esperado: PASS.
  - Tocar no card expande para a tela cheia com a capa voando até 420 (mola). Fechar faz o caminho inverso.
  - No `device`: `./gradlew installDeviceDebug`, conceder acesso, tocar um áudio no Chrome do emulador. O card mostra título e origem "Chrome".

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: Tocando agora em tela cheia e leitura de mídia via MediaSession"
  ```

---

### Task 9: Clima e avisos visuais

**Files:**
- Create: `designsystem/components/{Hud.kt,PageHeader.kt}`, `feature/climate/ClimateScreen.kt`, `feature/hud/{VolumeHud.kt,ClimateHud.kt}`
- Modify: `navigation/AppShell.kt`

**Interfaces:**
- Consumes: `ClimateState`, `formatTemp`, `toneFor`, `fanLabel`, `fanSpinMillis`, `HudController`, `VolumeState`.
- Produces:
  - `@Composable fun PageHeader(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {})`: altura 96, padding 28, voltar 48 em `fill3` com `chevron_left` 30, título 34 Bold, tracking −0,025 em.
  - `@Composable fun Hud(visible: Boolean, height: Dp, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit)`: centralizado no topo a 18 dp, cor `material`, raio = altura/2, sombra. Entra com `nordicSpring` (de −16 dp e escala 0,94 para 0 e 1) e alfa em 220 ms; sai com fade de 220 ms.
  - `@Composable fun ClimateScreen(c: ClimateState, onBack: () -> Unit)`.

- [ ] **Step 1: Tela Clima com as medidas do protótipo 03**
  - **Cabeçalho:** `PageHeader("Clima")` com `info` 18 + "Somente leitura · o ajuste continua nos botões do painel" (14, `label2`) à direita. A raiz é `sharedBounds("sheet-clima")`.
  - **Grade:** a partir de y 96, margens 28, espaço 16. Colunas 560 / 1fr / 1fr, linhas 1fr / auto / auto. Áreas `t f e / t d d / t s s`.
  - **Card temperatura (raio 30, padding 28/32):**
    - rótulo `thermostat` "Temperatura definida";
    - valor Inter Display ExtraLight 176, tracking −0,06 em, tnum, mais "°C" 44 Light `label2`;
    - linha com selo AUTO (só se `auto`; 12 Bold, tracking .06 em, fundo `accent` a 16%, texto `accent`, raio 7) e "A/C ligado" / "A/C desligado";
    - régua de 29 segmentos (altura 26, espaço 4, raio 4), aceso se `18 + i * 0,5 ≤ setTemp`, com cor por `toneFor`: Cool `#0A84FF`, Mid `label`, Warm `#FF9F0A`;
    - legendas "18°", "25°" e "32°".
  - **Ventilação:** `mode_fan` girando (`infiniteRepeatable`, duração `fanSpinMillis`), 7 barras de 24 de largura com alturas 16 + 8·i e raio 6 (acesas até `fanSpeed`), e `fanLabel`.
  - **Externa:** `device_thermostat`, valor 76 Light + "°", "Sensor do carro".
  - **Direção do ar:** 4 pílulas de 52 com raio 16. A ativa usa `accent` a 16% e texto `accent` SemiBold.
  - **Estados:** 5 chips de 86 com raio 18 (`hdr_auto` AUTO, `ac_unit` A/C, `sync` Recircular, `windshield_defrost_front` "Desemb. frontal", `windshield_defrost_rear` "Desemb. traseiro"), com o mesmo destaque de ativo.

- [ ] **Step 2: Avisos**
  - **`VolumeHud`:** altura 60; ícone 24 + `SegmentMeter(20, round(level / 2), segmentWidth = 5.dp)` + número 22 SemiBold.
  - **`ClimateHud`:** altura 72; `ac_unit` em `accent` + "22,5" 32 SemiBold com "°C" 18 `label2` + separador de 0,5 × 32 + `mode_fan` + 7 mini barras (5 de largura, alturas 6 + 2,3·i) + separador + selo AUTO. O toque vai para `Screen.Climate`.
  - Os dois ficam no `AppShell`, acima das telas, e são controlados por `vm.hud.visible`.

- [ ] **Step 3: Verificar no emulador** (painel de simulação ainda não existe; usar `SimVehicleRepository.press` temporariamente via `LaunchedEffect` de depuração e remover antes do commit)
  - Run: `./gradlew installSimDebug`.
  - O chip abre o Clima com a transição a partir do chip.
  - Mudar a temperatura fora do Clima mostra o aviso por 2,6 s.
  - Comparar o Clima com o protótipo em Dia e Noite.

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: tela Clima somente leitura e avisos de volume e ar"
  ```

---

### Task 10: Gaveta de apps

**Files:**
- Create: `feature/apps/{AppsLogic.kt,AppsViewModel.kt,AppsScreen.kt,AppIcon.kt}`, `src/device/.../device/DeviceAppsRepository.kt`
- Modify: `navigation/AppShell.kt`, `AndroidManifest.xml` (`<queries><intent><action MAIN/><category LAUNCHER/></intent></queries>`), `src/device/.../AppContainer.kt`
- Test: `feature/apps/AppsLogicTest.kt`

**Interfaces:**
- Consumes: `AppsRepository`, `PrefsRepository.setHidden`, `VehicleRepository.climate`, `VENDOR_APPS`.
- Produces:
  ```kotlin
  data class AppSections(val mine: List<AppEntry>, val car: List<AppEntry>, val hidden: List<AppEntry>)
  fun normalize(s: String): String                          // sem acento, minúsculo
  fun sectionApps(apps: List<AppEntry>, hidden: Set<String>, query: String): AppSections   // filtra por normalize(label).contains(normalize(query)); mantém a ordem de entrada
  val CLIMATE_APP = AppEntry(id = "internal.climate", label = "Clima", section = AppSection.Car, glyph = "ac_unit", route = InternalRoute.Climate)
  ```
  - `AppsViewModel(c: AppContainer)`: `sections: StateFlow<AppSections>` (`apps` + `CLIMATE_APP` se `climate != null` + ocultos + busca), `query`, `editing`, `setQuery`, `toggleEditing`, `toggleHidden(id)`.

- [ ] **Step 1: Testes que falham**
  ```kotlin
  class AppsLogicTest {
      private val radio = AppEntry("radio", "Rádio", AppSection.Car)
      private val spotify = AppEntry("spotify", "Spotify", AppSection.Mine)
      private val camera = AppEntry("camera", "Câmera 360", AppSection.Car)
      @Test fun splits_by_section_keeping_order() { val s = sectionApps(listOf(spotify, radio, camera), emptySet(), ""); assertEquals(listOf(spotify), s.mine); assertEquals(listOf(radio, camera), s.car); assertTrue(s.hidden.isEmpty()) }
      @Test fun hidden_moves_out_of_its_section() { val s = sectionApps(listOf(spotify, radio), setOf("radio"), ""); assertEquals(listOf(radio), s.hidden); assertTrue(s.car.isEmpty()) }
      @Test fun stale_hidden_ids_are_ignored() { val s = sectionApps(listOf(spotify), setOf("removido.pkg"), ""); assertTrue(s.hidden.isEmpty()); assertEquals(listOf(spotify), s.mine) }
      @Test fun search_ignores_accents_and_case() { assertEquals(listOf(radio), sectionApps(listOf(spotify, radio), emptySet(), "RADIO").car); assertEquals(listOf(camera), sectionApps(listOf(camera), emptySet(), "câm").car) }
      @Test fun search_applies_to_hidden_too() { assertTrue(sectionApps(listOf(radio), setOf("radio"), "spot").hidden.isEmpty()) }
  }
  ```
  Run: `./gradlew testSimDebugUnitTest --tests '*AppsLogicTest'`. Esperado: FALHA.

- [ ] **Step 2: Implementar**
  - **`AppIcon(app, editing, hiddenSection)`:** placa 88 com `SquircleShape(24.dp)`.
    - `brandColor` → fundo da cor com glifo branco 40 preenchido;
    - só `glyph` → fundo `surface` com glifo `label`;
    - `icon` → imagem recortada no squircle.
    - nome 14 Medium, uma linha com reticências, largura máxima 130.
  - **Selo de edição:** 28 em `rgba(120,120,128,.92)`, com `remove` (ou `add` em Ocultos) branco 20, posicionado a 54 dp à esquerda do centro e no topo. Entra com `nordicSpring` em escala.
  - **Tremor:** `rotationZ` alternando −1,4°/+1,4° em 340 ms, com fase defasada por índice (`(i * 37 % 10) / 30f` do ciclo).
  - **`AppsScreen`:**
    - `PageHeader("Apps")` com busca à direita: 340×48, raio 24, `fill3`, `search` + `BasicTextField` placeholder "Buscar" 17;
    - "Editar"/"Concluir" 17 SemiBold em `accent`, largura mínima 92;
    - corpo rolável: títulos "Seus apps", "Do carro" e "Ocultos" (15 SemiBold `label2`, cada um oculto quando a lista está vazia) e uma grade de 8 colunas com espaço vertical de 10 (`LazyVerticalGrid` com `span` de linha cheia para os títulos e `Modifier.animateItem()`);
    - raiz `sharedBounds("sheet-apps")`.
  - **Comportamento:**
    - toque longo de 550 ms num ícone entra em edição;
    - em edição, tocar alterna oculto;
    - fora da edição, um app com `route` vai para a tela da rota e os demais usam `launch`;
    - sair da tela encerra a edição;
    - voltar do Android, com a edição ativa, só encerra a edição.
  - **`DeviceAppsRepository`:**
    - `LauncherApps.getActivityList(null, Process.myUserHandle())`, excluindo o próprio pacote;
    - seção `Car` se `applicationInfo.flags and FLAG_SYSTEM != 0`;
    - `VENDOR_APPS[pkg]` substitui glifo e nome;
    - ícone: se `AdaptiveIconDrawable`, desenhar fundo + frente num bitmap de 88 dp (sem a máscara do sistema); senão `toBitmap(88, 88)`;
    - recarrega em `LauncherApps.Callback` (adição, remoção e mudança de pacote);
    - `launch` usa `startMainActivity`;
    - `id` = nome do pacote.

- [ ] **Step 3: Testes passam e verificação manual**
  - Run: `./gradlew testSimDebugUnitTest installSimDebug`. Esperado: PASS.
  - Apps expande a partir do tile.
  - Editar faz os ícones tremerem. Ocultar "Vídeos" leva o app para Ocultos com animação, e ele continua oculto depois de reiniciar o app.
  - Buscar "camera" encontra "Câmera 360".
  - No `device`: a gaveta lista os apps reais do emulador.

- [ ] **Step 4: Commit**
  ```bash
  git add -A && git commit -m "feat: gaveta de apps com seções, busca, edição e ocultos"
  ```

---

### Task 11: Volume e status reais, painel de simulação e verificação final

**Files:**
- Create: `src/device/.../device/{DeviceVolumeRepository.kt,DeviceStatusRepository.kt}`, `src/sim/.../sim/SimPanel.kt`
- Modify: os dois `AppContainer.kt`, `AndroidManifest.xml` (`ACCESS_NETWORK_STATE`, `BLUETOOTH_CONNECT`)

**Interfaces:**
- Consumes: `toUiLevel`/`toSysLevel`/`stepped` (Task 3), `SimVolumeRepository`/`SimVehicleRepository` (Task 4).
- Produces: `@Composable fun SimPanel(container: AppContainer, onClose: () -> Unit)`. Fica aberto no `AppShell` quando `devPanel != null` e o relógio recebe toque longo.

- [ ] **Step 1: `DeviceVolumeRepository`**
  - `AudioManager.STREAM_MUSIC`, máximo via `getStreamMaxVolume`.
  - `step` aplica `stepped` no estado da UI e grava `setStreamVolume(STREAM_MUSIC, toSysLevel(ui, max), 0)`. Guarda o último valor gravado para distinguir origem.
  - `toggleMute` usa `adjustStreamVolume(..., ADJUST_TOGGLE_MUTE, 0)`.
  - Mudanças por fora são detectadas com `ContentObserver` em `Settings.System.CONTENT_URI`. Quando o valor do sistema difere do último gravado, emite `origin = External`.

- [ ] **Step 2: `DeviceStatusRepository`**
  - `ConnectivityManager.registerDefaultNetworkCallback` com `TRANSPORT_WIFI` / `TRANSPORT_CELLULAR`.
  - Bluetooth = `BluetoothManager.adapter?.isEnabled`, atualizado por `ACTION_STATE_CHANGED`.

- [ ] **Step 3: `SimPanel`**
  - Folha inferior de 280 dp em `surface`, com os botões do protótipo 03 em `FlowRow`.
  - Linha "Painel do ar": Temp −, Temp +, Vent −, Vent +, AUTO, A/C, Direção, Recircular, Desemb. frontal, Desemb. traseiro, que chamam `press`.
  - Linha "Volante": Vol − e Vol +, que chamam `simulateExternal(∓1)`.
  - Linha "Carro": Farol ligado / Farol desligado / Farol sem sinal (`setHeadlights(true/false/null)`) e "Sem dados do ar" (alterna `setClimateAvailable`).
  - Botão "Fechar".
  - Ligar no `devPanel` do `AppContainer` do sim.

- [ ] **Step 4: Verificação final** (marcar cada item)
  ```bash
  ./gradlew testSimDebugUnitTest assembleDeviceDebug installSimDebug
  ```
  - Todos os testes passam e `device` compila.
  - Volante Vol + repetido mantém o aviso aberto, que some 1,8 s depois do último toque. Usar os botões do painel A não mostra aviso.
  - "Farol ligado" com o modo Auto escurece a interface. "Farol sem sinal" volta ao horário.
  - "Sem dados do ar" com o Clima aberto volta ao Início, e o chip e o app Clima somem.
  - **Densidade:** `adb shell wm density 240` e reabrir. O layout fica idêntico ao de 160 dpi (comparar `adb exec-out screencap -p`). Depois, `adb shell wm density reset`.
  - Ativar "Remover animações" nas opções do desenvolvedor: as telas trocam sem animação e nada quebra.
  - **Fluidez:** "Renderização de GPU por perfil" nas opções do desenvolvedor. Abrir e fechar Tocando agora, Apps e Clima sem barras acima da linha verde na maior parte dos quadros.
  - **Device:** `./gradlew installDeviceDebug`. Mudar o volume pelas teclas do emulador mostra o aviso. Os ícones de Wi-Fi e celular refletem o emulador.

- [ ] **Step 5: Commit**
  ```bash
  git add -A && git commit -m "feat: volume e status reais, painel de simulação e verificação final"
  ```
