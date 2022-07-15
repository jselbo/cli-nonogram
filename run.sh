#!/bin/bash

set -e

./gradlew assemble

CLASSPATH=$(./gradlew -q printClasspath)

java -cp "$CLASSPATH" MainKt
