import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask

plugins {
    id("java")
    id("com.strumenta.antlr-kotlin") version "1.0.0"
    kotlin("jvm")
    application
}

group = "edu.stella.checker"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.strumenta:antlr-kotlin-runtime:1.0.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

val generateStellaGrammarSource = tasks.register<AntlrKotlinTask>("generateStellaGrammarSource") {
    dependsOn("cleanGenerateStellaGrammarSource")
    group = "grammar"

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory.dir("antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "com.strumenta.antlrkotlin.parsers.generated"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}
kotlin {
    jvmToolchain(17)

    sourceSets {
        main {
            kotlin {
                srcDir(generateStellaGrammarSource)
            }
        }
    }
}

private val mainFQName = "edu.stella.MainKt"

application {
    mainClass = mainFQName
}

val run by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}