package dev.roasti.feature.post.data.mapper

import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.domain.model.VoteDirection

private const val UP = "up"
private const val DOWN = "down"
private const val NONE = "none"

fun VoteDirection.toWireString(): String = when (this) {
    VoteDirection.UP -> UP
    VoteDirection.DOWN -> DOWN
    VoteDirection.NONE -> NONE
}

fun String?.toVoteDirection(): VoteDirection = when (this) {
    UP -> VoteDirection.UP
    DOWN -> VoteDirection.DOWN
    else -> VoteDirection.NONE
}

fun VoteDirection.toDto(): VoteDirectionDto = when (this) {
    VoteDirection.UP -> VoteDirectionDto.UP
    VoteDirection.DOWN -> VoteDirectionDto.DOWN
    VoteDirection.NONE -> VoteDirectionDto.NONE
}

fun VoteDirectionDto.toDomain(): VoteDirection = when (this) {
    VoteDirectionDto.UP -> VoteDirection.UP
    VoteDirectionDto.DOWN -> VoteDirection.DOWN
    VoteDirectionDto.NONE -> VoteDirection.NONE
}
