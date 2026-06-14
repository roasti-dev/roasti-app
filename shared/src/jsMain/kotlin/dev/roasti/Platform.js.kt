package dev.roasti

class JSPlatform: Platform {
    override val name: String = "Web JS"
}

actual fun getPlatform(): Platform = JSPlatform()