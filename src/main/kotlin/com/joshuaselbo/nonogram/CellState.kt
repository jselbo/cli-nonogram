package com.joshuaselbo.nonogram

enum class CellState {
    EMPTY, DOT, FILL;

    fun toFormatString(): String {
        return when (this) {
            EMPTY -> " "
            DOT -> "·"
            FILL -> "■"
        }
    }
}