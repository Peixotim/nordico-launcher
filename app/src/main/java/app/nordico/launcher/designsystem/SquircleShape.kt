package app.nordico.launcher.designsystem

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Cantos contínuos (estilo Apple). A curva começa a 1,28 × raio do canto e os pontos de
 * controle ficam a 27,7% desse trecho: mesma "profundidade" de um canto circular de mesmo
 * raio, mas com entrada suave, sem quebra de curvatura.
 */
class SquircleShape(private val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val a = minOf(with(density) { radius.toPx() } * 1.28f, w / 2f, h / 2f)
        val k = a * 0.277f
        val path = Path().apply {
            moveTo(a, 0f)
            lineTo(w - a, 0f)
            cubicTo(w - k, 0f, w, k, w, a)
            lineTo(w, h - a)
            cubicTo(w, h - k, w - k, h, w - a, h)
            lineTo(a, h)
            cubicTo(k, h, 0f, h - k, 0f, h - a)
            lineTo(0f, a)
            cubicTo(0f, k, k, 0f, a, 0f)
            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is SquircleShape && other.radius == radius

    override fun hashCode() = radius.hashCode()
}
