package com.joshuaselbo.nonogram

sealed class Board(val columnSize: Int, val rowSize: Int) {

    val states = Array(columnSize) {
        Array(rowSize) { CellState.EMPTY }
    }

    fun countColumnBlocks(): List<List<Int>> =
        states.map { col -> countBlocks(col.toList()) }

    fun countRowBlocks(): List<List<Int>> {
        val rowBlocks = mutableListOf<List<Int>>()
        for (rowIndex in 0 until rowSize) {
            val rowStates = states.map { col -> col[rowIndex] }
            rowBlocks.add(countBlocks(rowStates))
        }
        return rowBlocks
    }

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
        return "Board{columnSize=$columnSize, rows=$rowSize}"
    }
}