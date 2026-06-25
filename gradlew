#!/bin/sh

APP_HOME=$(cd "${0%/*}" >/dev/null 2>&1 && pwd -P) || exit
exec java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
