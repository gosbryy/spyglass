// Плагины объявляются здесь один раз (apply false), чтобы все модули грузили их в общий classloader
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(ktorLibs.plugins.ktor) apply false
}
