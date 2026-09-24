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

        // Direccion del backend. 10.0.2.2 es el computador visto desde el emulador de Android.
        // Cuando el backend este desplegado, el buildType release apuntara a su URL publica.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")

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
