package com.example.money.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.systemMotionEnabled

enum class MotionGlyph {
    Took,
    Returned,
    Check,
}

@Composable
fun MotionGlyphIcon(
    glyph: MotionGlyph,
    tint: Color,
    modifier: Modifier = Modifier,
    play: Boolean = false,
) {
    val motion = systemMotionEnabled(LocalContext.current)
    val progress = remember(glyph, play) { Animatable(if (motion && play) 0f else 1f) }
    val path = remember { Path() }
    val measure = remember { PathMeasure() }
    val drawn = remember { Path() }
    LaunchedEffect(glyph, play, motion) {
        if (motion && play) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(MoneyMotion.short, easing = MoneyMotion.emphasizedDecelerate))
        } else {
            progress.snapTo(1f)
        }
    }
    Canvas(modifier.size(22.dp)) {
        path.rewind()
        traceGlyph(path, glyph, size.width, size.height)
        measure.setPath(path, false)
        drawn.rewind()
        val length = measure.length
        if (length > 0f) {
            measure.getSegment(0f, length * progress.value, drawn, true)
        }
        drawPath(
            path = drawn,
            color = tint,
            style = Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun traceGlyph(path: Path, glyph: MotionGlyph, width: Float, height: Float) {
    when (glyph) {
        MotionGlyph.Check -> {
            path.moveTo(width * 0.18f, height * 0.52f)
            path.lineTo(width * 0.40f, height * 0.74f)
            path.lineTo(width * 0.82f, height * 0.28f)
        }
        MotionGlyph.Took -> {
            path.moveTo(width * 0.72f, height * 0.28f)
            path.lineTo(width * 0.28f, height * 0.72f)
            path.lineTo(width * 0.28f, height * 0.42f)
            path.lineTo(width * 0.28f, height * 0.72f)
            path.lineTo(width * 0.58f, height * 0.72f)
        }
        MotionGlyph.Returned -> {
            path.moveTo(width * 0.28f, height * 0.72f)
            path.lineTo(width * 0.72f, height * 0.28f)
            path.lineTo(width * 0.42f, height * 0.28f)
            path.lineTo(width * 0.72f, height * 0.28f)
            path.lineTo(width * 0.72f, height * 0.58f)
        }
    }
}

@Composable
fun PaidCheck(play: Boolean, tint: Color, modifier: Modifier = Modifier) {
    MotionGlyphIcon(
        glyph = MotionGlyph.Check,
        tint = tint,
        play = play,
        modifier = modifier,
    )
}
