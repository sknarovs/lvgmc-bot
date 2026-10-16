# The jar is platform independent, so build it once on the build host even for multi-arch images.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /src

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN ./gradlew --no-daemon test installDist

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /src/build/install/lvgmc-bot/lib /app/lib

CMD ["java", "-XX:+UseSerialGC", "--enable-native-access=ALL-UNNAMED", "-cp", "/app/lib/*", "lv.meteobot.Main"]
