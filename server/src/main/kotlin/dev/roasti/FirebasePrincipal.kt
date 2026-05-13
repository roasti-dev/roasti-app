package dev.roasti

import dev.roasti.features.users.model.UserId

const val FIREBASE_AUTH = "firebase"

data class FirebasePrincipal(val id: UserId)
