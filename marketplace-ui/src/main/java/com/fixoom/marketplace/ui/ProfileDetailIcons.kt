package com.fixoom.marketplace.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Original monoline symbols share a 24-unit grid and rounded 1.6-unit strokes. */
internal object ProfileDetailIcons {
    val Contact =
        symbol("ProfileContact") {
            moveTo(7f, 4f)
            lineTo(17f, 4f)
            curveTo(19f, 4f, 20f, 5f, 20f, 7f)
            lineTo(20f, 17f)
            curveTo(20f, 19f, 19f, 20f, 17f, 20f)
            lineTo(7f, 20f)
            curveTo(5f, 20f, 4f, 19f, 4f, 17f)
            lineTo(4f, 7f)
            curveTo(4f, 5f, 5f, 4f, 7f, 4f)
            close()
            moveTo(11f, 9f)
            curveTo(11f, 11.5f, 7f, 11.5f, 7f, 9f)
            curveTo(7f, 6.5f, 11f, 6.5f, 11f, 9f)
            close()
            moveTo(6.5f, 16f)
            curveTo(6.5f, 12.8f, 11.5f, 12.8f, 11.5f, 16f)
            moveTo(14.5f, 9f)
            lineTo(17f, 9f)
            moveTo(14.5f, 12f)
            lineTo(17f, 12f)
            moveTo(14.5f, 15f)
            lineTo(16f, 15f)
        }
    val Services =
        symbol("ProfileServices") {
            moveTo(8f, 7f)
            lineTo(8f, 5.5f)
            curveTo(8f, 4.4f, 8.8f, 3.5f, 10f, 3.5f)
            lineTo(14f, 3.5f)
            curveTo(15.2f, 3.5f, 16f, 4.4f, 16f, 5.5f)
            lineTo(16f, 7f)
            moveTo(6f, 7f)
            lineTo(18f, 7f)
            curveTo(20f, 7f, 21f, 8f, 21f, 10f)
            lineTo(21f, 17f)
            curveTo(21f, 19f, 20f, 20f, 18f, 20f)
            lineTo(6f, 20f)
            curveTo(4f, 20f, 3f, 19f, 3f, 17f)
            lineTo(3f, 10f)
            curveTo(3f, 8f, 4f, 7f, 6f, 7f)
            close()
            moveTo(3f, 11f)
            curveTo(7f, 14.5f, 17f, 14.5f, 21f, 11f)
            moveTo(10f, 12.5f)
            lineTo(10f, 15.5f)
            lineTo(14f, 15.5f)
            lineTo(14f, 12.5f)
        }
    val Introduction =
        symbol("ProfileIntroduction") {
            moveTo(7f, 3f)
            lineTo(14f, 3f)
            lineTo(19f, 8f)
            lineTo(19f, 18f)
            curveTo(19f, 20f, 18f, 21f, 16f, 21f)
            lineTo(7f, 21f)
            curveTo(5.5f, 21f, 5f, 20f, 5f, 18f)
            lineTo(5f, 6f)
            curveTo(5f, 4f, 5.5f, 3f, 7f, 3f)
            close()
            moveTo(14f, 3f)
            lineTo(14f, 8f)
            lineTo(19f, 8f)
            moveTo(8.5f, 12f)
            lineTo(15.5f, 12f)
            moveTo(8.5f, 15f)
            lineTo(15.5f, 15f)
            moveTo(8.5f, 18f)
            lineTo(12f, 18f)
        }

    val Person =
        symbol("ProfileName") {
            moveTo(15.5f, 7.5f)
            curveTo(15.5f, 12.1f, 8.5f, 12.1f, 8.5f, 7.5f)
            curveTo(8.5f, 2.9f, 15.5f, 2.9f, 15.5f, 7.5f)
            close()
            moveTo(5f, 20f)
            lineTo(5f, 18.5f)
            curveTo(5f, 12f, 19f, 12f, 19f, 18.5f)
            lineTo(19f, 20f)
        }
    val Email =
        symbol("ProfileEmail") {
            moveTo(6f, 5f)
            lineTo(18f, 5f)
            curveTo(20f, 5f, 21f, 6f, 21f, 8f)
            lineTo(21f, 16f)
            curveTo(21f, 18f, 20f, 19f, 18f, 19f)
            lineTo(6f, 19f)
            curveTo(4f, 19f, 3f, 18f, 3f, 16f)
            lineTo(3f, 8f)
            curveTo(3f, 6f, 4f, 5f, 6f, 5f)
            close()
            moveTo(3.5f, 7f)
            lineTo(10.5f, 12.5f)
            curveTo(11.5f, 13.3f, 12.5f, 13.3f, 13.5f, 12.5f)
            lineTo(20.5f, 7f)
        }
    val Phone =
        symbol("ProfilePhone") {
            moveTo(8f, 3.5f)
            lineTo(16f, 3.5f)
            curveTo(17.5f, 3.5f, 18f, 4.5f, 18f, 6f)
            lineTo(18f, 18f)
            curveTo(18f, 19.5f, 17.5f, 20.5f, 16f, 20.5f)
            lineTo(8f, 20.5f)
            curveTo(6.5f, 20.5f, 6f, 19.5f, 6f, 18f)
            lineTo(6f, 6f)
            curveTo(6f, 4.5f, 6.5f, 3.5f, 8f, 3.5f)
            close()
            moveTo(10f, 6.5f)
            lineTo(14f, 6.5f)
            moveTo(11f, 17.5f)
            lineTo(13f, 17.5f)
        }
    val Category =
        symbol("ProfileCategory") {
            moveTo(4f, 4f)
            lineTo(10f, 4f)
            lineTo(10f, 10f)
            lineTo(4f, 10f)
            close()
            moveTo(14f, 4f)
            lineTo(20f, 4f)
            lineTo(20f, 10f)
            lineTo(14f, 10f)
            close()
            moveTo(4f, 14f)
            lineTo(10f, 14f)
            lineTo(10f, 20f)
            lineTo(4f, 20f)
            close()
            moveTo(14f, 14f)
            lineTo(20f, 14f)
            lineTo(20f, 20f)
            lineTo(14f, 20f)
            close()
        }
    val Location =
        symbol("ProfileLocation") {
            moveTo(12f, 21f)
            curveTo(10f, 18f, 5f, 14.5f, 5f, 9f)
            curveTo(5f, 0.8f, 19f, 0.8f, 19f, 9f)
            curveTo(19f, 14.5f, 14f, 18f, 12f, 21f)
            close()
            moveTo(14.5f, 9f)
            curveTo(14.5f, 12.3f, 9.5f, 12.3f, 9.5f, 9f)
            curveTo(9.5f, 5.7f, 14.5f, 5.7f, 14.5f, 9f)
            close()
        }
    val Availability =
        symbol("ProfileAvailability") {
            moveTo(20f, 12f)
            curveTo(20f, 22.7f, 4f, 22.7f, 4f, 12f)
            curveTo(4f, 1.3f, 20f, 1.3f, 20f, 12f)
            close()
            moveTo(12f, 7f)
            lineTo(12f, 12f)
            lineTo(15f, 14f)
        }

    private fun symbol(name: String, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = draw,
                )
            }
            .build()
}
