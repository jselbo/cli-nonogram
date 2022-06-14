package com.joshuaselbo.nonogram

class Board(val cols: List<List<Int>>, val rows: List<List<Int>>) {

    private val emptyBlockIndicator = listOf(0)

    val states = Array(cols.size) {
        Array(rows.size) { CellState.EMPTY }
    }

    fun isSolved(): Boolean {
        for (colIndex in cols.indices) {
            val colStates = states[colIndex]
            var expected = cols[colIndex]
            if (expected == emptyBlockIndicator) {
                expected = emptyList()
            }
            if (countBlocks(colStates) != expected) {
                return false
            }
        }
        for (rowIndex in rows.indices) {
            val rowStates = states.map { col -> col[rowIndex] }
            var expected = rows[rowIndex]
            if (expected == emptyBlockIndicator) {
                expected = emptyList()
            }
            if (countBlocks(rowStates) != expected) {
                return false
            }
        }
        return true
    }

    private fun countBlocks(cells: Array<CellState>): List<Int> =
        countBlocks(cells.toList())

    private fun countBlocks(cells: List<CellState>): List<Int> {
        val counts = mutableListOf<Int>()

        var blockStart = -1
        for ((i, cell) in cells.withIndex()) {
            if (cell == CellState.FILL) {
                if (blockStart == -1) {
                    blockStart = i
                }
            } else {
                if (blockStart != -1) {
                    counts.add(i - blockStart)
                    blockStart = -1
                }
            }
        }
        if (blockStart != -1) {
            counts.add(cells.size - blockStart)
        }
        return counts
    }

    override fun toString(): String {
        return "Board{cols=$cols, rows=$rows}"
    }
}