package com.joshuaselbo.nonogram

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

private const val CONTINUE_MESSAGE = "Press any key to continue..."

private const val PUZZLE_CREATOR_MAX_DIMEN = 20

private enum class GameState {
    MENU,
    CONTROLS,
    PUZZLE,
    PUZZLE_CREATOR_SETUP,
    PUZZLE_CREATOR_INTERACTIVE,
    // Only used for testing
    KEYBOARD_DEBUG,
}

private sealed interface MenuDestination

private interface PuzzleIdentifier {
    fun load(): Board
}
private class ResourcePuzzleIdentifier(val filename: String) : PuzzleIdentifier, MenuDestination {
    override fun load(): Board = loadBoardFromResource(filename)
}
private class FilePuzzleIdentifier(val path: Path) : PuzzleIdentifier {
    override fun load(): Board = loadBoardFromFile(path)
}
private class AlreadyLoadedPuzzleIdentifier(val board: Board) : PuzzleIdentifier {
    override fun load(): Board = board
}

private object PuzzleCreatorDestination : MenuDestination

private class MenuEntry(val name: String, val destination: MenuDestination)

class GameEngine(private val terminal: Terminal) {

    private val bindingReader = BindingReader(terminal.reader())
    private val menuKeyMap = KeyMap<Action>()
    private val puzzleKeyMap = KeyMap<Action>()
    private val puzzleCreatorOptionKeyMap = KeyMap<PuzzleCreatorOption>()
    private val menuEntries = listOf(
        MenuEntry("Puzzle 1 (Easy)", ResourcePuzzleIdentifier("p1.txt")),
        MenuEntry("Debug Puzzle", ResourcePuzzleIdentifier("debug.txt")),
        MenuEntry("Puzzle Creator", PuzzleCreatorDestination)
    )

    private var gameState = GameState.MENU

    private var menuCursorIndex = 0
    private var selectedPuzzle: PuzzleIdentifier? = null
    private var exitAfterSolve = false

    private var rowCursor = 0
    private var colCursor = 0

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
        // TODO maybe esc isn't good - there is unavoidable delay because of other escape sequences
        puzzleKeyMap.bind(Action.END, ANSI_ESC)

