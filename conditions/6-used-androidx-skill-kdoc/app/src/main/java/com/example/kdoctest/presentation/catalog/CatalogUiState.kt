package com.example.kdoctest.presentation.catalog

import com.example.kdoctest.domain.model.Product

data class CatalogUiState(
    val items: List<Product> = emptyList(),
    val totalCount: Int = 0,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val initialError: String? = null,
    val refreshError: String? = null,
    val loadMoreError: String? = null,
) {
    val isLoading: Boolean
        get() = isInitialLoading || isRefreshing || isLoadingMore
}
