package com.miaugrid.core.game

import kotlinx.serialization.Serializable

@Serializable
enum class GameModeId { CLASSIC, DAILY }

/**
 * Política de validação de colocações de gato.
 *
 * - [STRICT_SOLUTION]: a colocação precisa coincidir com a solução oficial. Qualquer gato
 *   fora da solução (mesmo respeitando as regras locais) é recusado e custa uma vida.
 * - [RULES_ONLY]: só quebras de regra (linha, coluna, região, vizinhança) são recusadas e
 *   custam vida. Um gato que respeita as regras mas não pertence à solução é aceito sem
 *   penalidade; a vitória continua exigindo um tabuleiro totalmente válido.
 */
@Serializable
enum class ValidationPolicy { STRICT_SOLUTION, RULES_ONLY }

/** Parâmetros comerciais da recuperação por anúncio recompensado. */
@Serializable
data class RecoveryPolicy(
    val enabled: Boolean = true,
    val maxRecoveriesPerGame: Int = 1,
    val livesRestored: Int = 1,
) {
    init {
        require(maxRecoveriesPerGame >= 0)
        require(livesRestored >= 1)
    }
}

/** Custos das dicas. Dicas grátis primeiro; depois, opcionalmente, por anúncio recompensado. */
@Serializable
data class HintPolicy(
    val freeHintsPerLevel: Int = 3,
    val rewardedAdForExtraHints: Boolean = true,
    /** Se o modo permite revelar a posição de um gato (colocação automática). */
    val allowReveal: Boolean = true,
)

/**
 * Configuração completa de um modo de jogo. Todas as regras de vidas, desfazer e
 * reinício ficam aqui, num único lugar testado.
 */
@Serializable
data class GameModeConfig(
    val id: GameModeId,
    val validation: ValidationPolicy = ValidationPolicy.STRICT_SOLUTION,
    val initialLives: Int = 3,
    val allowRemovingCats: Boolean = true,
    /** Reiniciar devolve as vidas iniciais e zera o contador de erros da tentativa. */
    val restartRestoresLives: Boolean = true,
    val recovery: RecoveryPolicy = RecoveryPolicy(),
    val hints: HintPolicy = HintPolicy(),
) {
    init {
        require(initialLives >= 1)
    }

    companion object {
        val CLASSIC = GameModeConfig(id = GameModeId.CLASSIC)
        val DAILY = GameModeConfig(
            id = GameModeId.DAILY,
            hints = HintPolicy(freeHintsPerLevel = 1),
        )

        fun forId(id: GameModeId): GameModeConfig = when (id) {
            GameModeId.CLASSIC -> CLASSIC
            GameModeId.DAILY -> DAILY
        }
    }
}
