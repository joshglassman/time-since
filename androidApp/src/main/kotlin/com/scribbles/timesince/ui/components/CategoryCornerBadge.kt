package com.scribbles.timesince.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Parses a `#rrggbb` hex string to a Compose [Color], falling back to gray. */
fun parseHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

/**
 * A category's corner badge: a right-triangle filling the top-right corner
 * (shaped like `◹`) whose right-angle corner is rounded to match the card's
 * radius (like `◝`), with the category [icon] emoji in the foreground.
 *
 * Kept small and standalone so the Glance home-screen widget (Stage 5) can
 * mirror its look.
 */
@Composable
fun CategoryCornerBadge(
    colorHex: String,
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    cornerRadius: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TopRightRoundedTriangle(cornerRadius))
            .background(parseHexColor(colorHex)),
    ) {
        if (icon.isNotEmpty()) {
            Text(
                text = icon,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 5.dp),
            )
        }
    }
}

/** Right triangle in the top-right corner with a rounded right-angle corner. */
private class TopRightRoundedTriangle(private val cornerRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)          // top-left (sharp)
            lineTo(w - r, 0f)       // top edge up to the rounded corner
            quadraticTo(w, 0f, w, r) // round the top-right (right-angle) corner
            lineTo(w, h)            // right edge down to bottom-right (sharp)
            close()                 // hypotenuse back to top-left
        }
        return Outline.Generic(path)
    }
}
