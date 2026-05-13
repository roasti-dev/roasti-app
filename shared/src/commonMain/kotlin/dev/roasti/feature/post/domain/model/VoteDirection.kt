package dev.roasti.feature.post.domain.model

enum class VoteDirection {
    UP,
    DOWN,
    NONE;

    fun deltaTo(target: VoteDirection): Int = when (this) {
        UP -> when (target) {
            UP -> 0
            NONE -> -1
            DOWN -> -2
        }
        NONE -> when (target) {
            UP -> +1
            NONE -> 0
            DOWN -> -1
        }
        DOWN -> when (target) {
            UP -> +2
            NONE -> +1
            DOWN -> 0
        }
    }
}
