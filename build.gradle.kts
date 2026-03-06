import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
//import kotlin.text.set

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("de.fabmax.kool:kool-core:0.19.0")
    implementation("de.fabmax.kool:kool-physics:0.19.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    // dependencies - блок с подключенными бмблиотеками (зависимости)
    // implementation - команда добавлябщая библиотеку в проект
    // org.jetbrains.kotlinx:...  - коорлинаты библиотеки
    // kotlinx-coroutines-core - имя необходимого артефакта
    // версия библиотеки 1.7.3
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
//val compileKotlin: KotlinCompile by tasks
//compileKotlin.compilerOptions {
//    freeCompilerArgs.set(listOf("-Xnested-type-aliases"))
//}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xnested-type-aliases"))
}