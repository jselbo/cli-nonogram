# cli-Nonogram

### Overview

Terminal-based nonogram puzzle game with 7 included puzzles and support for custom puzzles.

![Demo](https://github.com/jselbo/cli-nonogram/blob/master/demo.gif?raw=true "Demo")

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
- Ubuntu
  - ✔ Terminal (Known issue: filled cells render too wide)

Uses [JLine](https://github.com/jline/jline3) with [Jansi](https://github.com/fusesource/jansi) for cross-platform terminal support.

Please create an issue if you run into trouble.
