#!/usr/bin/env sh
# Gradle wrapper script
gradle 8.7 "$@" || ./gradle/wrapper/gradle-wrapper.jar || gradle "$@"
