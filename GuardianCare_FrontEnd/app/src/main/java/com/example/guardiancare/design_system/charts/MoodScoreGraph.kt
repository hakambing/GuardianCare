package com.example.guardiancare.design_system.charts

import android.util.Log
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.cartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shape.rounded
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.example.guardiancare.data.models.CheckInResponse
import com.patrykandpatrick.vico.core.cartesian.CartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Insets
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.cartesian.FadingEdges
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAG = "MoodScoreGraph"

private val YDecimalFormat = DecimalFormat("#")
private val StartAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
    YDecimalFormat.format(value)
}
private val BottomAxisLabelKey = ExtraStore.Key<List<String>>()
private val BottomAxisValueFormatter = CartesianValueFormatter { context, x, _ ->
    context.model.extraStore[BottomAxisLabelKey][x.toInt()]
}

fun parseDate(dateString: String?): Date? {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    return dateString?.let {
        return try {
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    } ?: run {
        null
    }
}

fun formatDate(date: Date): String {
    val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    return try {
        outputFormat.format(date)
    } catch (e: Exception) {
        "Invalid Date"
    }
}

@Composable
private fun rememberHorizontalLine(y: Double = 0.0, label: String = "Neutral"): HorizontalLine {
    val fill = fill(MaterialTheme.colorScheme.primaryContainer)
    val line = rememberLineComponent(fill = fill, thickness = 2.dp)
    val labelComponent =
        rememberTextComponent(
            margins = Insets(),
            padding = Insets(6f, 2f),
            background = rememberShapeComponent(fill, CorneredShape.rounded(bottomLeft = 4.dp, bottomRight = 4.dp)),
            color = Color.White
        )
    return remember {
        HorizontalLine(
            y = { y },
            line = line,
            labelComponent = labelComponent,
            label = { label },
            verticalLabelPosition = Position.Vertical.Bottom,
            horizontalLabelPosition = Position.Horizontal.Center,
        )
    }
}


@Composable
private fun MoodScoreGraph(
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
) {
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(MaterialTheme.colorScheme.primary)),
                        areaFill = LineCartesianLayer.AreaFill.double(
                            fill(
                                ShaderProvider.verticalGradient(
                                    intArrayOf(Color(0xFFC0F0A3).toArgb(), Color.Transparent.toArgb())
                                )
                            ),
                            fill(
                                ShaderProvider.verticalGradient(
                                    intArrayOf(Color.Transparent.toArgb(), MaterialTheme.colorScheme.error.copy(0.5f).toArgb())
                                )
                            ),
                        ),
                        pointConnector = LineCartesianLayer.PointConnector.cubic(),
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.Point(rememberShapeComponent(fill(MaterialTheme.colorScheme.primaryContainer), CorneredShape.Pill))
                        ),

                    ),
                ),
                rangeProvider = CartesianLayerRangeProvider.fixed(minY = -3.0, maxY = 3.0),
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = BottomAxisValueFormatter,
            ),
            decorations = listOf(rememberHorizontalLine(3.0, "Very Positive"), rememberHorizontalLine(0.0, "Neutral"), rememberHorizontalLine(-3.0, "Very Negative")),
            layerPadding = { cartesianLayerPadding(scalableStart = 8.dp, scalableEnd = 8.dp) },
            fadingEdges = FadingEdges()
        ),
        modelProducer = modelProducer,
        modifier = modifier.height(height),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
}

@Composable
fun MoodScoreGraph(
    checkIns: List<CheckInResponse>,
    modifier: Modifier = Modifier,
    height: Dp = 400.dp,
    onDataPointSelected: (CheckInResponse) -> Unit = {}
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    val sortedCheckIns = checkIns.sortedBy {
        parseDate(it.created_at)
    }

    val data = sortedCheckIns
        .mapNotNull { checkIn ->
            parseDate(checkIn.created_at)?.let { date ->
                checkIn.mood?.let { mood ->
                    formatDate(date) to mood.toFloat()
                }
            }
        }
        .toMap()

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries { series(data.values) }
            extras { it[BottomAxisLabelKey] = data.keys.toList() }
        }
    }

    MoodScoreGraph(modelProducer, modifier, height)
}
