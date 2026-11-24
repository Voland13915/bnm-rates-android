package md.bnm.rates.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import md.bnm.rates.core.PeriodStats
import md.bnm.rates.core.RatePoint

/**
 * Линейный график курса по дням. Ось Y — от минимума до максимума периода,
 * поэтому даже малые колебания видны; точки минимума и максимума крупнее.
 */
@Composable
fun LineChart(points: List<RatePoint>, stats: PeriodStats, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier) {
        Text("макс. ${Format.rate(stats.max.rate)}", style = labelStyle, color = labelColor)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 8.dp)
        ) {
            val inset = 6.dp.toPx()
            val low = stats.min.rate.toFloat()
            val high = stats.max.rate.toFloat()
            val width = size.width - 2 * inset
            val height = size.height - 2 * inset

            fun x(index: Int): Float =
                if (points.size > 1) inset + width * index / (points.size - 1) else size.width / 2

            fun y(rate: Float): Float =
                if (high > low) inset + height * (high - rate) / (high - low) else size.height / 2

            for (line in 0..2) {
                val gridY = inset + height * line / 2
                drawLine(gridColor, Offset(0f, gridY), Offset(size.width, gridY), strokeWidth = 1.dp.toPx())
            }

            if (points.size > 1) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val px = x(index)
                    val py = y(point.rate.toFloat())
                    if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path,
                    lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            points.forEachIndexed { index, point ->
                val extreme = point == stats.min || point == stats.max
                drawCircle(
                    lineColor,
                    radius = (if (extreme) 5 else 3).dp.toPx(),
                    center = Offset(x(index), y(point.rate.toFloat()))
                )
            }
        }
        Text("мин. ${Format.rate(stats.min.rate)}", style = labelStyle, color = labelColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Format.shortDate(stats.first.date), style = labelStyle, color = labelColor)
            Text(Format.shortDate(stats.last.date), style = labelStyle, color = labelColor)
        }
    }
}
