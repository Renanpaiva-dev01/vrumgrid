package com.miaugrid.core.settings

import kotlinx.serialization.Serializable

/** Modelos de carrinho disponíveis na garagem. */
@Serializable
enum class CarBody { SEDAN, SPORT, BUGGY, PICKUP, VAN, FORMULA }

/** Adesivo/pintura extra. */
@Serializable
enum class CarDecal { NONE, STRIPES, NUMBER, CHECKERED, FLAMES }

/** Velocidade do carrinho que faz as marcações automáticas. */
@Serializable
enum class CarSpeed(val millisPerCell: Int) { CALM(150), NORMAL(95), TURBO(50) }

/**
 * Aparência do carrinho do jogador, editada na garagem. Cores em ARGB.
 */
@Serializable
data class CarStyle(
    val body: CarBody = CarBody.SEDAN,
    val bodyColor: Long = 0xFFE5383B,
    val accentColor: Long = 0xFFFFFFFF,
    val decal: CarDecal = CarDecal.STRIPES,
    val number: Int = 7,
) {
    init {
        require(number in 0..99) { "Número do carro deve estar entre 0 e 99" }
    }
}

@Serializable
enum class ControlMode {
    /** Toque simples marca X; toque duplo estaciona um carro (padrão). */
    DOUBLE_TAP,
    /** Ferramenta escolhida na barra (X ou carro) é aplicada no toque simples; toque duplo continua estacionando. */
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
    /** Ao estacionar um carro, o carrinho percorre o tabuleiro marcando X nas células bloqueadas. */
    val autoMarkWithCar: Boolean = true,
    val carSpeed: CarSpeed = CarSpeed.NORMAL,
    val car: CarStyle = CarStyle(),
    val adsPersonalizationConsentAsked: Boolean = false,
)
