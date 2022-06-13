package com.joshuaselbo.nonogram

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.terminal.Terminal
import kotlin.math.max
import kotlin.math.min

private enum class GameState {
    MENU,
    CONTROLS,
    PUZZLE,
    // Only used for testing
    KEYBOARD_DEBUG,
}

private class Puzzle(val name: String, val file: String)

class GameEngine(private val terminal: Terminal) {

    private val bindingReader = BindingReader(terminal.reader())
    private val menuKeyMap = KeyMap<Action>()
    private val puzzleKeyMap = KeyMap<Action>()

    init {
        menuKeyMap.bind(Action.UP, ANSI_UP, ANSI_UP_WIN)
        menuKeyMap.bind(Action.DOWN, ANSI_DOWN, ANSI_DOWN_WIN)
        menuKeyMap.bind(Action.CONFIRM, " ", ANSI_CR)

        puzzleKeyMap.bind(Action.UP, ANSI_UP, ANSI_UP_WIN)
        puzzleKeyMap.bind(Action.DOWN, ANSI_DOWN, ANSI_DOWN_WIN)
        puzzleKeyMap.bind(Action.RIGHT, ANSI_RIGHT, ANSI_RIGHT_WIN)
        puzzleKeyMap.bind(Action.LEFT, ANSI_LEFT, ANSI_LEFT_WIN)
        puzzleKeyMap.bind(Action.MARK_DOT, "x", "X")
        puzzleKeyMap.bind(Action.MARK_FILL, " ")
    }

    private val puzzles = listOf(
        Puzzle("Puzzle 1 (Easy)", "p1.txt"),
        Puzzle("Puzzle 2 (Easy)", "p2.txt"),
        Puzzle("Puzzle 3 (Easy)", "p3.txt")
    )

    private var gameState = GameState.MENU

    private var menuCursorIndex = 0
    private var selectedPuzzle: Puzzle? = null

    private var rowCursor = 0
    private var colCursor = 0

    fun gameLoop() {
        while (true) {
            doGameLoop()
        }
    }

    private fun doGameLoop() {
        val writer = terminal.writer()
        when (gameState) {
            GameState.MENU -> {
                writer.run {
                    var menu = "$ANSI_CLEAR\n"
                    for ((i, puzzle) in puzzles.withIndex()) {
                        menu += if (i == menuCursorIndex) {
                            "> "
                        } else {
                            "  "
                        }
                        menu += puzzle.name + "\n"
                    }
                    menu += "\n"
                    println(menu)
                    flush()
                }

                when (bindingReader.readBinding(menuKeyMap)) {
                    Action.UP -> menuCursorIndex = max(menuCursorIndex-1, 0)
                    Action.DOWN -> menuCursorIndex = min(puzzles.size-1, menuCursorIndex+1)
                    Action.CONFIRM -> {
                        gameState = GameState.CONTROLS
                        selectedPuzzle = puzzles[menuCursorIndex]
                    }
                    else -> {}
                }
            }
            GameState.CONTROLS -> {
                writer.run {
                    println(
                        """
                        == Nonogram Puzzle ==
                        
                        $ANSI_BOLD Controls$ANSI_RESET
                        
                          - Up, Down, Right, Left arrow keys
                          - Space -> Mark as filled
                          - x     -> Mark as empty
                          Press any key to continue...
                    """.trimIndent()
                    )
                    flush()
                }

                terminal.reader().read()

                gameState = GameState.PUZZLE
            }
            GameState.PUZZLE -> {
                val puzzle = checkNotNull(selectedPuzzle)

                val board = loadBoard(puzzle.file)
                val boardFormat = BoardFormatter().format(board)

                writer.println(ANSI_CLEAR)
                writer.println(boardFormat.contents)

                writer.print(getTerminalCursorPos(boardFormat))

                var solved = false
                while (!solved) {
                    when (bindingReader.readBinding(puzzleKeyMap)) {
                        Action.UP -> {
                            rowCursor = max(0, rowCursor - 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.DOWN -> {
                            rowCursor = min(board.rows.size - 1, rowCursor + 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.LEFT -> {
                            colCursor = max(0, colCursor - 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.RIGHT -> {
                            colCursor = min(board.cols.size - 1, colCursor + 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.MARK_DOT -> {
                            val curr = board.states[colCursor][rowCursor]
                            val newCellState =
                                if (curr == CellState.DOT) CellState.EMPTY else CellState.DOT
                            board.states[colCursor][rowCursor] = newCellState
                            writer.print(newCellState.toFormatString() + ANSI_LEFT)
                        }
                        Action.MARK_FILL -> {
                            val curr = board.states[colCursor][rowCursor]
                            val newCellState =
                                if (curr == CellState.FILL) CellState.EMPTY else CellState.FILL
                            board.states[colCursor][rowCursor] = newCellState
                            writer.print(newCellState.toFormatString() + ANSI_LEFT)
                        }
                        else -> {
                            // May be null on end of stream, if the program is already dying from ctrl+c
                        }
                    }

                    solved = isSolved(board)
                }

                writer.print(ansiCursorPosition(boardFormat.endRowIndex, 1))

                writer.run {
                    println("""
                        Puzzle Solved!!
                        
                        Press any key to continue...
                    """.trimIndent())
                    flush()
                }

                terminal.reader().read()

                gameState = GameState.MENU
            }
            GameState.KEYBOARD_DEBUG -> {
                writer.println("== Keyboard Debug Mode ==")

                val input = terminal.reader().read()
                writer.println("read: %d / 0x00%x".format(input, input))
                writer.flush()
            }
        }
    }

    private fun getTerminalCursorPos(formatterResult: BoardFormat): String {
        val row = formatterResult.startRowIndex + rowCursor
        val col = formatterResult.startColIndex + colCursor*2 // account for "|" separator
        return ansiCursorPosition(row, col)
    }

    // TODO support "0" block length
    private fun isSolved(board: Board): Boolean {
        for (colI in 0 until board.cols.size) {
            val col = board.cols[colI].toMutableList()
            var rowI = 0
            var blockCount = 0
            while (col.isNotEmpty()) {
                if (rowI >= board.rows.size) {
                    return false
                }
                if (board.states[colI][rowI] == CellState.FILL) {
                    blockCount++
                }
                if (blockCount == col.first()) {
                    col.removeFirst()
                    blockCount = 0
                }
                rowI++
            }
            while (rowI < board.rows.size) {
                if (board.states[colI][rowI] == CellState.FILL) {
                    return false
                }
                rowI++
            }
        }
        for (rowI in 0 until board.rows.size) {
            val row = board.rows[rowI].toMutableList()
            var colI = 0
            var blockCount = 0
            while (row.isNotEmpty()) {
                if (colI >= board.cols.size) {
                    return false
                }
                if (board.states[colI][rowI] == CellState.FILL) {
                    blockCount++
                }
                if (blockCount == row.first()) {
                    row.removeFirst()
                    blockCount = 0
                }
                colI++
            }
            while (colI < board.cols.size) {
                if (board.states[colI][rowI] == CellState.FILL) {
                    return false
                }
                colI++
            }
        }
        return true
    }

}