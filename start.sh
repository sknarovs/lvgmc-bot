#!/bin/sh
cd "$(dirname "$0")"
./gradlew --quiet installDist && exec build/install/lvgmc-bot/bin/lvgmc-bot
