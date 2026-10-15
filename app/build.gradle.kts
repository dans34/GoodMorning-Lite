import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
}

fun gitValue(vararg args: String, fallback: String): String {
    return try {
        val process = ProcessBuilder(listOf("git") + args)
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (process.waitFor() == 0 && output.isNotEmpty()) output else fallback
    } catch (_: Exception) {
        fallback
    }
}

val gitCommit = gitValue("rev-parse", "HEAD", fallback = "unknown")
val gitStatus = gitValue("status", "--porcelain", fallback = "unknown")
val gitDirtyKnown = gitStatus != "unknown"
val gitDirty = gitDirtyKnown && gitStatus.isNotEmpty()
val buildDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))

android {
    namespace = "com.dans34.goodmorninglite"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.dans34.goodmorninglite"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-alpha"

        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("boolean", "GIT_DIRTY", gitDirty.toString())
        buildConfigField("boolean", "GIT_METADATA_KNOWN", (gitCommit != "unknown" && gitDirtyKnown).toString())
    }
}
