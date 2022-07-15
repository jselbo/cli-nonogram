# cli-Nonogram

Simple terminal-based nonogram puzzle solver. Made in an afternoon.

Only one puzzle is included for now. :)

For more information on nonograms: https://en.wikipedia.org/wiki/Nonogram

Uses [JLine](https://github.com/jline/jline3) to read raw key presses from terminal.
(Only tested on MacOS with Terminal app)

Must be run from a native terminal (e.g. not IntelliJ terminal). It also doesn't work when running the binary with the gradle wrapper because gradle wraps the terminal session.

Example run command:

```
./gradlew assemble

kotlin -cp build/classes/kotlin/main:/Users/<username>/.gradle/caches/modules-2/files-2.1/org.jline/jline/3.21.0/<hash>/jline-3.21.0.jar MainKt
```
