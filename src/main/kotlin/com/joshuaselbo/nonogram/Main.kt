package com.joshuaselbo.nonogram

import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.notExists
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size > 1) {
        System.err.println("Usage: ${getScriptName()} <custom-puzzle-file>")
        exitProcess(1)
    }
    val customPuzzlePath: Path?
    if (args.isNotEmpty()) {
        customPuzzlePath = Path(args[0])
        if (customPuzzlePath.notExists()) {
            System.err.println("File '$customPuzzlePath' does not exist")
            exitProcess(1)
        }
    } else {
        customPuzzlePath = null
    }

    // Enable jline debug logging
    Logger.getLogger("org.jline").level = Level.FINE

    val terminal: Terminal
    try {
        terminal = TerminalBuilder.builder()
            .jna(false)
            .jansi(true)
            .dumb(false)
            .build()
    } catch (e: RuntimeException) {
        System.err.println("Error: '${e.message}'")
        System.err.println("Failed to configure terminal. Are you using the run script?" +
                " See README for instructions.")
        exitProcess(1)
    }

    terminal.enterRawMode()

    // On Windows it seems interrupt (ctrl+c) needs to be handled manually
    terminal.handle(Terminal.Signal.INT) {
        // Clear terminal first; otherwise shell resumes wherever cursor was and looks bad
        terminal.writer().println(ANSI_CLEAR)

        exitProcess(0)
    }

    val gameEngine = GameEngine(terminal)
    if (customPuzzlePath != null) {
        gameEngine.selectCustomPuzzle(customPuzzlePath)
    }
    gameEngine.gameLoop()
}

private fun getScriptName(): String {
    return if (isWindows()) "run.bat" else "run.sh"
}

private fun isWindows(): Boolean {
    val os = System.getProperty("os.name")
    return os.startsWith("Windows")
}
