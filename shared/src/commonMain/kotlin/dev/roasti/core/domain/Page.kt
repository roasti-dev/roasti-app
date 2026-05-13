package dev.roasti.core.domain

data class Page<T>(
    val items: List<T>,
    val currentPage: Int,
    val itemsCount: Int,
    val lastPage: Int,
    val nextPage: Int,
)
