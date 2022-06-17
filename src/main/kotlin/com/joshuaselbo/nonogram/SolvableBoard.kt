package com.joshuaselbo.nonogram

class SolvableBoard(
    val columnBlocks: List<List<Int>>,
    val rowBlocks: List<List<Int>>) : Board(columnBlocks.size, rowBlocks.size) {

    fun isSolved(): Boolean {
        val colBlocks = countColumnBlocks()
        val expectedColBlocks = sanitize(columnBlocks)
        if (colBlocks != expectedColBlocks) {
            return false
        }
        val rowBlocks = countRowBlocks()
        val expectedRowBlocks = sanitize(rowBlocks)
        return rowBlocks == expectedRowBlocks
    }

    private fun sanitize(colOrRowBlocks: List<List<Int>>): List<List<Int>> =
        colOrRowBlocks.map { blocks ->
            if (blocks == emptyBlockIndicator) emptyList() else blocks
        }

    companion object {
        val emptyBlockIndicator = listOf(0)
    }
}