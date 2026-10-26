package com.miaugrid.core.model

/**
 * Geometria de um tabuleiro N x N dividido em N regiões.
 *
 * Cada célula guarda apenas um identificador lógico de região (0 until size); a cor usada
 * na interface é decidida pela camada de apresentação. As regiões podem ter qualquer
 * formato, desde que sejam conexas (vizinhança ortogonal) e não vazias.
 */
class PuzzleLayout(val size: Int, regionIds: IntArray) {

    private val regions: IntArray = regionIds.copyOf()

    /** Células de cada região, na ordem de leitura do tabuleiro. */
    val regionCells: List<List<Position>>

    init {
        require(size in MIN_SIZE..MAX_SIZE) { "Tamanho de tabuleiro inválido: $size" }
        require(regions.size == size * size) {
            "Esperadas ${size * size} células, recebidas ${regions.size}"
        }
        require(regions.all { it in 0 until size }) { "Identificador de região fora de 0..${size - 1}" }
        val cells = List(size) { mutableListOf<Position>() }
        for (index in regions.indices) cells[regions[index]].add(Position(index / size, index % size))
        require(cells.all { it.isNotEmpty() }) { "Todas as $size regiões precisam ter ao menos uma célula" }
        regionCells = cells
        cells.forEachIndexed { id, list -> require(isConnected(list)) { "A região $id não é conexa" } }
    }

    val cellCount: Int get() = size * size

    fun contains(position: Position): Boolean =
        position.row in 0 until size && position.col in 0 until size

    fun regionAt(position: Position): Int {
        require(contains(position)) { "Posição fora do tabuleiro: $position" }
        return regions[position.row * size + position.col]
    }

    fun regionAt(row: Int, col: Int): Int = regionAt(Position(row, col))

    fun regionIds(): IntArray = regions.copyOf()

    /** Vizinhos existentes (8 direções) dentro dos limites do tabuleiro. */
    fun neighbors(position: Position): List<Position> = buildList {
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val p = Position(position.row + dr, position.col + dc)
            if (contains(p)) add(p)
        }
    }

    fun allPositions(): List<Position> = List(cellCount) { Position(it / size, it % size) }

    private fun isConnected(cells: List<Position>): Boolean {
        val set = cells.toHashSet()
        val seen = HashSet<Position>()
        val stack = ArrayDeque<Position>().apply { add(cells.first()) }
        while (stack.isNotEmpty()) {
            val p = stack.removeLast()
            if (!seen.add(p)) continue
            for ((dr, dc) in ORTHOGONAL) {
                val n = Position(p.row + dr, p.col + dc)
                if (n in set && n !in seen) stack.add(n)
            }
        }
        return seen.size == set.size
    }

    override fun equals(other: Any?): Boolean =
        other is PuzzleLayout && other.size == size && other.regions.contentEquals(regions)

    override fun hashCode(): Int = 31 * size + regions.contentHashCode()

    /** Representação textual: uma linha por fileira, uma letra por região (A, B, C...). */
    fun toRows(): List<String> = List(size) { r ->
        buildString { for (c in 0 until size) append('A' + regions[r * size + c]) }
    }

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 12
        private val ORTHOGONAL = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        fun fromRows(rows: List<String>): PuzzleLayout {
            val size = rows.size
            require(rows.all { it.length == size }) { "O tabuleiro precisa ser quadrado" }
            val ids = IntArray(size * size)
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, ch -> ids[r * size + c] = ch.uppercaseChar() - 'A' }
            }
            return PuzzleLayout(size, ids)
        }
    }
}
