package dev.roasti

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform