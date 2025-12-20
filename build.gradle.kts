plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // dependencies - блок с подключенными бмблиотеками (зависимости)
    // implementation - команда добавлябщая библиотеку в проект
    // org.jetbrains.kotlinx:...  - коорлинаты библиотеки
    // kotlinx-coroutines-core - имя необходимого артефакта
    // версия библиотеки 1.7.3
}

tasks.test {
    useJUnitPlatform()
}