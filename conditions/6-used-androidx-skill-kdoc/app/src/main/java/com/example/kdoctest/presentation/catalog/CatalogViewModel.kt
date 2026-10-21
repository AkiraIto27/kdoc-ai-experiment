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

/**
 * 商品ページを取得して画面の状態を更新する。
 *
 * ```kotlin
 * val viewModel = CatalogViewModel(useCase)
 * val states = viewModel.uiState
 * ```
 *
 * - 商品一覧・読み込み状態・失敗メッセージを[uiState]へ反映する
 * - 生成時に[refresh]で先頭ページの取得を開始する
 * - 要求の世代が一致する成功・失敗だけを[uiState]へ反映する
 * - 取得コルーチンのキャンセルはエラー表示へ変換せず再送出する
 *
 * @param useCase 各ページを取得する[LoadCatalogPage]。すべての取得で[PAGE_SIZE]を指定する
 */
class CatalogViewModel(private val useCase: LoadCatalogPage) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CatalogUiState())
    /**
     * 一覧画面へ読み取り専用の[StateFlow]を公開する。
     *
     * ```kotlin
     * val state = viewModel.uiState.value
     * val hasMore = state.hasMore
     * ```
     *
     * - 一覧画面が状態の変化を監視する
     * - [CatalogUiState.initialError]・[CatalogUiState.refreshError]・[CatalogUiState.loadMoreError]の`null`は、その種別に表示対象のエラーがないことを示す
     * - 取得成功時の[CatalogUiState.hasMore]は、次カーソルがあり、表示件数が取得した全件数より少ない場合に`true`になる
     */
    val uiState: StateFlow<CatalogUiState> = mutableUiState.asStateFlow()

    private var nextCursor: String? = null
    private var requestJob: Job? = null
    private var requestGeneration = 0L

    init {
        refresh()
    }

    /**
     * 先頭ページの非同期取得を開始する。
     *
     * ```kotlin
     * viewModel.refresh()
     * val state = viewModel.uiState.value
     * ```
     *
     * - 進行中の取得をキャンセルし、[CatalogUiState.initialError]・[CatalogUiState.refreshError]・[CatalogUiState.loadMoreError]を消す
     * - 取得中は既存商品を保持し、商品がなければ[CatalogUiState.isInitialLoading]、あれば[CatalogUiState.isRefreshing]を`true`にする
     * - 成功時は商品一覧を取得結果で置き換える
     * - 失敗時は商品とページ情報を保持し、商品がなければ[CatalogUiState.initialError]、あれば[CatalogUiState.refreshError]へメッセージを設定する
     */
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

    /**
     * 次ページを非同期に取得して商品を追記する。
     *
     * ```kotlin
     * viewModel.loadNextPage()
     * val state = viewModel.uiState.value
     * ```
     *
     * - 次カーソルで取得を開始し、成功した商品のみ既存一覧の末尾へ追記する
     * - 次カーソルがない場合、いずれかの取得中（[CatalogUiState.isLoading]）、または[CatalogUiState.hasMore]が`false`の場合は何もしない
     * - 開始時に[CatalogUiState.refreshError]と[CatalogUiState.loadMoreError]を消す
     * - 失敗時は商品とページ情報を保持し、[CatalogUiState.loadMoreError]へメッセージを設定する
     */
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

    /**
     * 表示中の取得失敗に応じて再試行する。
     *
     * ```kotlin
     * viewModel.retry()
     * val state = viewModel.uiState.value
     * ```
     *
     * - 初回失敗（[CatalogUiState.initialError]）・更新失敗（[CatalogUiState.refreshError]）は[refresh]で再試行する
     * - 追加取得失敗（[CatalogUiState.loadMoreError]）は[loadNextPage]で再試行する
     * - 取得中（[CatalogUiState.isLoading]）またはエラーがなければ何もしない
     */
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
        /**
         * 1ページの取得件数の上限を定める。
         *
         * ```kotlin
         * val useCase = LoadCatalogPage(repository)
         * val page = useCase(limit = CatalogViewModel.PAGE_SIZE)
         * ```
         *
         * - 初回取得・[CatalogViewModel.refresh]による更新・[CatalogViewModel.loadNextPage]による追加取得に共通で、1ページの上限を50件とする
         */
        const val PAGE_SIZE = 50
    }
}
