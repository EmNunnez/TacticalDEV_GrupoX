import java.util.Properties
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

// ---------- Firma del APK (release) ----------
// Los datos de la llave se leen desde keystore.properties (raíz del proyecto).
// Ver docs/FIRMA_APK.md para generarla.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "cl.duoc.tacticaldev"
    compileSdk = 35

    defaultConfig {
        applicationId = "cl.duoc.tacticaldev"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val usuariosUrl = project.findProperty("API_USUARIOS_URL") as String? ?: "http://10.0.2.2:8080/"
        val operacionesUrl = project.findProperty("API_OPERACIONES_URL") as String? ?: "http://10.0.2.2:8080/"
        buildConfigField("String", "API_USUARIOS_URL", "\"$usuariosUrl\"")
        buildConfigField("String", "API_OPERACIONES_URL", "\"$operacionesUrl\"")
        buildConfigField("String", "API_CLIMA_URL", "\"https://api.open-meteo.com/\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Kotest corre sobre la plataforma JUnit 5
        unitTests.all { it.useJUnitPlatform() }
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/INDEX.LIST")
    }
}

// ---------- Cobertura de pruebas (Kover) ----------
// Reporte:   ./gradlew koverHtmlReportDebug   -> app/build/reports/kover/htmlDebug/index.html
// Verificar: ./gradlew koverVerifyDebug       (falla si el proyecto tiene < 80 % en líneas, ramas o instrucciones)
//
// Se mide el PROYECTO COMPLETO (data, repository, viewmodel, util, navegación...).
// Solo se excluye lo que no puede ejecutarse en una prueba unitaria de JVM:
//  - Interfaz Compose (@Composable): se prueba con Compose UI Test en src/androidTest.
//  - Clases que necesitan un dispositivo Android real (Activity, Application, Room, DataStore,
//    GPS, cámara/Bitmap).
//  - Código generado automáticamente (BuildConfig, R, implementaciones de Room).
kover {
    reports {
        filters {
            excludes {
                packages(
                    "cl.duoc.tacticaldev.ui.screens",
                    "cl.duoc.tacticaldev.ui.components",
                    "cl.duoc.tacticaldev.ui.theme"
                )
                classes(
                    // Interfaz Compose
                    "cl.duoc.tacticaldev.ui.VmFactoryKt*",
                    "cl.duoc.tacticaldev.ui.navigation.AppNavigationKt*",
                    "*ComposableSingletons*",
                    // Requieren dispositivo Android
                    "cl.duoc.tacticaldev.MainActivity*",
                    "cl.duoc.tacticaldev.TacticalApp*",
                    "cl.duoc.tacticaldev.AppContainer*",
                    "cl.duoc.tacticaldev.data.local.AppDatabase*",
                    "cl.duoc.tacticaldev.data.local.DataStoreSesionStore*",
                    "cl.duoc.tacticaldev.data.local.SesionStoreKt*",
                    "cl.duoc.tacticaldev.util.AndroidLocationProvider*",
                    "cl.duoc.tacticaldev.util.AndroidImageEncoder*",
                    "cl.duoc.tacticaldev.util.ImageUtils*",
                    // Generado por el compilador
                    "cl.duoc.tacticaldev.BuildConfig",
                    "cl.duoc.tacticaldev.R",
                    "cl.duoc.tacticaldev.R\$*",
                    "*_Impl*"
                )
            }
        }
        verify {
            // Mínimo 80 % en líneas, ramas (if/when/?:) e instrucciones.
            // Clases y métodos se ven en el reporte HTML (columnas "Class" y "Method").
            rule("Cobertura mínima 80%") {
                minBound(80, CoverageUnit.LINE)
                minBound(80, CoverageUnit.BRANCH)
                minBound(80, CoverageUnit.INSTRUCTION)
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.navigation.compose)

    // Red: microservicios propios + API externa
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Persistencia local (modo sin conexión) y sesión
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Imágenes y GPS
    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ---- Pruebas unitarias (src/test) ----
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    // Servidor HTTP falso: verifica que Retrofit + Gson calcen con el JSON del backend
    testImplementation(libs.okhttp.mockwebserver)
    // Reflexión para verificar todos los modelos (data class) del proyecto
    testImplementation(libs.kotlin.reflect)

    // ---- Pruebas de UI (src/androidTest) ----
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
