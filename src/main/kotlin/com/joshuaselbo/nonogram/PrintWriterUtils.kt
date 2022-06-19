package com.joshuaselbo.nonogram

import java.io.PrintWriter

// I found that print on Mac always requires manual flush, so these extensions are for convenience.

fun PrintWriter.fprint(s: String) {
    print(s)
    flush()
}

fun PrintWriter.fprintln() {
    println()
    flush()
}

fun PrintWriter.fprintln(s: String) {
    println(s)
    flush()
}
