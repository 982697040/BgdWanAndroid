plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.bgd.myapplication.core.domain"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
}
