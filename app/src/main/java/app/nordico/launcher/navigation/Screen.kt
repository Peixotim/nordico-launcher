package app.nordico.launcher.navigation

import app.nordico.launcher.data.vehicle.ClimateState

enum class Screen { Home, NowPlaying, Apps, Climate }

/** Se os dados do ar somem com o Clima aberto, volta para o Início. */
fun nextScreenOnClimate(current: Screen, climate: ClimateState?): Screen =
    if (current == Screen.Climate && climate == null) Screen.Home else current