        puzzleCreatorOptionKeyMap.bind(PuzzleCreatorOption.COPY, "c", "C")
        puzzleCreatorOptionKeyMap.bind(PuzzleCreatorOption.WRITE, "f", "F")
        puzzleCreatorOptionKeyMap.bind(PuzzleCreatorOption.EDIT, "e", "E")
        puzzleCreatorOptionKeyMap.bind(PuzzleCreatorOption.MENU, "m", "M")
    }


    fun selectCustomPuzzle(path: Path) {
        selectedPuzzle = FilePuzzleIdentifier(path)
        gameState = GameState.CONTROLS
        exitAfterSolve = true
    }

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
                    for ((i, puzzle) in menuEntries.withIndex()) {
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
                    Action.DOWN -> menuCursorIndex = min(menuEntries.size-1, menuCursorIndex+1)
                    Action.CONFIRM -> {
                        when (val destination = menuEntries[menuCursorIndex].destination) {
                            is ResourcePuzzleIdentifier -> {
                                selectedPuzzle = destination
                                gameState = GameState.CONTROLS
                            }
                            is PuzzleCreatorDestination -> {
                                gameState = GameState.PUZZLE_CREATOR_SETUP
                            }
                        }
                    }
                    else -> {}
                }
            }
            GameState.CONTROLS -> {
                writer.run {
                    println(
                        """
                        $ANSI_BOLD Controls$ANSI_RESET
                        
                          - Up, Down, Right, Left -> Move
                          - Space                 -> Mark filled
                          - x                     -> Mark empty
                          
                          $CONTINUE_MESSAGE
                    """.trimIndent()
                    )
                    flush()
                }

                terminal.reader().read()

                gameState = GameState.PUZZLE
            }
            GameState.PUZZLE, GameState.PUZZLE_CREATOR_INTERACTIVE -> {
                val puzzle = checkNotNull(selectedPuzzle)

                val board = puzzle.load()
                val boardFormat = BoardFormatter().format(board)

                rowCursor = 0
                colCursor = 0

                writer.println(ANSI_CLEAR)
                writer.println(boardFormat.contents)

                writer.print(getTerminalCursorPos(boardFormat))

                var solved = false
                var ended = false
                while (!solved && !ended) {
                    when (bindingReader.readBinding(puzzleKeyMap)) {
                        Action.UP -> {
                            rowCursor = max(0, rowCursor - 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.DOWN -> {
                            rowCursor = min(board.rowSize - 1, rowCursor + 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.LEFT -> {
                            colCursor = max(0, colCursor - 1)
                            writer.print(getTerminalCursorPos(boardFormat))
                        }
                        Action.RIGHT -> {
                            colCursor = min(board.columnSize - 1, colCursor + 1)
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
                        Action.END -> {
                            ended = true
                        }
                        else -> {
                            // May be null on end of stream, if the program is already dying from ctrl+c
                        }
                    }

                    when (gameState) {
                        GameState.PUZZLE -> solved = (board as SolvableBoard).isSolved()
                        GameState.PUZZLE_CREATOR_INTERACTIVE -> Unit
                        else -> throw IllegalStateException()
                    }
                }

                writer.print(getEndTerminalCursorPos(boardFormat))

                if (solved) {
                    writer.println("""
                        ${ANSI_BOLD}${ANSI_GREEN}Puzzle Solved!$ANSI_RESET
                        
                        $CONTINUE_MESSAGE
                        """.trimIndent())

                    terminal.reader().read()

                    if (exitAfterSolve) {
                        exitProcess(0)
                    }

                    gameState = GameState.MENU
                } else {
                    when (gameState) {
                        // TODO maybe confirm pause before jumping back to menu
                        GameState.PUZZLE -> gameState = GameState.MENU
                        GameState.PUZZLE_CREATOR_INTERACTIVE -> {
                            writer.println("""
                                ${ANSI_BOLD}Editing Paused$ANSI_RESET
                                
                                - Press C to copy puzzle format to your clipboard
                                - Press F to write puzzle format to a file
                                - Press E to continue editing
                                - Press M to return to menu
                            """.trimIndent())

                            when (bindingReader.readBinding(puzzleCreatorOptionKeyMap)) {
                                PuzzleCreatorOption.COPY -> {
                                    Toolkit.getDefaultToolkit()
                                        .systemClipboard
                                        .setContents(
                                            StringSelection(serialize(board)),
                                            null
                                        )

                                    writer.println("""
                                        ${ANSI_GREEN}Copied to clipboard!$ANSI_RESET
                                        
                                        $CONTINUE_MESSAGE
                                        """.trimIndent())
                                    terminal.reader().read()
                                }
                                PuzzleCreatorOption.WRITE -> TODO()
                                PuzzleCreatorOption.EDIT -> Unit
                                PuzzleCreatorOption.MENU -> {
                                    gameState = GameState.MENU
                                }
                                else -> {}
                            }
                        }
                        else -> throw IllegalStateException()
                    }
                }
            }
            GameState.PUZZLE_CREATOR_SETUP -> {
                writer.println(ANSI_CLEAR)
                writer.println("${ANSI_BOLD}Puzzle Creator$ANSI_RESET\n\n")

                val lineReader = LineReaderBuilder.builder().terminal(terminal).build()

                var numColumns: Int? = null
                while (numColumns == null) {
                    writer.print("Number of columns (puzzle width): ")
                    numColumns = tryReadInt(lineReader, 1..PUZZLE_CREATOR_MAX_DIMEN)
                }

                var numRows: Int? = null
                while (numRows == null) {
                    writer.print("Number of rows (puzzle height): ")
                    numRows = tryReadInt(lineReader, 1..PUZZLE_CREATOR_MAX_DIMEN)
                }

                writer.println()
                writer.println("""
                    During creator mode, regular controls apply. Press "ESC" key to finish.
                    
                    $CONTINUE_MESSAGE
                """.trimIndent())
                terminal.reader().read()

                val board = PuzzleCreatorBoard(numColumns, numRows)
                selectedPuzzle = AlreadyLoadedPuzzleIdentifier(board)
                gameState = GameState.PUZZLE_CREATOR_INTERACTIVE
            }
            GameState.KEYBOARD_DEBUG -> {
                writer.println("== Keyboard Debug Mode ==")

                val input = terminal.reader().read()
                writer.println("read: %d / 0x00%x".format(input, input))
                writer.flush()
            }
        }
    }

    private fun getTerminalCursorPos(format: BoardFormat): String {
        val (row, col) = format.getCursorPosition(rowCursor, colCursor)
        return ansiCursorPosition(row, col)
    }

    private fun getEndTerminalCursorPos(format: BoardFormat): String {
        val (row, col) = format.getEndCursorPosition()
        return ansiCursorPosition(row, col)
    }

    private fun tryReadInt(lineReader: LineReader, acceptedRange: IntRange): Int? {
        val input = lineReader.readLine()
        val inputAsInt =
            try {
                input.toInt()
            } catch (e: NumberFormatException) {
                return null
            }
        return if (inputAsInt in acceptedRange) { inputAsInt } else null
    }

}
