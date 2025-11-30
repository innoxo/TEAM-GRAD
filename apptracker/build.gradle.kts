// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false

    // 🔥 구글 서비스 플러그인 (버전 관리)
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // 간혹 구버전 안드로이드 스튜디오 호환성을 위해 필요할 수 있음
        classpath("com.google.gms:google-services:4.4.2")
    }
}