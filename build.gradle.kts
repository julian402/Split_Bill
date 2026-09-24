// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
}

// Si el proyecto esta dentro de una carpeta sincronizada (OneDrive), la sincronizacion bloquea los
// archivos de build mientras Gradle los escribe y la compilacion falla. Quien tenga ese problema
// puede mandar las salidas a otra carpeta con una linea en su local.properties (que no se sube a git):
//     splitbill.buildDir=C:/Temp/splitbill-build
// Sin esa linea, todo queda como siempre, en la carpeta build de cada modulo.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
localProperties.getProperty("splitbill.buildDir")?.let { buildRoot ->
    allprojects {
        layout.buildDirectory.set(file("$buildRoot/${project.name}"))
    }
}
