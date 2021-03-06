package com.joshuaselbo.nonogram

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText
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

    private val controlsMessage = """
        ${ANSI_BOLD}Controls$ANSI_RESET
        
        - Arrow keys  -> Move
        - Space       -> Mark filled
        - X           -> Mark empty
        - Q           -> Quit/Pause
        """.trimIndent()

    private val bindingReader = BindingReader(terminal.reader())
    private val menuKeyMap = KeyMap<Action>()
    private val puzzleKeyMap = KeyMap<Action>()
    private val puzzleMenuKeyMap = KeyMap<PuzzleMenuOption>()
    private val puzzleCreatorMenuKeyMap = KeyMap<PuzzleMenuOption>()
    private val menuEntries = listOf(
        MenuEntry("Puzzle 1 (5x5)", ResourcePuzzleIdentifier("p1.txt")),
        MenuEntry("Puzzle 2 (5x5)", ResourcePuzzleIdentifier("p2.txt")),
        MenuEntry("Puzzle 3 (5x5)", ResourcePuzzleIdentifier("p3.txt")),
        MenuEntry("Puzzle 4 (10x10)", ResourcePuzzleIdentifier("p4.txt")),
        MenuEntry("Puzzle 5 (10x10)", ResourcePuzzleIdentifier("p5.txt")),
        MenuEntry("Puzzle 6 (10x10)", ResourcePuzzleIdentifier("p6.txt")),
        MenuEntry("Puzzle 7 (10x10)", ResourcePuzzleIdentifier("p7.txt")),
        //MenuEntry("Debug Puzzle", ResourcePuzzleIdentifier("debug.txt")),
        MenuEntry("Puzzle Creator", PuzzleCreatorDestination)
    )
    private val lineReader =
        LineReaderBuilder.builder()
            .terminal(terminal)
            .history(NoOpHistory())
            .build()

    private var gameState = GameState.MENU

    private var menuCursorIndex = 0
    private var selectedPuzzle: PuzzleIdentifier? = null
    private var loadedBoard: Board? = null
    private var exitAfterSolve = false

    private var rowCursor = 0
    private var colCursor = 0

    init {
        menuKeyMap.bind(Action.UP, ANSI_UP, ANSI_UP_WIN)
        menuKeyMap.bind(Action.DOWN, ANSI_DOWN, ANSI_DOWN_WIN)
        menuKeyMap.bind(Action.CONFIRM, " ", ANSI_CR)
        menuKeyMap.bind(Action.END, "q", "Q")

        puzzleKeyMap.bind(Action.UP, ANSI_UP, ANSI_UP_WIN)
        puzzleKeyMap.bind(Action.DOWN, ANSI_DOWN, ANSI_DOWN_WIN)
        puzzleKeyMap.bind(Action.RIGHT, ANSI_RIGHT, ANSI_RIGHT_WIN)
        puzzleKeyMap.bind(Action.LEFT, ANSI_LEFT, ANSI_LEFT_WIN)
        puzzleKeyMap.bind(Action.MARK_DOT, "x", "X")
        puzzleKeyMap.bind(Action.MARK_FILL, " ")
        puzzleKeyMap.bind(Action.END, "q", "Q")

        puzzleMenuKeyMap.bind(PuzzleMenuOption.EDIT, "z", "Z")
        puzzleMenuKeyMap.bind(PuzzleMenuOption.MENU, "m", "M")

        puzzleCreatorMenuKeyMap.bind(PuzzleMenuOption.COPY, "c", "C")
        puzzleCreatorMenuKeyMap.bind(PuzzleMenuOption.WRITE, "f", "F")
        puzzleCreatorMenuKeyMap.bind(PuzzleMenuOption.EDIT, "z", "Z")
        puzzleCreatorMenuKeyMap.bind(PuzzleMenuOption.MENU, "m", "M")
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

    fun cleanupAndExit() {
        // Reset cursor style
        terminal.writer().fprintln(ansiCursorStyle(CursorStyle.DEFAULT))
        exitProcess(0)
    }

    private fun doGameLoop() {
        val writer = terminal.writer()
        when (gameState) {
            GameState.MENU -> {
                clearTerminal()
                var menu = ""
                for ((i, puzzle) in menuEntries.withIndex()) {
                    menu += if (i == menuCursorIndex) {
                        "> "
                    } else {
                        "  "
                    }
                    menu += puzzle.name + "\n"
                }
                menu += "\nPress 'Q' or ^C to quit."
                writer.fprintln(menu)

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
                    Action.END -> {
                        cleanupAndExit()
                    }
                    else -> {}
                }
            }
            GameState.CONTROLS -> {
                writer.fprintln(controlsMessage + "\n\n" + CONTINUE_MESSAGE)

                terminal.reader().read()

                gameState = GameState.PUZZLE
            }
            GameState.PUZZLE, GameState.PUZZLE_CREATOR_INTERACTIVE -> {
                val puzzle = checkNotNull(selectedPuzzle)

                val board = loadedBoard ?: puzzle.load()
                loadedBoard = board
                val boardFormat = BoardFormatter().format(board)

                rowCursor = 0
                colCursor = 0

                clearTerminal()
                writer.fprintln(boardFormat.contents)

                writer.fprint(getTerminalCursorPos(boardFormat))

                var solved = false
                var paused = false
                while (!solved && !paused) {
                    when (bindingReader.readBinding(puzzleKeyMap)) {
                        Action.UP -> {
                            rowCursor = max(0, rowCursor - 1)
                            writer.fprint(getTerminalCursorPos(boardFormat))
                        }
                        Action.DOWN -> {
                            rowCursor = min(board.rowSize - 1, rowCursor + 1)
                            writer.fprint(getTerminalCursorPos(boardFormat))
                        }
                        Action.LEFT -> {
                            colCursor = max(0, colCursor - 1)
                            writer.fprint(getTerminalCursorPos(boardFormat))
                        }
                        Action.RIGHT -> {
                            colCursor = min(board.columnSize - 1, colCursor + 1)
                            writer.fprint(getTerminalCursorPos(boardFormat))
                        }
                        Action.MARK_DOT -> {
                            val curr = board.states[colCursor][rowCursor]
                            val newCellState =
                                if (curr == CellState.DOT) CellState.EMPTY else CellState.DOT
                            board.states[colCursor][rowCursor] = newCellState
                            writer.fprint(newCellState.toFormatString() + ANSI_LEFT)
                        }
                        Action.MARK_FILL -> {
                            val curr = board.states[colCursor][rowCursor]
                            val newCellState =
                                if (curr == CellState.FILL) CellState.EMPTY else CellState.FILL
                            board.states[colCursor][rowCursor] = newCellState
                            writer.fprint(newCellState.toFormatString() + ANSI_LEFT)
                        }
                        Action.END -> {
                            paused = true
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

                writer.fprint(getEndTerminalCursorPos(boardFormat))

                if (solved) {
                    writer.fprintln("""
                        ${ANSI_BOLD}${ANSI_GREEN}Puzzle Solved!$ANSI_RESET
                        
                        ${ANSI_BOLD}${ANSI_CYAN}~~~ ${board.name} ~~~$ANSI_RESET
                        
                        $CONTINUE_MESSAGE
                        """.trimIndent())

                    terminal.reader().read()

                    if (exitAfterSolve) {
                        cleanupAndExit()
                    }

                    loadedBoard = null
                    gameState = GameState.MENU
                } else {
                    when (gameState) {
                        GameState.PUZZLE -> {
                            writer.fprintln("""
                                ${ANSI_BOLD}Paused$ANSI_RESET
                                
                                - Press Z to continue solving
                                - Press M to return to menu
                            """.trimIndent())

                            when (bindingReader.readBinding(puzzleMenuKeyMap)) {
                                PuzzleMenuOption.EDIT -> Unit
                                PuzzleMenuOption.MENU -> {
                                    loadedBoard = null
                                    gameState = GameState.MENU
                                }
                                else -> {}
                            }
                        }
                        GameState.PUZZLE_CREATOR_INTERACTIVE -> {
                            writer.fprintln("""
                                ${ANSI_BOLD}Paused$ANSI_RESET
                                
                                - Press C to copy puzzle format to your clipboard
                                - Press F to write puzzle format to a file
                                - Press Z to continue editing
                                - Press M to return to menu
                            """.trimIndent())

                            when (bindingReader.readBinding(puzzleCreatorMenuKeyMap)) {
                                PuzzleMenuOption.COPY -> {
                                    Toolkit.getDefaultToolkit()
                                        .systemClipboard
                                        .setContents(
                                            StringSelection(serialize(board)),
                                            null
                                        )

                                    writer.fprintln("""
                                        ${ANSI_GREEN}Copied to clipboard!$ANSI_RESET
                                        
                                        $CONTINUE_MESSAGE
                                        """.trimIndent())
                                    terminal.reader().read()
                                }
                                PuzzleMenuOption.WRITE -> {
                                    var written = false
                                    while (!written) {
                                        val input = lineReader.readLine("Enter file name: ")
                                        if (input.isEmpty()) {
                                            continue
                                        }
                                        val path = Path(input)
                                        try {
                                            path.writeText(serialize(board))
                                            written = true
                                            writer.fprintln(
                                                "${ANSI_GREEN}Success!$ANSI_RESET\n\n$CONTINUE_MESSAGE")
                                            terminal.reader().read()
                                        } catch (e: IOException) {
                                            writer.fprintln("Error writing file: '${e.message}'")
                                        }
                                    }
                                }
                                PuzzleMenuOption.EDIT -> Unit
                                PuzzleMenuOption.MENU -> {
                                    loadedBoard = null
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
                clearTerminal()
                writer.fprintln("${ANSI_BOLD}Puzzle Creator$ANSI_RESET\n\n")

                var name: String? = null
                while (name == null) {
                    name = tryReadString("Puzzle name: ")
                }

                var numColumns: Int? = null
                while (numColumns == null) {
                    numColumns = tryReadInt("Number of columns (width), 1-20: ", 1..PUZZLE_CREATOR_MAX_DIMEN)
                }

                var numRows: Int? = null
                while (numRows == null) {
                    numRows = tryReadInt("Number of rows (height), 1-20: ", 1..PUZZLE_CREATOR_MAX_DIMEN)
                }

                writer.fprintln()
                writer.fprintln(controlsMessage + "\n\n" + CONTINUE_MESSAGE)
                terminal.reader().read()

                val board = PuzzleCreatorBoard(name, numColumns, numRows)
                selectedPuzzle = AlreadyLoadedPuzzleIdentifier(board)
                gameState = GameState.PUZZLE_CREATOR_INTERACTIVE
            }
            GameState.KEYBOARD_DEBUG -> {
                writer.fprintln("== Keyboard Debug Mode ==")

                val input = terminal.reader().read()
                writer.fprintln("read: %d / 0x00%x".format(input, input))
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

    private fun tryReadInt(prompt: String, acceptedRange: IntRange): Int? {
        val input = lineReader.readLine(prompt)
        val inputAsInt =
            try {
                input.toInt()
            } catch (e: NumberFormatException) {
                return null
            }
        return if (inputAsInt in acceptedRange) { inputAsInt } else null
    }

    private fun tryReadString(prompt: String): String? {
        val input = lineReader.readLine(prompt).trim()
        return input.ifEmpty { null }
    }

    private fun clearTerminal() {
        terminal.writer().fprintln(ANSI_CLEAR)
    }
}
