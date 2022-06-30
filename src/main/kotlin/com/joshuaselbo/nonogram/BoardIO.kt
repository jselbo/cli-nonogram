package com.joshuaselbo.nonogram

import java.nio.file.Path
import java.util.*
import kotlin.io.path.readBytes

const val SUPPORTED_VERSION = 1
const val COL_ROW_SEPARATOR = "="

class DummyClass

fun loadBoardFromFile(path: Path): Board {
    return loadBoard(path.readBytes())
}

fun loadBoardFromResource(filename: String): Board {
    val stream = DummyClass::class.java.classLoader.getResourceAsStream(filename)
    return loadBoard(stream.readBytes())
}

fun serialize(board: Board): String {
    var contents = "V$SUPPORTED_VERSION\n${board.name}\n"
    for (colBlocks in board.countColumnBlocks()) {
        contents += formatBlocks(colBlocks).joinToString(" ") + "\n"
    }
    contents += "$COL_ROW_SEPARATOR\n"
    for (rowBlocks in board.countRowBlocks()) {
        contents += formatBlocks(rowBlocks).joinToString(" ") + "\n"
    }
    return Base64.getEncoder().encodeToString(contents.toByteArray())
}

private fun loadBoard(input: ByteArray): Board {
    val decoded = Base64.getDecoder().decode(input)
    val lines = decoded.decodeToString().split("\n")
    if (lines.size < 2) {
        throw RuntimeException("Invalid puzzle")
    }

    val version = lines[0].trim('v', 'V').toInt()
    if (version > SUPPORTED_VERSION) {
        error("Unsupported file version: $version")
    }

    val name = lines[1]

    val columns = mutableListOf<List<Int>>()
    val rows = mutableListOf<List<Int>>()
    var readingRows = false
    for (line in lines.subList(2, lines.size)) {
        if (line.isEmpty()) continue

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
    return SolvableBoard(name, columns, rows)
}

private fun formatBlocks(blocks: List<Int>): List<Int> =
    blocks.ifEmpty { SolvableBoard.emptyBlockIndicator }
