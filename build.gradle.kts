plugins {
    application
}

group = "lv.sknarovs"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots-longpolling:10.3.0")
    implementation("org.telegram:telegrambots-client:10.3.0")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.3")
    implementation("ch.qos.logback:logback-classic:1.6.5")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
