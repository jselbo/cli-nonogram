package com.joshuaselbo.nonogram

class BoardFormat(
    val contents: String,
    // Not including "|" char
    private val colWidths: List<Int>,
    private val rowSize: Int,
    maxColBlockLen: Int,
    maxRowLen: Int) {

    private val startRowIndex = maxColBlockLen + 3
    private val startColIndex = maxRowLen

    fun getCursorPosition(row: Int, col: Int): Pair<Int, Int> {
        val cursorRow = startRowIndex + row
        var cursorCol = startColIndex+1
        for (i in 0 until col) {
            cursorCol += colWidths[i]+1
        }
        return Pair(cursorRow, cursorCol)
    }

    fun getEndCursorPosition(): Pair<Int, Int> {
        val endRowIndex = startRowIndex + rowSize + 2
        return Pair(endRowIndex, 1)
    }
}

class BoardFormatter {

    fun format(board: Board): BoardFormat {
        var formattedStr = ""

        val maxColBlockLen: Int
        val colWidths: List<Int>
        val maxRowLen: Int
        when (board) {
            is SolvableBoard -> {
                maxColBlockLen = board.columnBlocks.maxOf { col -> col.size }
                colWidths = board.columnBlocks.map { col -> col.maxOf { n -> digitCount(n) } }
                maxRowLen = board.rowBlocks.maxOf { row ->
                    row.sumOf { n -> digitCount(n)+1 }
                }
            }
            is PuzzleCreatorBoard -> {
                maxColBlockLen = 0
                colWidths = List(board.columnSize) { 1 }
                maxRowLen = 1
            }
        }

        if (board is SolvableBoard) {
            for (i in 0 until maxColBlockLen) {
                // TODO consider uniform column widths
                var line = "".padStart(maxRowLen - 1)
                for ((colIndex, col) in board.columnBlocks.withIndex()) {
                    val maxDigitLen = colWidths[colIndex]
                    val offset = maxColBlockLen - col.size
                    line += if (i - offset >= 0) {
                        val block = col[i - offset]
                        val digitCount = digitCount(block)
                        val padded = block.toString() + " ".repeat(maxDigitLen - digitCount)
                        "|$padded"
                    } else {
                        val padded = " ".repeat(maxDigitLen)
                        "|$padded"
                    }
                }
                line += "|\n"
                formattedStr += line
            }
        }

        val separatorLen = maxRowLen + colWidths.sum() + board.columnSize
        formattedStr += "-".repeat(separatorLen) + "\n"

        for (i in 0 until board.rowSize) {
            var line = ""
            when (board) {
                is SolvableBoard -> {
                    val rowLen = board.rowBlocks[i].sumOf { n -> digitCount(n)+1 }
                    line += " ".repeat(maxRowLen - rowLen)
                    for (n in board.rowBlocks[i]) {
                        line += "$n|"
                    }
                }
                is PuzzleCreatorBoard -> {
                    line += "|"
                }
            }

            for (j in 0 until board.columnSize) {
                val cell = board.states[j][i].toFormatString()
                val pad = " ".repeat(colWidths[j] - 1)
                line += "$cell$pad|"
            }

            formattedStr += line + "\n"
        }

        formattedStr += "-".repeat(separatorLen) + "\n"

        return BoardFormat(formattedStr, colWidths, board.rowSize, maxColBlockLen, maxRowLen)
    }

    private fun digitCount(n: Int) = n.toString().length
}
