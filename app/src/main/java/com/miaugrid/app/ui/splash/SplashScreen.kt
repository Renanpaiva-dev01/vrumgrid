package com.miaugrid.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.miaugrid.app.ui.components.CatIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tela de abertura curta e animada. Não bloqueia: os dados carregam em paralelo. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish = rememberUpdatedState(onFinished)
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(350)) }
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
        delay(450)
        finish.value()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("splash"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CatIcon(Modifier.size(140.dp).scale(scale.value), description = "Gato do MiauGrid")
        Spacer(Modifier.height(16.dp))
        Text("MiauGrid", style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(alpha.value))
        Text("Um gato por linha, coluna e região", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.alpha(alpha.value))
    }
}
