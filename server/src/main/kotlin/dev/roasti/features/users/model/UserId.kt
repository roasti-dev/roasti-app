package dev.roasti.features.users.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
@OptIn(ExperimentalUuidApi::class)
value class UserId(val value: Uuid)
