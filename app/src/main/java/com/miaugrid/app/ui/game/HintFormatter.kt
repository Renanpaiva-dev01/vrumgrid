package com.miaugrid.app.ui.game

import com.miaugrid.app.ui.theme.RegionColorNames
import com.miaugrid.core.hints.Hint
import com.miaugrid.core.rules.PlacementCheck
import com.miaugrid.core.rules.ViolationType
import com.miaugrid.core.solver.BoardUnit
import com.miaugrid.core.solver.Technique

/** Textos em português para dicas e erros. Mantidos fora do núcleo (apresentação). */
object HintFormatter {

    fun regionLabel(id: Int): String = "região ${'A' + id} (${RegionColorNames[id % RegionColorNames.size]})"

    fun unitLabel(unit: BoardUnit): String = when (unit) {
        is BoardUnit.Row -> "linha ${unit.index + 1}"
        is BoardUnit.Column -> "coluna ${unit.index + 1}"
        is BoardUnit.Region -> regionLabel(unit.id)
    }

    private fun list(units: List<BoardUnit>): String = when (units.size) {
        0 -> ""
        1 -> unitLabel(units[0])
        else -> units.dropLast(1).joinToString(", ") { unitLabel(it) } + " e " + unitLabel(units.last())
    }

    fun describe(hint: Hint, allowReveal: Boolean): HintDisplay = when (hint) {
        is Hint.MarkedSolutionCell -> HintDisplay(
            hint, "Há um X numa célula que precisa de um gato. Remova a marcação destacada.",
            emptyList(), hint.cells, hint.cells.first(), "Remover X",
        )
        is Hint.WrongCats -> HintDisplay(
            hint, "O gato destacado não faz parte da solução.",
            emptyList(), hint.cells, hint.cells.first(), "Remover gato",
        )
        is Hint.ForcedPlacement -> HintDisplay(
            hint, "A ${unitLabel(hint.unit)} só tem uma posição possível para um gato.",
            listOf(hint.unit), setOf(hint.position), hint.position, if (allowReveal) "Colocar gato" else null,
        )
        is Hint.CorrectPosition -> HintDisplay(
            hint, "Sem dedução simples aqui: a célula destacada tem um gato na solução.",
            emptyList(), setOf(hint.position), hint.position, if (allowReveal) "Colocar gato" else null,
        )
        is Hint.Elimination -> {
            val text = when (hint.technique) {
                Technique.CONFINEMENT ->
                    "Todas as posições possíveis da ${list(hint.sources)} estão na ${list(hint.targets)}. " +
                        "Por isso, as outras células destacadas não podem ter gato."
                Technique.GROUP_CONFINEMENT ->
                    "${list(hint.sources).replaceFirstChar { it.uppercase() }} só cabem em ${list(hint.targets)}. " +
                        "As demais células dessas áreas ficam impossíveis."
                Technique.CONTRADICTION ->
                    "Um gato na célula destacada deixaria uma linha, coluna ou região sem nenhuma posição possível."
                Technique.CAT_ELIMINATION, Technique.SINGLE_CANDIDATE ->
                    "As células destacadas não podem ter gato por causa dos gatos já colocados."
            }
            HintDisplay(hint, text, hint.sources, hint.cells, hint.cause, "Marcar com X")
        }
    }

    /** Mensagem de erro de colocação: regras primeiro, depois a solução oficial. */
    fun rejection(check: PlacementCheck): String {
        val types = check.violations.map { it.type }.toSet()
        return when {
            ViolationType.ROW_OCCUPIED in types -> "Já existe um gato nesta linha."
            ViolationType.COLUMN_OCCUPIED in types -> "Já existe um gato nesta coluna."
            ViolationType.REGION_OCCUPIED in types -> "Já existe um gato nesta região."
            ViolationType.ADJACENT_HORIZONTAL in types -> "Gatos não podem ficar lado a lado."
            ViolationType.ADJACENT_VERTICAL in types -> "Gatos não podem ficar um acima do outro."
            ViolationType.ADJACENT_DIAGONAL in types -> "Gatos não podem se tocar na diagonal."
            ViolationType.NOT_IN_SOLUTION in types -> "Este lugar não faz parte da solução."
            else -> "Jogada inválida."
        }
    }
}
