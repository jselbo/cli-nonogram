package com.joshuaselbo.nonogram

const val ANSI_CLEAR = "\u001B[2J\u001B[H"
const val ANSI_RESET = "\u001B[0m"
const val ANSI_BLACK = "\u001B[30m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_LIGHT_YELLOW = "\u001B[93m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_YELLOW_BACKGROUND = "\u001B[43m"
const val ANSI_BLUE = "\u001B[34m"
const val ANSI_PURPLE = "\u001B[35m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_WHITE = "\u001B[37m"
const val ANSI_BOLD = "\u001B[1m"
const val ANSI_UNBOLD = "\u001B[21m"
const val ANSI_UNDERLINE = "\u001B[4m"
const val ANSI_STOP_UNDERLINE = "\u001B[24m"
const val ANSI_BLINK = "\u001B[5m"

const val ANSI_CR = "\u000D"
const val ANSI_ESC = "\u001B"

const val ANSI_UP = "\u001B[A"
const val ANSI_DOWN = "\u001B[B"
const val ANSI_RIGHT = "\u001B[C"
const val ANSI_LEFT = "\u001B[D"

// On Windows CMD I observed different values for arrow keys
const val ANSI_UP_WIN = "\u001BOA"
const val ANSI_DOWN_WIN = "\u001BOB"
const val ANSI_RIGHT_WIN = "\u001BOC"
const val ANSI_LEFT_WIN = "\u001BOD"

fun ansiCursorPosition(line: Int, col: Int): String = "\u001B[${line};${col}H"

// Source: https://terminalguide.namepad.de/seq/csi_sq_t_space/
enum class CursorStyle(val code: Int) {
    DEFAULT(0),
    BLINKING_BLOCK(1),
    STEADY_BLOCK(2),
    BLINKING_UNDERLINE(3),
    STEADY_UNDERLINE(4),
    BLINKING_BAR(5),
    STEADY_BAR(6),
}
fun ansiCursorStyle(style: CursorStyle): String = "\u001B[${style.code} q"
