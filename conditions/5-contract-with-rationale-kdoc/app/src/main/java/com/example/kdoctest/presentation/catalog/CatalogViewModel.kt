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
 * 商品ページ取得を起動し、一覧・読み込み状態・失敗メッセージを[uiState]へ反映する。
 * 生成時に[refresh]で先頭取得を開始する。世代違いの成功・失敗は反映しない。
 * 取得キャンセルはエラー表示へ変換せず再送出する。
 *
 * @param useCase 全取得に[PAGE_SIZE]を指定するページ取得処理。
 *
 * 背景: 固定500件の共通基盤では、状態・失敗・再試行を直接確認できるよう、StateFlowと明示的な追加読み込みを採用する。
 */
class CatalogViewModel(private val useCase: LoadCatalogPage) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CatalogUiState())
    /**
     * 一覧画面用の読み取り専用状態。各エラーのnullは、その種別の表示対象エラーなし。
     * 成功時の`hasMore`は次カーソルがあり、表示件数が取得全件数より少ない場合にtrue。
     */
    val uiState: StateFlow<CatalogUiState> = mutableUiState.asStateFlow()

    private var nextCursor: String? = null
    private var requestJob: Job? = null
    private var requestGeneration = 0L

    init {
        refresh()
    }

    /**
     * 進行中の取得をキャンセルし、エラーを消して先頭の非同期取得を開始する。
     * 取得中は商品を保持し、空なら初回読み込み、商品があれば更新中とする。
     * 成功は一覧を置換。失敗は商品・ページ情報を保持し、空なら`initialError`、あれば`refreshError`へメッセージを設定する。
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
     * 次カーソルの非同期取得を開始し、成功した商品のみ末尾へ追記する。
     * 次カーソルなし・取得中・`hasMore`がfalseなら何もしない。
     * 開始時に更新・追加取得エラーを消す。失敗は商品・ページ情報を保持し、`loadMoreError`を設定する。
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
     * 初回・更新失敗は[refresh]、追加取得失敗は[loadNextPage]で再試行する。
     * 取得中・エラーなしなら何もしない。
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
         * 初回・更新・追加取得に共通の、1ページの件数上限。
         */
        const val PAGE_SIZE = 50
    }
}
