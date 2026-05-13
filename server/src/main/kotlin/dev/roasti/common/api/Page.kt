package dev.roasti.common.api

import dev.roasti.common.domain.Page
import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto

fun <A, B> Page<A>.toDto(mapper: (A) -> B) = PageResponseDto(
    items = items.map(mapper),
    pagination = paginationDto(),
)

fun <A> Page<A>.paginationDto() = PaginationResponseDto(
    currentPage,
    itemsCount,
    lastPage,
    nextPage,
)