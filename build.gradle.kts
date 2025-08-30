plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("idea")
}

group = "com.github.kjetilv"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
    withSourcesJar()
    modularity.inferModulePath
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

application {
    mainClass.set("com.github.kjetilv.gravitypie2.Main")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=javafx.graphics"
    )
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

val javafxVersion = "24.0.2"

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.graphics")
}

dependencies {
    // Your app deps here

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configuration and task to download JavaFX sources so IDEs can attach Javadoc/source
configurations {
    create("javafxSources")
}

val javafxModules = listOf("javafx.controls", "javafx.graphics")

dependencies {
    // Add source artifacts for each JavaFX module and platform
    val os = org.gradle.internal.os.OperatingSystem.current()
    val platform = when {
        os.isWindows -> "win"
        os.isMacOsX -> "mac"
        else -> "linux"
    }
    javafxModules.forEach { mod ->
        add("javafxSources", "org.openjfx:$mod:$javafxVersion:$platform@jar") // binary (ensures platform classifier resolution)
        add("javafxSources", "org.openjfx:$mod:$javafxVersion:sources@jar") // sources
    }
}

// Task that resolves and downloads the JavaFX source jars into Gradle cache
tasks.register("downloadJavafxSources") {
    group = "help"
    description = "Resolves JavaFX sources so IDE can attach Javadoc/source."
    doLast {
        val cfg = configurations.getByName("javafxSources")
        cfg.resolve()
        println("JavaFX sources resolved for modules: ${javafxModules.joinToString()}")
    }
}

tasks.test {
    useJUnitPlatform()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}