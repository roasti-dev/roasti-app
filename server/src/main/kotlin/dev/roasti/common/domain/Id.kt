package dev.roasti.common.domain

import kotlin.uuid.Uuid

inline fun <T> String.toId(constructor: (Uuid) -> T): T? =
    runCatching { constructor(Uuid.parse(this)) }.getOrNull()
