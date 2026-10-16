plugins {
    application
}

group = "lv.sknarovs"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.telegrambots.longpolling)
    implementation(libs.telegrambots.client)
    implementation(libs.sqlite.jdbc)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// sqlite-jdbc loads a native library
val jvmFlags = listOf("--enable-native-access=ALL-UNNAMED")

application {
    mainClass = "lv.sknarovs.lvgmcbot.Main"
    applicationDefaultJvmArgs = jvmFlags
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(jvmFlags)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}
