package app.expressiveclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

// Start over Australia. Normalize before trigonometry so 360° and 0° project identically.
internal fun globeRotationDegrees(phase: Float): Float = ((phase % 360f + 360f) % 360f) - 135f

/** Orthographic globe: land rotates in 3D and is clipped at the horizon, never mirrored. */
@Composable
internal fun RotatingGlobe(phase: () -> Float, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // Same 2-unit contour as the adjacent Material icons, scaled from their 24-unit viewport.
        val outlineWidth = size.minDimension * (2f / 24f)
        val radius = size.minDimension / 2f - maxOf(1.dp.toPx(), outlineWidth / 2f)
        val angle = globeRotationDegrees(phase()) * PI.toFloat() / 180f
        fun vertex(lon: Float, lat: Float): GlobePoint {
            val longitude = lon * PI.toFloat() / 180f + angle
            val latitude = lat * PI.toFloat() / 180f
            return GlobePoint(cos(latitude) * sin(longitude), -sin(latitude), cos(latitude) * cos(longitude))
        }
        fun projected(p: GlobePoint) = center + Offset(p.x, p.y) * (radius * (3f / (3f - p.z * .12f)))
        // The previous faint lines were explicit meridian strokes. Land is now a single
        // unioned fill, avoiding antialias seams where continent polygons overlap.
        var land = Path()
        for (continent in continents) {
            val points = continent.map { (lon, lat) -> vertex(lon, lat) }
            val front = mutableListOf<GlobePoint>()
            for (i in points.indices) {
                val a = points[i]; val b = points[(i + 1) % points.size]
                if (a.z >= 0) front.add(a)
                if ((a.z >= 0) != (b.z >= 0)) {
                    val t = a.z / (a.z - b.z)
                    front.add(GlobePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, 0f))
                }
            }
            if (front.size >= 3) {
                val path = Path()
                front.forEachIndexed { i, p ->
                    val at = projected(p)
                    if (i == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
                }
                path.close()
                land = Path.combine(PathOperation.Union, land, path)
            }
        }
        drawPath(land, tint)
        drawCircle(tint, radius, style = Stroke(outlineWidth))
    }
}

private data class GlobePoint(val x: Float, val y: Float, val z: Float)
private val continents = listOf(
    listOf(-168f to 65f, -130f to 70f, -55f to 50f, -65f to 20f, -85f to 10f, -105f to 25f, -125f to 50f),
    listOf(-80f to 10f, -50f to 0f, -35f to -10f, -65f to -55f, -75f to -20f),
    listOf(-15f to 35f, 15f to 37f, 50f to 12f, 35f to -5f, 20f to -35f, 5f to -30f, -18f to 10f),
    listOf(-10f to 36f, -10f to 60f, 25f to 70f, 80f to 75f, 170f to 60f, 145f to 35f,
        120f to 20f, 110f to 0f, 90f to 22f, 75f to 5f, 55f to 25f, 25f to 35f),
    listOf(112f to -12f, 145f to -10f, 155f to -25f, 135f to -40f, 115f to -30f),
    listOf(-55f to 60f, -25f to 75f, -45f to 84f, -65f to 75f),
)
