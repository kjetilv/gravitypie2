import org.gradle.internal.impldep.org.apache.http.client.methods.RequestBuilder.options
import org.openjfx.gradle.JavaFXPlatform

plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
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

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}