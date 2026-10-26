package com.miaugrid.core.settings

import kotlinx.serialization.Serializable

@Serializable
enum class ControlMode {
    /** Toque simples marca X; toque duplo coloca gato (padrão). */
    DOUBLE_TAP,
    /** Ferramenta escolhida na barra (X ou gato) é aplicada no toque simples; toque duplo continua colocando gato. */
    TOOL_SELECT,
}

@Serializable
data class UserSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showTimer: Boolean = true,
    val highlightLines: Boolean = true,
    /** Mostra as células bloqueadas confirmadas pelo motor (pontinhos), separadas dos X do jogador. */
    val showBlockedCells: Boolean = true,
    /** Padrões e letras nas regiões, para quem tem dificuldade em distinguir cores. */
    val colorBlindAssist: Boolean = false,
    val controlMode: ControlMode = ControlMode.DOUBLE_TAP,
    val confirmRestart: Boolean = true,
    val adsPersonalizationConsentAsked: Boolean = false,
)
