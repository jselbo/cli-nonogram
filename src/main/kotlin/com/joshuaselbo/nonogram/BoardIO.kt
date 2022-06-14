package com.joshuaselbo.nonogram

const val SUPPORTED_VERSION = 1
const val COL_ROW_SEPARATOR = "="

class DummyClass

fun loadBoard(filename: String): Board {
    val reader = DummyClass::class.java.classLoader.getResourceAsStream(filename).bufferedReader()
    val version = reader.readLine().trim('v', 'V').toInt()
    if (version > SUPPORTED_VERSION) {
        error("Unsupported file version: $version")
    }
    val lines = reader.readLines()
    val columns = mutableListOf<List<Int>>()
    val rows = mutableListOf<List<Int>>()
    var readingRows = false
    for (line in lines) {
        if (line == COL_ROW_SEPARATOR) {
            readingRows = true
        } else {
            val blockStrs = line.split(" ")

            val blocks =
                try {
                    blockStrs.map(String::toInt)
                } catch (e: NumberFormatException) {
                    error("Invalid block definition: '$line'")
                }

            for (block in blocks) {
                if (block < 0) {
                    error("Invalid block value: '$block'")
                }
            }
            if (blocks.any { it == 0 } && blocks.size > 1) {
                error("If there is a 'zero' block, it must be the only block in the row or column.")
            }

            val list = if (readingRows) rows else columns
            list.add(blocks)
        }
    }
    if (columns.isEmpty()) {
        error("No columns")
    }
    if (rows.isEmpty()) {
        error("No rows")
    }
    return Board(columns, rows)
}