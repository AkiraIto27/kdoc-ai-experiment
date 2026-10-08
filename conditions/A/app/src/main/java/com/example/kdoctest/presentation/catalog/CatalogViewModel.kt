package com.example.kdoctest.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kdoctest.domain.repository.CatalogException
import com.example.kdoctest.domain.usecase.LoadCatalogPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(private val useCase: LoadCatalogPage) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = mutableUiState.asStateFlow()

    private var nextCursor: String? = null
    private var requestJob: Job? = null
    private var requestGeneration = 0L

    init {
        refresh()
    }

    fun refresh() {
        val generation = ++requestGeneration
        requestJob?.cancel()
        val current = mutableUiState.value
        mutableUiState.value = current.copy(
            isInitialLoading = current.items.isEmpty(),
            isRefreshing = current.items.isNotEmpty(),
            isLoadingMore = false,
            initialError = null,
            refreshError = null,
            loadMoreError = null,
        )
        loadPage(cursor = null, replace = true, generation = generation)
    }

    fun loadNextPage() {
        val current = mutableUiState.value
        val cursor = nextCursor ?: return
        if (current.isLoading || !current.hasMore) return

        val generation = ++requestGeneration
        mutableUiState.value = current.copy(
            isLoadingMore = true,
            refreshError = null,
            loadMoreError = null,
        )
        loadPage(cursor = cursor, replace = false, generation = generation)
    }

    fun retry() {
        val current = mutableUiState.value
        if (current.isLoading) return
        when {
            current.initialError != null || current.refreshError != null -> refresh()
            current.loadMoreError != null -> loadNextPage()
        }
    }

    private fun loadPage(cursor: String?, replace: Boolean, generation: Long) {
        requestJob = viewModelScope.launch {
            try {
                val page = useCase(cursor = cursor, limit = PAGE_SIZE)
                if (generation != requestGeneration) return@launch

                val items = if (replace) page.items else mutableUiState.value.items + page.items
                nextCursor = page.nextCursor
                mutableUiState.value = CatalogUiState(
                    items = items,
                    totalCount = page.totalCount,
                    hasMore = page.nextCursor != null && items.size < page.totalCount,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != requestGeneration) return@launch

                val current = mutableUiState.value
                val message = when (error) {
                    is CatalogException.InvalidResponse -> "商品情報を読み込めませんでした。もう一度お試しください。"
                    else -> "商品を読み込めませんでした。もう一度お試しください。"
                }
                mutableUiState.value = current.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    initialError = message.takeIf { replace && current.items.isEmpty() },
                    refreshError = message.takeIf { replace && current.items.isNotEmpty() },
                    loadMoreError = message.takeUnless { replace },
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
