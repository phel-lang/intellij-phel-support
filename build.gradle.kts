import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "org.phellang"
version = "0.1.4"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
        kotlin {
            srcDirs("src/main/kotlin", "src/main/gen")
        }
    }
    test {
        kotlin {
            srcDirs("src/test/kotlin")
        }
    }
}

// Configure IntelliJ Platform Dependencies
dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2.5")
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}

val generatePhelLexer = tasks.register<GenerateLexerTask>("generatePhelLexer") {
    sourceFile.set(file("src/main/kotlin/org/phellang/language/Phel.flex"))
    targetOutputDir.set(file("src/main/gen/org/phellang/language/"))
    purgeOldFiles.set(true)
}

val generatePhelParser = tasks.register<GenerateParserTask>("generatePhelParser") {
    sourceFile.set(file("src/main/kotlin/org/phellang/language/Phel.bnf"))
    targetRootOutputDir.set(file("src/main/gen"))
    pathToParser.set("org/phellang/language/parser/PhelParser.java")
    pathToPsiRoot.set("org/phellang/language/psi")
    purgeOldFiles.set(true)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        options.release.set(21)
        dependsOn(generatePhelLexer, generatePhelParser)
    }

    // Configure Kotlin compilation for main sources
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
            languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
            apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
        }
        dependsOn(generatePhelLexer, generatePhelParser)
    }

    intellijPlatform {
        buildSearchableOptions {
            enabled = false
        }

        patchPluginXml {
            sinceBuild.set("242")
            untilBuild.set("252.*")
        }

        signPlugin {
            certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
            privateKey.set(System.getenv("PRIVATE_KEY"))
            password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
        }

        publishPlugin {
            token.set(System.getenv("PUBLISH_TOKEN"))
        }
    }
}
