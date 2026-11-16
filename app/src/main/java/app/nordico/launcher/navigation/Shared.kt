@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.nordico.launcher.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import app.nordico.launcher.designsystem.nordicSpring

val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private val SpringBounds = BoundsTransform { _, _ -> nordicSpring() }

/** Chaves das transições compartilhadas (spec 9.6). */
object SharedKeys {
    const val NP_CARD = "np-card"
    const val NP_ART = "np-art"
    const val SHEET_APPS = "sheet-apps"
    const val SHEET_CLIMA = "sheet-clima"
}

/** Container que "cresce" de uma tela para outra (card → tela cheia). */
@Composable
fun Modifier.sharedSheet(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val animated = LocalAnimatedScope.current ?: return this
    return with(shared) {
        this@sharedSheet.sharedBounds(rememberSharedContentState(key), animated, boundsTransform = SpringBounds)
    }
}

/** Elemento idêntico nas duas telas (ex.: capa do álbum). */
@Composable
fun Modifier.sharedItem(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val animated = LocalAnimatedScope.current ?: return this
    return with(shared) {
        this@sharedItem.sharedElement(rememberSharedContentState(key), animated, boundsTransform = SpringBounds)
    }
}
