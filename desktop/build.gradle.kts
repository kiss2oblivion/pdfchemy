import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose Multiplatform Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Apache PDFBox for pure JVM Desktop (Windows & Linux)
    implementation("org.apache.pdfbox:pdfbox:2.0.31")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // Document & Conversion Libraries (Pure JVM)
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.17.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.0")

    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.pdfchemy.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )
            packageName = "PDFchemy"
            packageVersion = "1.0.1"
            description = "PDFchemy Tools - Local-First Offline PDF Utility"
            copyright = "© 2026 Andrei Ioan Cucos. All rights reserved."
            vendor = "PDFchemy"

            windows {
                menuGroup = "PDFchemy"
                upgradeUuid = "8a2f07d2-a7d0-4cb5-8d59-2fce4d15f129"
                shortcut = true
                iconFile.set(project.file("src/main/resources/icons/windows/icon.ico"))
            }

            linux {
                shortcut = true
                packageName = "pdfchemy"
                appCategory = "Office;Utility;"
                menuGroup = "Office"
                iconFile.set(project.file("src/main/resources/icons/linux/icon.png"))
            }
        }
    }
}
