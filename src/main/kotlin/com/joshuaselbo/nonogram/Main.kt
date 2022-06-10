package com.joshuaselbo.nonogram

import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

fun main() {
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
        exitProcess(0)
    }

    val gameEngine = GameEngine(terminal)
    gameEngine.gameLoop()
}
