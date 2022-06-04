import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.PrintWriter
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

// Only one puzzle for now :)
val board1Cols = listOf(listOf(1,1,1), listOf(2,2), listOf(5), listOf(1), listOf(1))
val board1Rows = listOf(listOf(3), listOf(2), listOf(1,3), listOf(2), listOf(3))
val board1 = Board(board1Cols, board1Rows)

var rowCursor = 0
var colCursor = 0

enum class Action {
    UP, DOWN, LEFT, RIGHT, MARK_DOT, MARK_FILL
}

fun main() {
    // Enable jline debug logging
    Logger.getLogger("org.jline").level = Level.FINE

    val terminal: Terminal = TerminalBuilder.builder()
        .jna(false)
        .jansi(true)
        .build()
    terminal.enterRawMode()

    // On Windows it seems interrupt (ctrl+c) needs to be handled manually
    terminal.handle(Terminal.Signal.INT) {
        exitProcess(0)
    }

    val bindingReader = BindingReader(terminal.reader())
    val keyMap = KeyMap<Action>()
    keyMap.bind(Action.UP, ANSI_UP, ANSI_UP_WIN)
    keyMap.bind(Action.DOWN, ANSI_DOWN, ANSI_DOWN_WIN)
    keyMap.bind(Action.RIGHT, ANSI_RIGHT, ANSI_RIGHT_WIN)
    keyMap.bind(Action.LEFT, ANSI_LEFT, ANSI_LEFT_WIN)
    keyMap.bind(Action.MARK_DOT, "x")
    keyMap.bind(Action.MARK_FILL, " ")

    val writer = terminal.writer()

    writer.println()
    writer.println("== Nonogram Puzzle ==")
    writer.println()
    writer.println("$ANSI_BOLD Controls$ANSI_RESET")
    writer.println("  - Up, Down, Right, Left arrow keys")
    writer.println("  - Space -> Mark as filled")
    writer.println("  - x     -> Mark as empty")
    writer.println()
    writer.println("Press any key to continue...")
    writer.flush()

    terminal.reader().read()

    var solved = false
    while (!solved) {
        writer.println(ANSI_CLEAR)
        printBoard(writer, board1)

        when (bindingReader.readBinding(keyMap)) {
            Action.UP -> rowCursor = max(0, rowCursor-1)
            Action.DOWN -> rowCursor = min(board1.rows.size-1, rowCursor+1)
            Action.LEFT -> colCursor = max(0, colCursor-1)
            Action.RIGHT -> colCursor = min(board1.cols.size-1, colCursor+1)
            Action.MARK_DOT -> {
                val curr = board1.grid[colCursor][rowCursor]
                board1.grid[colCursor][rowCursor] = if (curr == CellState.DOT) CellState.EMPTY else CellState.DOT
            }
            Action.MARK_FILL -> {
                val curr = board1.grid[colCursor][rowCursor]
                board1.grid[colCursor][rowCursor] = if (curr == CellState.FILL) CellState.EMPTY else CellState.FILL
            }
            else -> {
                // May be null on end of stream, if the program is already dying from ctrl+c
            }
        }

        solved = isSolved()
    }

    writer.println(ANSI_CLEAR)
    printBoard(writer, board1)

    writer.println()
    writer.println("You solved it!!!")
    writer.flush()
}

// assumes single digit block lengths... oof
fun printBoard(writer: PrintWriter, board: Board) {
    val maxColLen = board.cols.maxOf { col -> col.size }
    val maxRowLen = board.rows.maxOf { row -> row.size }

    for (i in 0 until maxColLen) {
        var line = "".padStart(maxRowLen+1)
        for (col in board.cols) {
            val char = if (maxColLen-i-1 < col.size) col[maxColLen-i-1] else ' '
            line += "|$char"
        }
        line += "|"
        writer.println(line)
    }

    val separatorLen = maxRowLen + 1 + board.cols.size*2 + 1
    writer.println("-".repeat(separatorLen))

    for (i in 0 until board.rows.size) {
        val pad = (maxRowLen - board.rows[i].size)*2
        var line = "".padStart(pad)
        for (n in board.rows[i]) {
            line += "$n|"
        }

        for (j in 0 until board.cols.size) {
            val cell = board.grid[j][i]
            line += if (i == rowCursor && j == colCursor) {
                "$ANSI_GREEN$ANSI_UNDERLINE$cell$ANSI_RESET"
            } else {
                cell.toString()
            }
            line += "|"
        }

        writer.println(line)
    }

    writer.println("-".repeat(separatorLen))
    writer.flush()
}

// doesn't work with "0" block length
private fun isSolved(): Boolean {
    for (colI in 0 until board1.cols.size) {
        val col = board1.cols[colI].toMutableList()
        var rowI = 0
        var blockCount = 0
        while (col.isNotEmpty()) {
            if (rowI >= board1.rows.size) {
                return false
            }
            if (board1.grid[colI][rowI] == CellState.FILL) {
                blockCount++
            }
            if (blockCount == col.first()) {
                col.removeFirst()
                blockCount = 0
            }
            rowI++
        }
        while (rowI < board1.rows.size) {
            if (board1.grid[colI][rowI] == CellState.FILL) {
                return false
            }
            rowI++
        }
    }
    for (rowI in 0 until board1.rows.size) {
        val row = board1.rows[rowI].toMutableList()
        var colI = 0
        var blockCount = 0
        while (row.isNotEmpty()) {
            if (colI >= board1.cols.size) {
                return false
            }
            if (board1.grid[colI][rowI] == CellState.FILL) {
                blockCount++
            }
            if (blockCount == row.first()) {
                row.removeFirst()
                blockCount = 0
            }
            colI++
        }
        while (colI < board1.cols.size) {
            if (board1.grid[colI][rowI] == CellState.FILL) {
                return false
            }
            colI++
        }
    }
    return true
}

enum class CellState {
    EMPTY, DOT, FILL;

    override fun toString(): String {
        return when (this) {
            EMPTY -> " "
            DOT -> "·"
            FILL -> "■"
        }
    }
}

class Board(val cols: List<List<Int>>, val rows: List<List<Int>>) {
    val grid = Array(cols.size) {
        Array(rows.size) { CellState.EMPTY }
    }
}