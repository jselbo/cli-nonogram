# cli-Nonogram

### Overview

Terminal-based nonogram puzzle game with 10 included puzzles and custom puzzle support.

TODO add gif

For more information on nonogram puzzles: https://en.wikipedia.org/wiki/Nonogram

### Prerequisites

- Java (JDK) installed and on path (Check with `javac -version`)

### How to Run

- Open Terminal or Command Prompt
- Run script:
  - Windows: `run.bat`
  - Mac/Linux: `./run.sh`

To load a custom puzzle from file:
- Windows: `run.bat <filename>`
- Mac/Linux: `./run.sh <filename>`

### Technical Details

Tested on:
- Windows
  - ✔ Command Prompt (cmd.exe)
  - ✔ Powershell
- Mac
  - ✔ Terminal 
  - ✔ iTerm

Uses [JLine](https://github.com/jline/jline3) with [Jansi](https://github.com/fusesource/jansi) to read raw key presses from terminal across platforms.

Please create an issue if you run into trouble.
