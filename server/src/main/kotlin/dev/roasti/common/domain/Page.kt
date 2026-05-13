package dev.roasti.common.domain

fun pageOffset(page: Int, limit: Int) = (page - 1) * limit

data class Page<T>(
    val items: List<T>,
    val currentPage: Int,
    val itemsCount: Int,
    val lastPage: Int,
    val nextPage: Int,
) {
  companion object {
    fun <T> of(items: List<T>, page: Int, total: Int, limit: Int): Page<T> {
      val lastPage = if (total == 0) 1 else (total + limit - 1) / limit
      return Page(
          items = items,
          currentPage = page,
          itemsCount = total,
          lastPage = lastPage,
          nextPage = if (page < lastPage) page + 1 else page,
      )
    }
  }
}
