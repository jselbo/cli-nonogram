package com.joshuaselbo.nonogram

class BoardFormat(
    val contents: String,
    /** Terminal cursor row index of first cell */
    val startRowIndex: Int,
    /** Terminal cursor col index of first cell */
    val startColIndex: Int,
    /** Terminal cursor row index after the puzzle end */
    val endRowIndex: Int)

class BoardFormatter {

    // assumes single digit block lengths... oof
    // todo support multiple digits
    fun format(board: Board): BoardFormat {
        var formattedStr = ""
        val maxColLen = board.cols.maxOf { col -> col.size }
        val maxRowLen = board.rows.maxOf { row -> row.size }

        for (i in 0 until maxColLen) {
            var line = "".padStart(maxRowLen + 1)
            for (col in board.cols) {
                val char = if (maxColLen - i - 1 < col.size) col[maxColLen - i - 1] else ' '
                line += "|$char"
            }
            line += "|\n"
            formattedStr += line
        }

        val separatorLen = maxRowLen + 1 + board.cols.size * 2 + 1
        formattedStr += "-".repeat(separatorLen) + "\n"

        for (i in 0 until board.rows.size) {
            val pad = (maxRowLen - board.rows[i].size) * 2
            var line = "".padStart(pad)
            for (n in board.rows[i]) {
                line += "$n|"
            }

            for (j in 0 until board.cols.size) {
                val cell = board.states[j][i].toFormatString()
                line += "$cell|"
            }

            formattedStr += line + "\n"
        }

        formattedStr += "-".repeat(separatorLen) + "\n"

        val startRowIndex = maxColLen + 3
        val startColIndex = maxRowLen*2 + 1
        val endRowIndex = startRowIndex + board.rows.size + 2
        return BoardFormat(formattedStr, startRowIndex, startColIndex, endRowIndex)
    }
}
