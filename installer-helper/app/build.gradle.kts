import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
val trust = Properties().apply { rootProject.file("trust.properties").inputStream().use { load(it) } }
android {
    namespace = "app.expressiveclock.installer"
    compileSdk = 37
    defaultConfig {
        applicationId = "app.expressiveclock.installer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-local"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "EXPECTED_PACKAGE", "\"${trust.getProperty("package")}\"")
        buildConfigField("String", "EXPECTED_CERT", "\"${trust.getProperty("certificate")}\"")
    }
    buildFeatures { buildConfig = true; compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation("com.android.tools.build:apksig:9.2.1")
    implementation(platform("androidx.compose:compose-bom-alpha:2026.07.01"))
    implementation("androidx.compose.material3:material3:1.5.0-alpha25")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom-alpha:2026.07.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    // Compose’s transitive Espresso 3.5 uses a removed Android 17 input API. Test-only fix.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
