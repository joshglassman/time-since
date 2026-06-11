package com.scribbles.timesince.ui.tasklist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Heat-gradient progress pill for a task's "time since" fraction.
 *
 * The gradient (translated from `gradient_bar.svg`) always spans the full width, so the leading
 * (fill) edge color reflects urgency — green when little time has elapsed, red as the deadline
 * nears. The drawn fill grows from 0 width (just completed) to the full width at the deadline.
 */
@Composable
fun TimeSinceBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)
        val pill = Path().apply {
            addRoundRect(RoundRect(0f, 0f, w, h, radius))
        }

        clipPath(pill) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = GradientStops,
                    startX = 0f,
                    endX = w,
                ),
                size = Size(w * clamped, h),
            )
        }

        val strokePx = 1.dp.toPx()
        val inset = strokePx / 2f
        drawRoundRect(
            color = Color.Black,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = Size(w - strokePx, h - strokePx),
            cornerRadius = radius,
            style = Stroke(width = strokePx),
        )
    }
}

/**
 * The exact gradient color at [fraction] — i.e. the color of [TimeSinceBar]'s leading edge — by
 * linearly interpolating between the two surrounding [GradientStops] entries.
 */
fun gradientColorAt(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    val stops = GradientStops
    if (f <= stops.first().first) return stops.first().second
    if (f >= stops.last().first) return stops.last().second
    for (i in 1 until stops.size) {
        val (pos, color) = stops[i]
        if (f <= pos) {
            val (prevPos, prevColor) = stops[i - 1]
            val t = if (pos == prevPos) 0f else (f - prevPos) / (pos - prevPos)
            return lerp(prevColor, color, t)
        }
    }
    return stops.last().second
}

private val GradientStops: Array<Pair<Float, Color>> = arrayOf(
    0.0000f to Color(0xFF01FF01),
    0.0100f to Color(0xFF3EF61B),
    0.0200f to Color(0xFF50F51B),
    0.0300f to Color(0xFF5EF31B),
    0.0400f to Color(0xFF6AF21B),
    0.0500f to Color(0xFF74F01B),
    0.0600f to Color(0xFF7CEF1B),
    0.0700f to Color(0xFF84EE1A),
    0.0800f to Color(0xFF8AEC1A),
    0.0900f to Color(0xFF90EB1A),
    0.1000f to Color(0xFF96EA1A),
    0.1100f to Color(0xFF9BE91A),
    0.1200f to Color(0xFFA0E71A),
    0.1300f to Color(0xFFA5E61A),
    0.1400f to Color(0xFFA9E51A),
    0.1500f to Color(0xFFADE41A),
    0.1600f to Color(0xFFB1E31A),
    0.1700f to Color(0xFFB5E21A),
    0.1800f to Color(0xFFB8E11A),
    0.1900f to Color(0xFFBBE01A),
    0.2000f to Color(0xFFBFDF1A),
    0.2100f to Color(0xFFC2DE1A),
    0.2200f to Color(0xFFC5DD1A),
    0.2300f to Color(0xFFC7DC1A),
    0.2400f to Color(0xFFCADB1A),
    0.2500f to Color(0xFFCDDA1A),
    0.2600f to Color(0xFFCFD91A),
    0.2700f to Color(0xFFD2D81A),
    0.2800f to Color(0xFFD4D71A),
    0.2900f to Color(0xFFD7D61A),
    0.3000f to Color(0xFFD9D61A),
    0.3100f to Color(0xFFDBD51A),
    0.3200f to Color(0xFFDDD41A),
    0.3300f to Color(0xFFDFD31A),
    0.3400f to Color(0xFFE2D21A),
    0.3500f to Color(0xFFE4D11A),
    0.3600f to Color(0xFFE6D01A),
    0.3700f to Color(0xFFE8CF1A),
    0.3800f to Color(0xFFE9CF1A),
    0.3900f to Color(0xFFEBCE1A),
    0.4000f to Color(0xFFEDCD1A),
    0.4100f to Color(0xFFEFCC1A),
    0.4200f to Color(0xFFF1CB1A),
    0.4300f to Color(0xFFF3CA1A),
    0.4400f to Color(0xFFF5C91A),
    0.4500f to Color(0xFFF6C81A),
    0.4600f to Color(0xFFF8C81A),
    0.4700f to Color(0xFFFAC71A),
    0.4800f to Color(0xFFFCC61A),
    0.4900f to Color(0xFFFDC51A),
    0.5000f to Color(0xFFFFC41A),
    0.5100f to Color(0xFFFFC216),
    0.5200f to Color(0xFFFFBF10),
    0.5300f to Color(0xFFFFBC09),
    0.5400f to Color(0xFFFFBA01),
    0.5500f to Color(0xFFFFB91A),
    0.5600f to Color(0xFFFFB71E),
    0.5700f to Color(0xFFFFB51A),
    0.5800f to Color(0xFFFFB216),
    0.5900f to Color(0xFFFFB011),
    0.6000f to Color(0xFFFFAD0C),
    0.6100f to Color(0xFFFFAB04),
    0.6200f to Color(0xFFFFA90A),
    0.6300f to Color(0xFFFFA91E),
    0.6400f to Color(0xFFFFA61C),
    0.6500f to Color(0xFFFFA418),
    0.6600f to Color(0xFFFFA114),
    0.6700f to Color(0xFFFF9E0F),
    0.6800f to Color(0xFFFF9C09),
    0.6900f to Color(0xFFFF9903),
    0.7000f to Color(0xFFFF980E),
    0.7100f to Color(0xFFFF971D),
    0.7200f to Color(0xFFFF951A),
    0.7300f to Color(0xFFFF9216),
    0.7400f to Color(0xFFFF8F12),
    0.7500f to Color(0xFFFF8C0D),
    0.7600f to Color(0xFFFF8907),
    0.7700f to Color(0xFFFF8601),
    0.7800f to Color(0xFFFF8512),
    0.7900f to Color(0xFFFF841B),
    0.8000f to Color(0xFFFF8117),
    0.8100f to Color(0xFFFF7D13),
    0.8200f to Color(0xFFFF7A0F),
    0.8300f to Color(0xFFFF7608),
    0.8400f to Color(0xFFFF7202),
    0.8500f to Color(0xFFFF700D),
    0.8600f to Color(0xFFFF701A),
    0.8700f to Color(0xFFFF6C16),
    0.8800f to Color(0xFFFF6712),
    0.8900f to Color(0xFFFF620C),
    0.9000f to Color(0xFFFF5D05),
    0.9100f to Color(0xFFFF5804),
    0.9200f to Color(0xFFFF5814),
    0.9300f to Color(0xFFFF5416),
    0.9400f to Color(0xFFFF4D11),
    0.9500f to Color(0xFFFF450A),
    0.9600f to Color(0xFFFF3C03),
    0.9700f to Color(0xFFFF370B),
    0.9800f to Color(0xFFFF3516),
    0.9900f to Color(0xFFFF2811),
    1.0000f to Color(0xFFFF0101),
)
