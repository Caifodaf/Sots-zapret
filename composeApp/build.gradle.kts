// Лицензия: MIT License. См. файл LICENSE в корне проекта.
// Проект использует Flowseal (https://github.com/Flowseal/zapret-discord-youtube, MIT), bol-van (2016-2025),
// а также WinDivert (https://github.com/basil00/WinDivert, LGPLv3/GPLv2). Подробности и условия — в LICENSE.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

val appVersionFromNamespace: String by lazy {
    val namespaceFile = file("src/commonMain/kotlin/util/path/NamespaceProject.kt")
    val regex = """const val APP_VERSION\s*=\s*"([^"]+)"""".toRegex()
    val content = namespaceFile.readText()
    regex.find(content)?.groupValues?.get(1)
        ?: error("APP_VERSION not found in NamespaceProject.kt")
}

val generateApiSecrets = tasks.register("generateApiSecrets") {
    val secretsFile = rootProject.file("secrets.properties")
    val exampleFile = rootProject.file("secrets.properties.example")
    val inputFile = if (secretsFile.exists()) secretsFile else exampleFile
    val props = Properties().apply { inputFile.inputStream().use { load(it) } }

    fun escapeForKotlin(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    val outputDir = layout.buildDirectory.dir("generated/sources/apiSecrets/kotlin").get().asFile
    val outputFile = File(outputDir, "util/path/ApiSecrets.kt")
    outputFile.parentFile.mkdirs()
    outputFile.writeText("""
package util.path

internal object ApiSecrets {
    const val API_BASE_URL: String = "${escapeForKotlin(props.getProperty("API_BASE_URL", ""))}"
    const val API_BASE_KEY: String = "${escapeForKotlin(props.getProperty("API_BASE_KEY", ""))}"
    const val SUPABASE_API_ZIP_URL: String = "${escapeForKotlin(props.getProperty("SUPABASE_API_ZIP_URL", ""))}"
}
""".trimIndent())
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/sources/apiSecrets/kotlin"))
        }
        val desktopMain by getting {
            resources.srcDir("resources")
            resources.srcDir("../../../../LICENSE.txt")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose)

            implementation(libs.kotlinx.dataframe)

            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kdroidfilter.composenativetray)
        }
    }
}

tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(generateApiSecrets)
}

compose.desktop {
    application {
        mainClass = "org.cmdtype.sots.MainKt"
        nativeDistributions {
            includeAllModules = true
            modules("java.sql")
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Sots"
            packageVersion = appVersionFromNamespace
            vendor  = "cmd.type"
            copyright = "© 2025 cmd.type. All rights reserved."
            licenseFile.set(project.file("../LICENSE.txt"))
            windows {
                iconFile.set(project.file("src/desktopMain/resources/logo.ico"))
                shortcut = true
                upgradeUuid = "DF7086B5-B450-4338-B662-32DE02CF4092"
            }
        }
    }
}



