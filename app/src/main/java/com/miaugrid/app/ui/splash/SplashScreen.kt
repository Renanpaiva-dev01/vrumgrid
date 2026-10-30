package com.miaugrid.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.miaugrid.app.ui.components.LocalCarLook
import com.miaugrid.app.ui.components.drawCar
import com.miaugrid.app.ui.theme.Road
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Abertura curta: o carrinho do jogador entra pela esquerda, freia no centro (com um
 * leve "mergulho" de frenagem) e o nome aparece. Os dados carregam em paralelo.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish = rememberUpdatedState(onFinished)
    val drive = remember { Animatable(0f) }
    val brake = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        drive.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        launch { brake.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f)) }
        title.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f))
        delay(350)
        finish.value()
    }
    val look = LocalCarLook.current
    val measurer = rememberTextMeasurer()
    Column(
        Modifier
            .fillMaxSize()
            .background(Road.AsphaltDeep)
            .testTag("splash")
            .semantics(mergeDescendants = true) { contentDescription = "VrumGrid" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val h = size.height
            val y = h / 2
            drawLine(
                Road.LanePaint.copy(alpha = 0.6f), Offset(0f, y + h * 0.42f), Offset(size.width, y + h * 0.42f), h * 0.03f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(h * 0.25f, h * 0.18f), phase = -drive.value * h * 2),
            )
            val carLength = h * 0.9f
            val x = -carLength + (size.width / 2 + carLength) * drive.value
            // Linhas de velocidade enquanto o carro ainda está em movimento.
            val speed = 1f - drive.value
            if (speed > 0.02f) {
                for (k in 0 until 3) {
                    val ly = y + (k - 1) * h * 0.18f
                    drawLine(Color.White.copy(alpha = 0.5f * speed), Offset(x - carLength * (0.7f + k * 0.15f), ly), Offset(x - carLength * (1.4f + speed), ly), h * 0.02f)
                }
            }
            val tilt = (1f - brake.value) * 6f * (if (drive.value >= 1f) 1f else 0f)
            drawCar(Offset(x, y), carLength, look, rotation = 90f + tilt, textMeasurer = measurer)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "VrumGrid",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            modifier = Modifier.graphicsLayer {
                alpha = title.value.coerceIn(0f, 1f)
                translationY = (1f - title.value) * 40f
            },
        )
        Text(
            "Um carro por linha, coluna e bairro",
            style = MaterialTheme.typography.bodyMedium,
            color = Road.TaxiYellow,
            modifier = Modifier.alpha(title.value.coerceIn(0f, 1f)),
        )
    }
}
