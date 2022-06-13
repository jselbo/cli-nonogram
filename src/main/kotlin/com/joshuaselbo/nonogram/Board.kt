package com.joshuaselbo.nonogram

class Board(val cols: List<List<Int>>, val rows: List<List<Int>>) {

    val states = Array(cols.size) {
        Array(rows.size) { CellState.EMPTY }
    }

    override fun toString(): String {
        return "Board{cols=$cols, rows=$rows}"
    }
}