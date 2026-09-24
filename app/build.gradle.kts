import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ue.edu.co.splitbill"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ue.edu.co.splitbill"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Direccion del backend. Por defecto 10.0.2.2, que es el computador visto desde el emulador.
        // Cada quien puede cambiarla en su local.properties (no se sube a git), por ejemplo para un
        // celular fisico: splitbill.apiBaseUrl=http://localhost:8080/ junto con adb reverse tcp:8080 tcp:8080.
        // Cuando el backend este desplegado, el buildType release apuntara a su URL publica.
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        val apiBaseUrl = localProperties.getProperty("splitbill.apiBaseUrl", "http://10.0.2.2:8080/")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // Exporta el esquema de Room a app/schemas para poder revisar el SQL generado
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }

    // Las pruebas de migracion leen los esquemas exportados por Room (schemas/1.json, 2.json...)
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Persistencia local: Room es una capa sobre SQLite, el SQL sigue escrito a mano en los @Dao
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Red: Retrofit arma las peticiones HTTP a partir de una interfaz y Gson convierte el JSON
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
