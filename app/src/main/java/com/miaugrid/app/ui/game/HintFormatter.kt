package com.miaugrid.app.ui.game

import com.miaugrid.app.ui.theme.RegionColorNames
import com.miaugrid.core.hints.Hint
import com.miaugrid.core.rules.PlacementCheck
import com.miaugrid.core.rules.ViolationType
import com.miaugrid.core.solver.BoardUnit
import com.miaugrid.core.solver.Technique

/** Textos em português para dicas e erros. Mantidos fora do núcleo (apresentação). */
object HintFormatter {

    fun regionLabel(id: Int): String = "bairro ${'A' + id} (${RegionColorNames[id % RegionColorNames.size]})"

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
            hint, "Tem um X numa vaga que precisa de um carro. Remova a marcação destacada.",
            emptyList(), hint.cells, hint.cells.first(), "Remover X",
        )
        is Hint.WrongCats -> HintDisplay(
            hint, "O carro destacado não faz parte da solução.",
            emptyList(), hint.cells, hint.cells.first(), "Retirar carro",
        )
        is Hint.ForcedPlacement -> HintDisplay(
            hint, "${unitLabel(hint.unit).replaceFirstChar { it.uppercase() }} só tem uma vaga possível para um carro.",
            listOf(hint.unit), setOf(hint.position), hint.position, if (allowReveal) "Estacionar carro" else null,
        )
        is Hint.CorrectPosition -> HintDisplay(
            hint, "Sem dedução simples aqui: a vaga destacada tem um carro na solução.",
            emptyList(), setOf(hint.position), hint.position, if (allowReveal) "Estacionar carro" else null,
        )
        is Hint.Elimination -> {
            val text = when (hint.technique) {
                Technique.CONFINEMENT ->
                    "Todas as vagas possíveis de ${list(hint.sources)} estão em ${list(hint.targets)}. " +
                        "Por isso, as outras células destacadas não podem ter carro."
                Technique.GROUP_CONFINEMENT ->
                    "${list(hint.sources).replaceFirstChar { it.uppercase() }} só cabem em ${list(hint.targets)}. " +
                        "As demais células dessas áreas ficam impossíveis."
                Technique.CONTRADICTION ->
                    "Um carro na célula destacada deixaria uma linha, coluna ou bairro sem nenhuma vaga possível."
                Technique.CAT_ELIMINATION, Technique.SINGLE_CANDIDATE ->
                    "As células destacadas não podem ter carro por causa dos carros já estacionados."
            }
            HintDisplay(hint, text, hint.sources, hint.cells, hint.cause, "Marcar com X")
        }
    }

    /** Mensagem de erro de colocação: regras primeiro, depois a solução oficial. */
    fun rejection(check: PlacementCheck): String {
        val types = check.violations.map { it.type }.toSet()
        return when {
            ViolationType.ROW_OCCUPIED in types -> "Já tem um carro nesta linha."
            ViolationType.COLUMN_OCCUPIED in types -> "Já tem um carro nesta coluna."
            ViolationType.REGION_OCCUPIED in types -> "Já tem um carro neste bairro."
            ViolationType.ADJACENT_HORIZONTAL in types -> "Carros não podem estacionar lado a lado."
            ViolationType.ADJACENT_VERTICAL in types -> "Carros não podem estacionar um atrás do outro."
            ViolationType.ADJACENT_DIAGONAL in types -> "Carros não podem encostar na diagonal."
            ViolationType.NOT_IN_SOLUTION in types -> "Esta vaga não faz parte da solução."
            else -> "Jogada inválida."
        }
    }
}
