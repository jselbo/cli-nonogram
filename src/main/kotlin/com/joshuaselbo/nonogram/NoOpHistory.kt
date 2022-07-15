package com.joshuaselbo.nonogram

import org.jline.reader.History
import org.jline.reader.LineReader
import java.nio.file.Path
import java.time.Instant

class NoOpHistory : History {
    override fun iterator(index: Int): MutableListIterator<History.Entry> =
        mutableListOf<History.Entry>().listIterator()

    override fun attach(reader: LineReader?) {}

    override fun load() {}

    override fun save() {}

    override fun write(file: Path?, incremental: Boolean) {}

    override fun append(file: Path?, incremental: Boolean) {}

    override fun read(file: Path?, checkDuplicates: Boolean) {}

    override fun purge() {}

    override fun size(): Int = 0

    override fun index(): Int = 0

    override fun first(): Int = 0

    override fun last(): Int = 0

    override fun get(index: Int): String = ""

    override fun add(time: Instant?, line: String?) {}

    override fun current(): String = ""

    override fun previous(): Boolean = false

    override fun next(): Boolean = false

    override fun moveToFirst(): Boolean = false

    override fun moveToLast(): Boolean = false

    override fun moveTo(index: Int): Boolean = false

    override fun moveToEnd() {}

    override fun resetIndex() {}
}