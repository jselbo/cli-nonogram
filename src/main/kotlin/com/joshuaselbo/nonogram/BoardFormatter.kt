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
        val maxColBlockLen = board.cols.maxOf { col -> col.size }
        val colWidths = board.cols.map { col -> col.maxOf { n -> digitCount(n) } }
        val maxRowLen = board.rows.maxOf { row ->
            row.sumOf { n -> digitCount(n)+1 }
        }

        for (i in 0 until maxColBlockLen) {
            var line = "".padStart(maxRowLen-1)
            for ((colIndex, col) in board.cols.withIndex()) {
                val maxDigitLen = colWidths[colIndex]
                line += if (maxColBlockLen - i - 1 < col.size) {
                    val block = col[maxColBlockLen - i - 1]
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

        val separatorLen = maxRowLen + colWidths.sumOf { w -> w + 1 }
        formattedStr += "-".repeat(separatorLen) + "\n"

        for (i in 0 until board.rows.size) {
            val rowLen = board.rows[i].sumOf { n -> digitCount(n)+1 }
            var line = " ".repeat(maxRowLen - rowLen)
            for (n in board.rows[i]) {
                line += "$n|"
            }

            for (j in 0 until board.cols.size) {
                val cell = board.states[j][i].toFormatString()
                val pad = " ".repeat(colWidths[j] - 1)
                line += "$cell$pad|"
            }

            formattedStr += line + "\n"
        }

        formattedStr += "-".repeat(separatorLen) + "\n"

        val rowSize = board.rows.size
        return BoardFormat(formattedStr, colWidths, rowSize, maxColBlockLen, maxRowLen)
    }

    private fun digitCount(n: Int) = n.toString().length
}
